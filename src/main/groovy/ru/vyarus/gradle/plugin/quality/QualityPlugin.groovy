package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
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
import ru.vyarus.gradle.plugin.quality.report.CodeNarcReporter
import ru.vyarus.gradle.plugin.quality.report.SpotbugsReporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.factory.CpdModelFactory
import ru.vyarus.gradle.plugin.quality.report.model.factory.ModelFactory
import ru.vyarus.gradle.plugin.quality.report.model.factory.SpotbugsModelFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
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
    public static final String CONFIGS_SERVICE = 'qualityConfigs'

    private static final String JAR_NAME = 'gradle-quality-plugin-'
    private static final String QUALITY_TASK = 'checkQuality'
    private static final String CODENARC_GROOVY4 = '-groovy-4.0'

    // need to collect all task data, required for reporting (under config cache)
    private final Map<String, ModelFactory> taskDescFactory = initFactories()
    // cacheable tasks data (cached by tasksListener)
    // Works due to lazy properties init: collections filled under configuration time; service configured lazily
    // and started only at runtime, so service receive complete data and cache it is its property for config. cache
    private final List<TaskDesc> qualityTasks = []
    private final Map<String, Object> reportersData = [:]
    private Provider<TasksListenerService> tasksListener

    @Inject
    abstract BuildEventsListenerRegistry getEventsListenerRegistry()

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)

            Provider<ConfigsService> configs = initServices(project, extension)
            addTasks(project, extension, configs)

            project.afterEvaluate {
                configureGroupingTasks(project)

                Context context = createContext(project, extension)
                project.tasks.withType(QualityToolVersionsTask)
                        .configureEach { it.context.set(context) }

                configureJavac(project, extension)
                if (JavaVersion.current().java11Compatible) {
                    applyCheckstyle(project, extension, configs, context.registerJavaPlugins)
                }
                applyPMD(project, extension, configs, context.registerJavaPlugins)
                applySpotbugs(project, extension, configs, context.registerJavaPlugins)
                configureAnimalSniffer(project, extension)
                configureCpdPlugin(project, extension, configs,
                        !context.registerJavaPlugins && context.registerGroovyPlugins)
                applyCodeNarc(project, extension, configs, context.registerGroovyPlugins)
            }
        }
    }

    protected Provider<ConfigsService> initServices(Project project, QualityExtension extension) {
        Provider<ConfigsService> configs = project.gradle.sharedServices.registerIfAbsent(
                CONFIGS_SERVICE, ConfigsService, spec -> {
            spec.maxParallelUsages.set(1)
            spec.parameters {
                (it as ConfigsService.Params).configDir.set(
                        project.layout.file(project
                                .provider { project.rootProject.file(extension.configDir.get()) }))
                (it as ConfigsService.Params).tempDir.set(createTmpConfigsDir(project.layout))
            }
        })
        // just to avoid early destroy
        eventsListenerRegistry.onTaskCompletion(configs)

        // Service created per project because otherwise it is impossible to initialize tasks map properly
        // (singleton service created in the first project and configurations from other modules not persisted)
        tasksListener = project.gradle.sharedServices.registerIfAbsent(
                "qualityEvents$project.name", TasksListenerService, spec -> {
            spec.maxParallelUsages.set(1)
            spec.parameters {
                (it as TasksListenerService.Params).with {
                    configsService.set(configs)
                    htmlReports.set(extension.htmlReports)
                    consoleReporting.set(extension.consoleReporting)
                    // it is important to not use service at configuration time so list would contain
                    // all quality tasks (and would be stored by configuration cache)
                    qualityTasks.set(this.qualityTasks)
                    reportersData.set(this.reportersData)
                }
            }
        })
        eventsListenerRegistry.onTaskCompletion(tasksListener)

        return configs
    }

    protected void addTasks(Project project, QualityExtension extension, Provider<ConfigsService> configsService) {
        project.tasks.register('initQualityConfig', InitQualityConfigTask) {
            it.configs.set(configsService)
        }
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
    protected void applyCheckstyle(Project project, QualityExtension extension, Provider<ConfigsService> configs,
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
                    configFile = configs.get().resolveCheckstyleConfig(false)
                    // this is required for ${config_loc} variable, but this will ALWAYS point to
                    // gradle/config/checkstyle/ (or other configured config location dir) because custom
                    // configuration files may be only there
                    configDirectory = configs.get().resolveCheckstyleConfigDir()
                    sourceSets = extension.sourceSets.get()
                }

                tasks.withType(Checkstyle).configureEach { task ->
                    doFirst {
                        configs.get().resolveCheckstyleConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports.get())
                    registerTaskForReport(task, TOOL_CHECKSTYLE)
                    reportAfterTask(task)
                }
            }
            configurePluginTasks(project, extension, Checkstyle, TOOL_CHECKSTYLE)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void applyPMD(Project project, QualityExtension extension, Provider<ConfigsService> configs,
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
                    ruleSetFiles = files(configs.get().resolvePmdConfig(false).absolutePath)
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
                        configs.get().resolvePmdConfig()
                        applyExcludes(it, extension)
                    }
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports.get())
                    registerTaskForReport(task, TOOL_PMD)
                    reportAfterTask(task)
                }
            }
            configurePluginTasks(project, extension, Pmd, TOOL_PMD)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings(['MethodSize', 'NestedBlockDepth', 'AbcMetric'])
    protected void applySpotbugs(Project project, QualityExtension extension, Provider<ConfigsService> configs,
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
                        configs.get().resolveSpotbugsExclude()
                        // it is not possible to substitute filter file here (due to locked Property)
                        // but possible to update already configured file (it must be already a temp file here)
                        SpotbugsUtils.replaceExcludeFilter(task, extension, logger)
                    }
                    // have to use this way instead of doFirst hook, because nothing else will work (damn props!)
                    excludeFilter.set(project.provider(new SpotbugsExclusionConfigProvider(
                            task, configs, extension
                    )))
                    // read plugin error descriptions under configuration time
                    readSpotbugsPluginsDescriptors(project)
                    reports {
                        xml {
                            required.set(true)
                        }
                        html {
                            required.set(extension.htmlReports.get())
                        }
                    }
                    registerTaskForReport(task, TOOL_SPOTBUGS)
                    reportAfterTask(task)
                }
            }

            configurePluginTasks(project, extension, spotbugsTaskType, TOOL_SPOTBUGS)
        }
    }

    protected void readSpotbugsPluginsDescriptors(Project project) {
        synchronized (reportersData) {
            if (!reportersData.containsKey(TOOL_SPOTBUGS)) {
                reportersData.put(TOOL_SPOTBUGS, SpotbugsReporter.resolvePluginsChecks(project))
            }
        }
    }

    protected void readCodenarcProperties(Project project) {
        synchronized (reportersData) {
            if (!reportersData.containsKey(TOOL_CODENARC)) {
                reportersData.put(TOOL_CODENARC, CodeNarcReporter.loadCodenarcProperties(project))
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void applyCodeNarc(Project project, QualityExtension extension, Provider<ConfigsService> configs,
                                 boolean register) {
        configurePlugin(project,
                extension.codenarc.get(),
                register,
                CodeNarcPlugin) {
            project.configure(project) {
                codenarc {
                    toolVersion = extension.codenarcVersion.get()
                    ignoreFailures = !extension.strict.get()
                    configFile = configs.get().resolveCodenarcConfig(false)
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
                        configs.get().resolveCodenarcConfig()
                        applyExcludes(it, extension)
                    }
                    reportAfterTask(task)
                    // read codenarc properties under configuration phase
                    readCodenarcProperties(project)
                    reports.xml.required.set(true)
                    reports.html.required.set(extension.htmlReports.get())
                    registerTaskForReport(task, TOOL_CODENARC)
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
    protected void configureCpdPlugin(Project project, QualityExtension extension, Provider<ConfigsService> configs,
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
                    configs.get().resolveCpdXsl()
                }
                registerTaskForReport(task, TOOL_CPD)
                reportAfterTask(task)
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

    /**
     * Each quality task is registered directly to properly apply reporting.
     * <p>
     * {@code useFullTaskName} is required for CPD task only, which use single task for all source sets.
     *
     * @param task task instance
     * @param type task type (to reference reporter and resolve source set)
     * @param useFullTaskName (true to use task name for a report file name instead of source set name)
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerTaskForReport(Task task, String type) {
        qualityTasks.add(taskDescFactory[type].buildDesc(task, type))
    }

    protected void reportAfterTask(Task task) {
        // This is the only way to print report DIRECTLY AFTER the task output (like it was before).
        // Also, the only way to report after not executed task (from cache)
        // If task execution fails, doLast block would not be called at all, and in this case build service
        // task listener would log it
        task.doLast {
            tasksListener.get().execute(task.path)
        }
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

    protected Provider<RegularFile> createTmpConfigsDir(ProjectLayout layout) {
        // use plugin version to avoid case when default configs used and old cache being used for
        // a new plugin version (usually leading to silly errors)
        String version = 'unknown_version'
        String location = this.class.protectionDomain.codeSource.location
        int end = location.indexOf('.jar')
        if (end > 0) {
            int start = location.indexOf(JAR_NAME)
            version = location.substring(start + JAR_NAME.length(), end)
        }

        return layout.buildDirectory.file("quality-configs/$version")
    }

    protected Map<String, ModelFactory> initFactories() {
        Map<String, ModelFactory> res = [:]
        res[TOOL_CHECKSTYLE] = new ModelFactory()
        res[TOOL_PMD] = new ModelFactory()
        res[TOOL_SPOTBUGS] = new SpotbugsModelFactory()
        res[TOOL_CODENARC] = new ModelFactory()
        res[TOOL_CPD] = new CpdModelFactory()
        return res
    }

    /**
     * Internal configuration context.
     */
    static class Context {
        boolean registerJavaPlugins
        boolean registerGroovyPlugins
    }
}
