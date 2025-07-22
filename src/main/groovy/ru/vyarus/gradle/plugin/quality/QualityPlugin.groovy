package ru.vyarus.gradle.plugin.quality

import com.github.spotbugs.snom.SpotBugsTask
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.*
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.process.CommandLineArgumentProvider
import ru.vyarus.gradle.plugin.quality.spotbugs.CustomSpotBugsPlugin
import ru.vyarus.gradle.plugin.quality.task.InitQualityConfigTask
import ru.vyarus.gradle.plugin.quality.util.CpdUtils
import ru.vyarus.gradle.plugin.quality.util.SpotbugsExclusionConfigProvider
import ru.vyarus.gradle.plugin.quality.util.SpotbugsUtils
import ru.vyarus.gradle.plugin.quality.service.TasksListenerService

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
            addInitConfigTask(project)

            tasksListener = project.gradle.sharedServices.registerIfAbsent(
                            'taskEvents', TasksListenerService, spec -> { })
            eventsListenerRegistry.onTaskCompletion(tasksListener)

            project.afterEvaluate {
                configureGroupingTasks(project)

                Context context = createContext(project, extension)
                ConfigLoader configLoader = new ConfigLoader(project)
                tasksListener.get().init(configLoader, extension)

                configureJavac(project, extension)
                applyCheckstyle(project, extension, configLoader, context.registerJavaPlugins)
                applyPMD(project, extension, configLoader, context.registerJavaPlugins)
                applySpotbugs(project, extension, configLoader, context.registerJavaPlugins)
                configureAnimalSniffer(project, extension)
                configureCpdPlugin(project, extension, configLoader,
                        !context.registerJavaPlugins && context.registerGroovyPlugins)
                applyCodeNarc(project, extension, configLoader, context.registerGroovyPlugins)
            }
        }
    }

    protected void addInitConfigTask(Project project) {
        project.tasks.register('initQualityConfig', InitQualityConfigTask)
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
        if (!extension.lintOptions) {
            return
        }
        project.tasks.withType(JavaCompile).configureEach { JavaCompile t ->
            t.options.compilerArgumentProviders
                    .add({ extension.lintOptions.collect { "-Xlint:$it" as String } } as CommandLineArgumentProvider)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings(['MethodSize', 'NestedBlockDepth'])
    protected void applyCheckstyle(Project project, QualityExtension extension, ConfigLoader configLoader,
                                 boolean register) {
        configurePlugin(project,
                extension.checkstyle,
                register,
                CheckstylePlugin) {
            project.configure(project) {
                // required due to checkstyle update of gradle metadata causing now collission with google collections
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
                    toolVersion = extension.checkstyleVersion
                    ignoreFailures = !extension.strict
                    // this may be custom user file (gradle/config/checkstyle/checkstyle.xml) or default one
                    // (in different location)
                    configFile = configLoader.resolveCheckstyleConfig(false)
                    // this is required for ${config_loc} variable, but this will ALWAYS point to
                    // gradle/config/checkstyle/ (or other configured config location dir) because custom
                    // configuration files may be only there
                    configDirectory = configLoader.resolveCheckstyleConfigDir()
                    sourceSets = extension.sourceSets
                }
                if (extension.checkstyleBackport) {
                    repositories {
                        maven {
                            url 'https://rnveach.github.io/checkstyle-backport-jre8/maven2/'
                            // use custom repository ONLY for checkstyle (just in case)
                            content { includeGroup 'com.puppycrawl.tools' }
                        }
                    }
                    dependencies {
                        checkstyle "com.puppycrawl.tools:checkstyle-backport-jre8:${extension.checkstyleVersion}"
                    }
                }

                tasks.withType(Checkstyle).configureEach { task ->
                    doFirst {
                        if (extension.checkstyleBackport) {
                            project.logger.warn("WARNING: checkstyle-backport-jre8 (${extension.checkstyleVersion})" +
                                    ' used instead of regular checkstyle: https://checkstyle.org/#Backport')
                        }
                        configLoader.resolveCheckstyleConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports)

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
                extension.pmd,
                register,
                PmdPlugin) {
            project.configure(project) {
                pmd {
                    toolVersion = extension.pmdVersion
                    ignoreFailures = !extension.strict
                    ruleSets = []
                    ruleSetFiles = files(configLoader.resolvePmdConfig(false).absolutePath)
                    sourceSets = extension.sourceSets
                }
                // have to override dependencies declaration due to split in pmd 7
                // https://github.com/gradle/gradle/issues/24502
                dependencies {
                    pmd("net.sourceforge.pmd:pmd-ant:$extension.pmdVersion")
                    pmd("net.sourceforge.pmd:pmd-java:$extension.pmdVersion")
                }
                if (extension.pmdIncremental) {
                    // block enables incremental analysis for gradle 5.6 - 6.4 (later it is enabled by default)
                    if (PmdExtension.metaClass.properties.any { it.name == 'incrementalAnalysis' }) {
                        pmd.incrementalAnalysis = true
                    } else {
                        project.logger.warn('WARNING: PMD incremental analysis option ignored, because it\'s '
                                + 'supported only from gradle 5.6')
                    }
                }
                tasks.withType(Pmd).configureEach { task ->
                    doFirst {
                        configLoader.resolvePmdConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports)

                    registerReporter(task, TOOL_PMD)
                }
            }
            configurePluginTasks(project, extension, Pmd, TOOL_PMD)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('MethodSize')
    protected void applySpotbugs(Project project, QualityExtension extension, ConfigLoader configLoader,
                               boolean register) {
        SpotbugsUtils.validateRankSetting(extension.spotbugsMaxRank)

        configurePlugin(project,
                extension.spotbugs,
                register,
                CustomSpotBugsPlugin) {
            project.configure(project) {
                spotbugs {
                    toolVersion = extension.spotbugsVersion
                    ignoreFailures = !extension.strict
                    effort = extension.spotbugsEffort
                    reportLevel = extension.spotbugsLevel

                    // in gradle 5 default 1g was changed and so spotbugs fails on large projects (recover behaviour),
                    // but not if value set manually
                    maxHeapSize.convention(extension.spotbugsMaxHeapSize)
                }

                // spotbugs annotations to simplify access to @SuppressFBWarnings
                // (applied according to plugin recommendation)
                if (extension.spotbugsAnnotations) {
                    dependencies {
                        compileOnly "com.github.spotbugs:spotbugs-annotations:${extension.spotbugsVersion}"
                    }
                }

                // plugins shortcut
                extension.spotbugsPlugins?.each {
                    project.configurations.getByName('spotbugsPlugins').dependencies.add(
                            project.dependencies.create(it)
                    )
                }

                tasks.withType(SpotBugsTask).configureEach { task ->
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
                            required.set(extension.htmlReports)
                        }
                    }
                    registerReporter(task, TOOL_SPOTBUGS)
                }
            }

            configurePluginTasks(project, extension, SpotBugsTask, TOOL_SPOTBUGS)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void applyCodeNarc(Project project, QualityExtension extension, ConfigLoader configLoader,
                               boolean register) {
        configurePlugin(project,
                extension.codenarc,
                register,
                CodeNarcPlugin) {
            project.configure(project) {
                codenarc {
                    toolVersion = extension.codenarcVersion
                    ignoreFailures = !extension.strict
                    configFile = configLoader.resolveCodenarcConfig(false)
                    sourceSets = extension.sourceSets
                }
                if (extension.codenarcGroovy4 && !extension.codenarcVersion.endsWith(CODENARC_GROOVY4)) {
                    // since codenarc 3.1 different groovy4-based jar could be used
                    dependencies {
                        codenarc "org.codenarc:CodeNarc:${extension.codenarcVersion}$CODENARC_GROOVY4"
                    }
                }
                tasks.withType(CodeNarc).configureEach { task ->
                    doFirst {
                        configLoader.resolveCodenarcConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports)

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
                    ignoreFailures = !extension.strict
                    sourceSets = extension.sourceSets
                }
                if (extension.animalsnifferVersion) {
                    animalsniffer.toolVersion = extension.animalsnifferVersion
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
        if (!extension.cpd) {
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
                    toolVersion = extension.pmdVersion
                    // assuming that in case of multi-module project quality plugin is applied in subprojects section
                    // and so it is normal that subproject configures root project
                    // Otherwise, side effect is possible that first configured module with quality plugin determines
                    // root project flag value
                    ignoreFailures = !extension.strict
                }
                // only default task is affected
                tasks.named('cpdCheck').configure {
                    doFirst {
                        if (extension.cpdUnifySources) {
                            applyExcludes(it, extension)
                        }
                    }
                }
            }
            // cpdCheck is always declared by cpd plugin
            TaskProvider<SourceTask> cpdCheck = CpdUtils.findCpdTask(prj)
            if (extension.cpdUnifySources) {
                // exclude sources, not declared for quality checks in quality plugin declaration project
                CpdUtils.unifyCpdSources(project, cpdCheck, extension.sourceSets)
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
            project.tasks.named('check').configure { it.dependsOn prj.tasks.withType(cpdTasksType) }

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
        if (extension.autoRegistration) {
            context.registerJavaPlugins = (extension.sourceSets.find { it.java.srcDirs.find { it.exists() } }) != null
            if (project.plugins.findPlugin(GroovyPlugin)) {
                context.registerGroovyPlugins =
                        (extension.sourceSets.find { it.groovy.srcDirs.find { it.exists() } }) != null
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
        if (!extension.enabled) {
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
        if (extension.exclude) {
            // exclude by patterns (relative to source roots)
            task.exclude extension.exclude
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
