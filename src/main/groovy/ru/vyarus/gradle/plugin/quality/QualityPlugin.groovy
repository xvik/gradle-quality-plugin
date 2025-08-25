package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.build.event.BuildEventsListenerRegistry
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.service.TasksListenerService
import ru.vyarus.gradle.plugin.quality.task.CopyConfigsTask
import ru.vyarus.gradle.plugin.quality.task.InitQualityConfigTask
import ru.vyarus.gradle.plugin.quality.task.QualityToolVersionsTask
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.animalsniffer.AnimalsnifferTool
import ru.vyarus.gradle.plugin.quality.tool.checkstyle.CheckstyleTool
import ru.vyarus.gradle.plugin.quality.tool.codenarc.CodenarcTool
import ru.vyarus.gradle.plugin.quality.tool.cpd.CpdTool
import ru.vyarus.gradle.plugin.quality.tool.javac.JavacTool
import ru.vyarus.gradle.plugin.quality.tool.pmd.PmdTool
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.SpotbugsTool

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
 */
@CompileStatic
abstract class QualityPlugin implements Plugin<Project> {

    public static final String CONFIGS_SERVICE = 'qualityConfigs'
    public static final String QUALITY_TASK = 'checkQuality'

    private static final String JAR_NAME = 'gradle-quality-plugin-'

    public static final List<QualityTool> TOOLS = [
            new JavacTool(),
            new CheckstyleTool(),
            new PmdTool(),
            new SpotbugsTool(),
            new CodenarcTool(),
            new AnimalsnifferTool(),
            new CpdTool(),
    ]

    // cacheable tasks data (cached by tasksListener)
    // Works due to lazy properties init: collections filled under configuration time; service configured lazily
    // and started only at runtime, so service receive complete data and cache it property
    private final List<TaskDesc> qualityTasks = []
    private final Map<String, Object> reportersData = [:]

    @Inject
    abstract BuildEventsListenerRegistry getEventsListenerRegistry()

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)

            Provider<ConfigsService> configs = initConfigService(project, extension)
            Provider<TasksListenerService> tasksListener = initListenerService(project, extension, configs)
            // langs used in lazy-initialized tool versions task
            List<ProjectSources> langs = []
            TaskProvider<CopyConfigsTask> configsTask = addTasks(project, extension, configs, langs)

            project.afterEvaluate {
                configureGroupingTasks(project)
                langs.addAll(detectLangs(project, extension))

                ToolContext context = new ToolContext(project, extension, configs, tasksListener, langs,
                        qualityTasks, reportersData, configsTask)

                TOOLS.each { QualityTool tool ->
                    // tool language only affects auto registration because if plugin registered manually
                    // it is still must be configured
                    boolean autoRegister = langs.intersect(tool.autoEnableForSources).size() > 0
                    tool.configure(context, autoRegister)
                }
            }
        }
    }

    protected Provider<ConfigsService> initConfigService(Project project, QualityExtension extension) {
        // service HAVE to be project-specific because copyConfigs task MUST use different temp folders,
        // otherwise gradle would complain about implicit tasks dependencies
        Provider<ConfigsService> configs = project.gradle.sharedServices.registerIfAbsent(
                CONFIGS_SERVICE + project.path, ConfigsService, spec -> {
            spec.maxParallelUsages.set(1)
            spec.parameters {
                (it as ConfigsService.Params).configDir.set(
                        project.rootProject.layout.dir(project
                                .provider { project.rootProject.file(extension.configDir.get()) }))
                // not a rootproject! because otherwise gradle would complain in multi-module project (as module tasks
                // would write into one top directory and gradle would assume cross project dependencies)
                (it as ConfigsService.Params).tempDir.set(project.layout.buildDirectory.dir('quality-configs'))
            }
        })
        // just to avoid early destroy
        eventsListenerRegistry.onTaskCompletion(configs)
        return configs
    }

    protected Provider<TasksListenerService> initListenerService(Project project, QualityExtension extension,
                                                                 Provider<ConfigsService> configs) {
        // Service created per project because otherwise it is impossible to initialize tasks map properly
        // (singleton service created in the first project and configurations from other modules not persisted)
        Provider<TasksListenerService> tasksListener = project.gradle.sharedServices.registerIfAbsent(
                "qualityEvents$project.path", TasksListenerService, spec -> {
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
        return tasksListener
    }

    protected TaskProvider<CopyConfigsTask> addTasks(Project project,
                                                     QualityExtension extension,
                                                     Provider<ConfigsService> configsService,
                                                     List<ProjectSources> langs) {
        project.tasks.register('initQualityConfig', InitQualityConfigTask) {
            it.configsService.set(configsService)
        }
        project.tasks.register('qualityToolVersions', QualityToolVersionsTask) {
            it.toolsInfo.set(project.provider {
                List<String> res = []
                TOOLS.each {
                    String info = it.getToolInfo(project, extension, langs)
                    if (info) {
                        res.add(info)
                    }
                }
                return res
            })
        }
        // task initialize default configuration files. It is important to use task for this because otherwise
        // it would be impossible to properly support caching (because files would be created dynamically)
        project.tasks.register('copyQualityConfigs', CopyConfigsTask) {
            it.configsService.set(configsService)
            it.exclude.set(extension.exclude)
            // can't use collection directly due to side effects of using other tasks outputs directly
            // so compute hash to still detect config changes and re-execute copying
            it.excludeSources.convention(
                    // "hash" is a sum of all configured paths.. not ideal but should work for most cases
                    // (copyConfigs task may not detect changes in filter/matcher patterns)
                    getCollectionHash(extension.excludeSources)
            )
            // important to invalidate task cache on plugin version change (in order to update default configs)
            it.pluginVersion.set(pluginVersion)
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
                    group = JavaBasePlugin.VERIFICATION_GROUP
                    description = "Run quality plugins for $set.name source set"
                }
            }
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
    protected List<ProjectSources> detectLangs(Project project, QualityExtension extension) {
        List<ProjectSources> langs = []
        if (extension.autoRegistration.get()) {
            if ((extension.sourceSets.get()
                    .find { it.java.srcDirs.find { it.exists() } }) != null) {
                langs.add(ProjectSources.Java)
            }
            if (project.plugins.findPlugin(GroovyPlugin) && (extension.sourceSets.get()
                    .find { it.groovy.srcDirs.find { it.exists() } }) != null) {
                langs.add(ProjectSources.Groovy)
            }
        }
        return langs
    }

    protected String getPluginVersion() {
        String version = 'unknown_version'
        String location = this.class.protectionDomain.codeSource.location
        int end = location ? location.indexOf('.jar') : -1
        if (end > 0) {
            int start = location.indexOf(JAR_NAME)
            version = location.substring(start + JAR_NAME.length(), end)
        }

        return version
    }

    // this is actually a quite bad hack: only major cases would be covered, when base directories change
    // but fine tunings with filter and matches will not affect hash and so copyConfigs task could remain up-to-date
    // Anyway, this is better then without detection at all (collection can't be used directly on task because
    // it might be build from the other task output which make gradle complain)
    @SuppressWarnings(['Instanceof', 'DuplicateStringLiteral'])
    protected String getCollectionHash(FileCollection collection) {
        if (!collection) {
            return '0'
        }
        if (collection instanceof ConfigurableFileCollection) {
            // specific filters ignored (no way to track it here)
            return (collection as ConfigurableFileCollection).from*.toString().join(';').length()
        }
        if (collection instanceof ConfigurableFileTree) {
            // matchers also not counted
            return (collection as ConfigurableFileTree).dir.toString().length()
        }
        return String.valueOf(collection.size())
    }
}
