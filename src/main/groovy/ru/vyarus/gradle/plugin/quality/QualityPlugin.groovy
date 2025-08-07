package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.*
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.process.CommandLineArgumentProvider
import ru.vyarus.gradle.plugin.quality.service.TasksListenerService
import ru.vyarus.gradle.plugin.quality.task.InitQualityConfigTask
import ru.vyarus.gradle.plugin.quality.task.QualityToolVersionsTask
import ru.vyarus.gradle.plugin.quality.util.CpdUtils
import ru.vyarus.gradle.plugin.quality.util.SpotbugsExclusionConfigProvider
import ru.vyarus.gradle.plugin.quality.util.SpotbugsUtils

import javax.inject.Inject

/**
 * Quality plugin enables and configures quality plugins for java and groovy projects.
 * Plugin must be registered after java or groovy plugins, otherwise wil do nothing.
 * <p>
 * Java project is detected by presence of java sources. In this case Checkstyle, PMD and Spotbugs plugins are
 * activated. If quality plugins applied manually, they would be configured too (even if auto detection didn't
 * recognize related sources). Also, additional javac lint options are activated to show more warnings
 * during compilation.
 * <p>
 * If groovy plugin enabled, CodeNarc plugin activated.
 * <p>
 * All plugins are configured to produce xml and html reports. For spotbugs html report
 * generated manually. All plugins violations are printed into console in unified format which makes console
 * output good enough for fixing violations.
 * <p>
 * Plugin may be configured with 'quality' closure. See {@link QualityExtension} for configuration options.
 * <p>
 * By default, plugin use bundled quality plugins configurations. These configs could be copied into project
 * with 'initQualityConfig' task (into quality.configDir directory). These custom configs will be used in
 * priority with fallback to default config if config not found.
 * <p>
 * Special tasks registered for each source set: checkQualityMain, checkQualityTest etc.
 * Tasks group registered quality plugins tasks for specific source set. This allows running quality plugins
 * directly without tests (comparing to using 'check' task). Also, allows running quality plugins on source sets
 * not enabled for main 'check' (example case: run quality checks for tests (time to time)). These tasks may be
 * used even when quality tasks are disabled ({@code quality.enabled = false}).
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @see CodeNarcPlugin
 * @see CheckstylePlugin
 * @see PmdPlugin
 * @see com.github.spotbugs.snom.SpotBugsPlugin
 * @see de.aaschmid.gradle.plugins.cpd.CpdPlugin
 */
@CompileStatic
@SuppressWarnings('ClassSize')
abstract class QualityPlugin implements Plugin<Project> {

    public static final String TOOL_CHECKSTYLE = 'checkstyle'
    public static final String TOOL_PMD = 'pmd'
    public static final String TOOL_SPOTBUGS = 'spotbugs'
    public static final String TOOL_CODENARC = 'codenarc'
    public static final String TOOL_CPD = 'cpd'

    private static final String QUALITY_TASK = 'checkQuality'
    private static final String CODENARC_GROOVY4 = '-groovy-4.0'

    private Provider<TasksListenerService> tasksListener

    @Inject
    abstract BuildEventsListenerRegistry getEventsListenerRegistry()

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)
            addTasks(project, extension)

            tasksListener = project.gradle.sharedServices.registerIfAbsent(
                    'taskEvents', TasksListenerService, spec -> { })
            eventsListenerRegistry.onTaskCompletion(tasksListener)

            project.afterEvaluate {
                configureGroupingTasks(project)

                Context context = createContext(project, extension)
                project.tasks.withType(QualityToolVersionsTask)
                        .configureEach { it.context.set(context) }
                ConfigLoader configLoader = new ConfigLoader(project)
                tasksListener.get().init(configLoader, extension)

                configureJavac(project, extension)
                if (JavaVersion.current().java11Compatible) {
                    applyCheckstyle(project, extension, configLoader, context.registerJavaPlugins)
                }
                applyPMD(project, extension, configLoader, context.registerJavaPlugins)
                applySpotbugs(project, extension, configLoader, context.registerJavaPlugins)
                configureAnimalSniffer(project, extension)
                configureCpdPlugin(project, extension, configLoader,
                        !context.registerJavaPlugins && context.registerGroovyPlugins)
                applyCodeNarc(project, extension, configLoader, context.registerGroovyPlugins)
            }
        }
    }

    protected void addTasks(Project project, QualityExtension extension) {
        project.tasks.register('initQualityConfig', InitQualityConfigTask)
        project.tasks.register('qualityToolVersions', QualityToolVersionsTask) {
            it.checkstyleVersion.set(project.provider {
                extension.checkstyle.get() ? extension.checkstyleVersion.get() : 'disabled' })

            it.pmdVersion.set(project.provider {
                extension.pmd.get() ? extension.pmdVersion.get() : 'disabled' })

            it.spotBugsVersion.set(project.provider {
                extension.spotbugs.get() ? extension.spotbugsVersion.get() : 'disabled' })

            it.codeNarcVersion.set(project.provider {
                extension.codenarc.get() ? extension.codenarcVersion.get() : 'disabled' })
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureGroupingTasks(Project project) {
        // create checkQualityMain, checkQualityTest (quality tasks for all source sets)
        // using all source sets and not just declared in extension to be able to run quality plugins
        // on source sets which are not included in check task run (e.g. run quality on tests time to time)
        project.sourceSets.each { SourceSet set ->
            project.tasks.register(set.getTaskName(QUALITY_TASK, null)) {
                it.with {
                    group = 'verification'
                    description = "Run quality plugins for $set.name source set"
                }
            }
        }
    }

    protected void configureJavac(Project project, QualityExtension extension) {
        if (!extension.lintOptions.get()) {
            return
        }
        project.tasks.withType(JavaCompile).configureEach { JavaCompile t ->
            t.options.compilerArgumentProviders.add({
                extension.lintOptions.get().collect { "-Xlint:$it" as String }
            } as CommandLineArgumentProvider)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings(['MethodSize', 'NestedBlockDepth'])
    protected void applyCheckstyle(Project project, QualityExtension extension, ConfigLoader configLoader,
                                   boolean register) {
        configurePlugin(project,
                extension.checkstyle.get(),
                register,
                CheckstylePlugin) {
            project.configure(project) {
                // required due to checkstyle update of gradle metadata causing now collision with google collections
                // https://github.com/google/guava/releases/tag/v32.1.0 (https://github.com/gradle/gradle/issues/27035)
                String guavaTarget = 'com.google.guava:guava:0'
                configurations.checkstyle {
                    resolutionStrategy.capabilitiesResolution.withCapability(
                            'com.google.collections:google-collections') {
                        select(guavaTarget)
                    }
                    resolutionStrategy.capabilitiesResolution.withCapability('com.google.guava:listenablefuture') {
                        select(guavaTarget)
                    }
                }

                checkstyle {
                    showViolations = false
                    toolVersion = extension.checkstyleVersion.get()
                    ignoreFailures = !extension.strict.get()
                    // this may be custom user file (gradle/config/checkstyle/checkstyle.xml) or default one
                    // (in different location)
                    configFile = configLoader.resolveCheckstyleConfig(false)
                    // this is required for ${config_loc} variable, but this will ALWAYS point to
                    // gradle/config/checkstyle/ (or other configured config location dir) because custom
                    // configuration files may be only there
                    configDirectory = configLoader.resolveCheckstyleConfigDir()
                    sourceSets = extension.sourceSets.get()
                }

                tasks.withType(Checkstyle).configureEach { task ->
                    doFirst {
                        configLoader.resolveCheckstyleConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports.get())

                    registerReporter(task, TOOL_CHECKSTYLE)
                }
            }
            configurePluginTasks(project, extension, Checkstyle, TOOL_CHECKSTYLE)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void applyPMD(Project project, QualityExtension extension, ConfigLoader configLoader,
                            boolean register) {
        configurePlugin(project,
                extension.pmd.get(),
                register,
                PmdPlugin) {
            project.configure(project) {
                pmd {
                    toolVersion = extension.pmdVersion.get()
                    ignoreFailures = !extension.strict.get()
                    ruleSets = []
                    ruleSetFiles = files(configLoader.resolvePmdConfig(false).absolutePath)
                    sourceSets = extension.sourceSets.get()
                }
                // have to override dependencies declaration due to split in pmd 7
                // https://github.com/gradle/gradle/issues/24502
                dependencies {
                    pmd("net.sourceforge.pmd:pmd-ant:${extension.pmdVersion.get()}")
                    pmd("net.sourceforge.pmd:pmd-java:${extension.pmdVersion.get()}")
                }
                tasks.withType(Pmd).configureEach { task ->
                    doFirst {
                        configLoader.resolvePmdConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports.get())

                    registerReporter(task, TOOL_PMD)
                }
            }
            configurePluginTasks(project, extension, Pmd, TOOL_PMD)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings(['MethodSize', 'NestedBlockDepth'])
    protected void applySpotbugs(Project project, QualityExtension extension, ConfigLoader configLoader,
                                 boolean register) {
        // if plugin is not on classpath - do nothing; if plugin in classpath but not applied - apply it
        Class<? extends Plugin> plugin = SpotbugsUtils.findPluginClass(project)
        if (plugin == null) {
            return
        }
        SpotbugsUtils.validateRankSetting(extension.spotbugsMaxRank.get())

        Class<? extends Task> spotbugsTaskType = plugin.classLoader
                .loadClass('com.github.spotbugs.snom.SpotBugsTask') as Class<? extends Task>

        configurePlugin(project,
                extension.spotbugs.get(),
                register,
                plugin) {
            project.configure(project) {
                spotbugs {
                    toolVersion = extension.spotbugsVersion.get()
                    ignoreFailures = !extension.strict.get()
                    effort = SpotbugsUtils.enumValue(plugin, 'Effort', extension.spotbugsEffort.get())
                    reportLevel = SpotbugsUtils.enumValue(plugin, 'Confidence', extension.spotbugsLevel.get())
                    // in gradle 5 default 1g was changed and so spotbugs fails on large projects (recover
                    // behaviour), but not if value set manually
                    maxHeapSize.convention(extension.spotbugsMaxHeapSize.get())
                }

                // override spotbugs plugin configuration: by default, it would apply ALL tasks
                SpotbugsUtils.fixCheckDependencies(project, extension, spotbugsTaskType)

                // spotbugs annotations to simplify access to @SuppressFBWarnings
                // (applied according to plugin recommendation)
                if (extension.spotbugsAnnotations.get()) {
                    dependencies {
                        compileOnly "com.github.spotbugs:spotbugs-annotations:${extension.spotbugsVersion.get()}"
                    }
                }

                // plugins shortcut
                extension.spotbugsPlugins.get()?.each {
                    project.configurations.getByName('spotbugsPlugins').dependencies.add(
                            project.dependencies.create(it)
                    )
                }

                tasks.withType(spotbugsTaskType).configureEach { task ->
                    doFirst {
                        configLoader.resolveSpotbugsExclude()
                        // it is not possible to substitute filter file here (due to locked Property)
                        // but possible to update already configured file (it must be already a temp file here)
                        SpotbugsUtils.replaceExcludeFilter(task, extension, logger)
                    }
                    // have to use this way instead of doFirst hook, because nothing else will work (damn props!)
                    excludeFilter.set(project.provider(new SpotbugsExclusionConfigProvider(
                            task, configLoader, extension
                    )))
                    reports {
                        xml {
                            required.set(true)
                        }
                        html {
                            required.set(extension.htmlReports.get())
                        }
                    }
                    registerReporter(task, TOOL_SPOTBUGS)
                }
            }

            configurePluginTasks(project, extension, spotbugsTaskType, TOOL_SPOTBUGS)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void applyCodeNarc(Project project, QualityExtension extension, ConfigLoader configLoader,
                                 boolean register) {
        configurePlugin(project,
                extension.codenarc.get(),
                register,
                CodeNarcPlugin) {
            project.configure(project) {
                codenarc {
                    toolVersion = extension.codenarcVersion.get()
                    ignoreFailures = !extension.strict.get()
                    configFile = configLoader.resolveCodenarcConfig(false)
                    sourceSets = extension.sourceSets.get()
                }
                if (extension.codenarcGroovy4.get() && !extension.codenarcVersion.get().endsWith(CODENARC_GROOVY4)) {
                    // since codenarc 3.1 different groovy4-based jar could be used
                    dependencies {
                        codenarc "org.codenarc:CodeNarc:${extension.codenarcVersion.get()}$CODENARC_GROOVY4"
                    }
                }
                tasks.withType(CodeNarc).configureEach { task ->
                    doFirst {
                        configLoader.resolveCodenarcConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports.get())

                    registerReporter(task, TOOL_CODENARC)
                }
            }
            configurePluginTasks(project, extension, CodeNarc, TOOL_CODENARC)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureAnimalSniffer(Project project, QualityExtension extension) {
        project.plugins.withId('ru.vyarus.animalsniffer') { plugin ->
            project.configure(project) {
                animalsniffer {
                    ignoreFailures = !extension.strict.get()
                    sourceSets = extension.sourceSets.get()
                }
                if (extension.animalsnifferVersion.present) {
                    animalsniffer.toolVersion = extension.animalsnifferVersion.get()
                }
            }
            applyEnabledState(project, extension,
                    plugin.class.classLoader.loadClass('ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer'))
            groupQualityTasks(project, 'animalsniffer')
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('MethodSize')
    protected void configureCpdPlugin(Project project, QualityExtension extension, ConfigLoader configLoader,
                                      boolean onlyGroovy) {
        if (!extension.cpd.get()) {
            return
        }

        CpdUtils.findAndConfigurePlugin(project) { Project prj, Plugin plugin ->
            boolean sameModuleDeclaration = prj == project
            // STAGE1 for multi-module project this part applies by all modules with quality plugin enabled
            prj.configure(prj) {
                cpd {
                    // special case for single-module projects
                    if (sameModuleDeclaration && onlyGroovy) {
                        language = 'groovy'
                    }
                    toolVersion = extension.pmdVersion.get()
                    // assuming that in case of multi-module project quality plugin is applied in subprojects section
                    // and so it is normal that subproject configures root project
                    // Otherwise, side effect is possible that first configured module with quality plugin determines
                    // root project flag value
                    ignoreFailures = !extension.strict.get()
                }
                // only default task is affected
                tasks.named('cpdCheck').configure {
                    doFirst {
                        if (extension.cpdUnifySources.get()) {
                            applyExcludes(it, extension)
                        }
                    }
                }
            }
            // cpdCheck is always declared by cpd plugin
            TaskProvider<SourceTask> cpdCheck = CpdUtils.findCpdTask(prj)
            if (extension.cpdUnifySources.get()) {
                // exclude sources, not declared for quality checks in quality plugin declaration project
                CpdUtils.unifyCpdSources(project, cpdCheck, extension.sourceSets.get())
            }

            // STAGE2 for multi-module project everything below must be applied just once
            if (CpdUtils.isCpdAlreadyConfigured(prj)) {
                return
            }

            Class<Task> cpdTasksType = plugin.class.classLoader.loadClass('de.aaschmid.gradle.plugins.cpd.Cpd')
            // reports applied for all registered cpd tasks
            prj.tasks.withType(cpdTasksType).configureEach { task ->
                reports.xml.required.set(true)
                doFirst {
                    configLoader.resolveCpdXsl()
                }
                // console reporting for each cpd task
                registerReporter(task, TOOL_CPD, true)
            }
            // cpd plugin recommendation: module check must also run cpd (check module changes for duplicates)
            // grouping tasks (checkQualityMain) are not affected because cpd applied to all source sets
            // For single module projects simply make sure check will trigger cpd
            project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME)
                    .configure { it.dependsOn prj.tasks.withType(cpdTasksType) }

            // cpd disabled together with all quality plugins
            // yes, it's not completely normal that module could disable root project task, but it would be much
            // simpler to use like that (because quality plugin assumed to be applied in subprojects section)
            applyEnabledState(prj, extension, cpdTasksType)
        }
    }

    /**
     * Detects available source folders in configured source sets to understand
     * what sources are available: groovy, java or both. Based on that knowledge
     * appropriate plugins could be registered.
     *
     * @param project project instance
     * @param extension extension instance
     * @return context instance
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    protected Context createContext(Project project, QualityExtension extension) {
        Context context = new Context()
        if (extension.autoRegistration.get()) {
            context.registerJavaPlugins = (extension.sourceSets.get()
                    .find { it.java.srcDirs.find { it.exists() } }) != null
            if (project.plugins.findPlugin(GroovyPlugin)) {
                context.registerGroovyPlugins =
                        (extension.sourceSets.get().find { it.groovy.srcDirs.find { it.exists() } }) != null
            }
        }
        context
    }

    /**
     * Plugins may be registered manually and in this case plugin will also will be configured, but only
     * when plugin support not disabled by quality configuration. If plugin not registered and
     * sources auto detection allow registration - it will be registered and then configured.
     *
     * @param project project instance
     * @param enabled true if quality plugin support enabled for plugin
     * @param register true to automatically register plugin
     * @param plugin plugin class
     * @param config plugin configuration closure
     */
    protected void configurePlugin(Project project, boolean enabled, boolean register, Class plugin, Closure config) {
        if (!enabled) {
            // do not configure even if manually registered
            return
        } else if (register) {
            // register plugin automatically
            project.plugins.apply(plugin)
            project.logger.info("[plugin:quality] Plugin $plugin.simpleName auto-applied in project $project.name ")
        }
        // configure plugin if registered (manually or automatic)
        project.plugins.withType(plugin) {
            config.call()
        }
    }

    protected void registerReporter(Task task, String type, boolean useFullTaskName = false) {
        tasksListener.get().register(task, type, useFullTaskName)
    }

    /**
     * Applies enabled state control and checkQuality* grouping tasks.
     *
     * @param project project instance
     * @param extension extension instance
     * @param taskType task class
     * @param task task base name
     */
    protected void configurePluginTasks(Project project, QualityExtension extension, Class taskType, String task) {
        applyEnabledState(project, extension, taskType)
        groupQualityTasks(project, task)
    }

    /**
     * If quality tasks are disabled in configuration ({@code quality.enabled = false})
     * then disabling tasks. Anyway, task must not be disabled if called directly
     * or through grouping quality task (e.g. checkQualityMain).
     * NOTE: if, for example, checkQualityMain is called after some other task
     * (e.g. someTask.dependsOn checkQualityMain) then quality tasks will be disabled!
     * Motivation is: plugins are disabled for a reason and could be enabled only when called
     * directly (because obviously user wants quality task(s) to run).
     *
     * @param project project instance
     * @param extension extension instance
     * @param task quality plugin task class
     */
    protected void applyEnabledState(Project project, QualityExtension extension, Class task) {
        if (!extension.enabled.get()) {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                project.tasks.withType(task).configureEach { Task t ->
                    // last task on stack obtained only on actual task usage
                    List<Task> tasks = graph.allTasks
                    Task called = tasks != null && !tasks.empty ? tasks.last() : null
                    // enable task only if it's called directly or through grouping task
                    t.enabled = called != null && (called == t || called.name.startsWith(QUALITY_TASK))
                }
            }
        }
    }

    /**
     * Applies exclude path patterns to quality tasks.
     * Note: this does not apply to animalsniffer. For spotbugs this appliance is useless, see custom support above.
     *
     * @param task quality task
     * @param extension extension instance
     */
    protected void applyExcludes(SourceTask task, QualityExtension extension) {
        if (extension.excludeSources) {
            // directly excluded sources
            task.source = task.source - extension.excludeSources
        }
        if (extension.exclude.get()) {
            // exclude by patterns (relative to source roots)
            task.exclude extension.exclude.get()
        }
    }

    /**
     * Quality plugins register tasks for each source set. Declared affected source sets
     * only affects which tasks will 'check' depend on.
     * Grouping tasks allow to call quality tasks, not included to 'check'.
     *
     * @param project project instance
     * @param task task base name
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    protected void groupQualityTasks(Project project, String task) {
        // each quality plugin generate separate tasks for each source set
        // assign plugin tasks to source set grouping quality task
        project.sourceSets.each {
            TaskProvider pluginTask = project.tasks.named(it.getTaskName(task, null))
            if (pluginTask) {
                project.tasks.named(it.getTaskName(QUALITY_TASK, null)).configure { it.dependsOn pluginTask }
            }
        }
    }

    /**
     * Internal configuration context.
     */
    static class Context {
        boolean registerJavaPlugins
        boolean registerGroovyPlugins
    }
}
