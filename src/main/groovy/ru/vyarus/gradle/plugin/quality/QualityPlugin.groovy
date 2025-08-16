package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.build.event.BuildEventsListenerRegistry
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.service.TasksListenerService
import ru.vyarus.gradle.plugin.quality.task.InitQualityConfigTask
import ru.vyarus.gradle.plugin.quality.task.QualityToolVersionsTask
import ru.vyarus.gradle.plugin.quality.tool.ProjectLang
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

    /**
     * Applies exclude path patterns to quality tasks.
     * Note: this does not apply to animalsniffer. For spotbugs this appliance is useless, see custom support above.
     * <p>
     * The method is static because it is referenced from runtime.
     *
     * @param task quality task
     * @param excludeSources sources to exclude
     * @param exclude exclude patterns
     */
    static void applyExcludes(SourceTask task, FileCollection excludeSources, List<String> exclude) {
        if (excludeSources) {
            // directly excluded sources
            task.source = task.source - excludeSources
        }
        if (exclude) {
            // exclude by patterns (relative to source roots)
            task.exclude exclude
        }
    }

    @Inject
    abstract BuildEventsListenerRegistry getEventsListenerRegistry()

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)

            Provider<ConfigsService> configs = initConfigService(project, extension)
            Provider<TasksListenerService> tasksListener = initListenerService(project, extension, configs)
            addTasks(project, extension, configs)

            project.afterEvaluate {
                configureGroupingTasks(project)

                List<ProjectLang> langs = detectLangs(project, extension)
                project.tasks.withType(QualityToolVersionsTask)
                        .configureEach { it.languages.addAll(langs) }

                ToolContext context = new ToolContext(project, extension, configs, tasksListener, langs,
                        qualityTasks, reportersData)

                TOOLS.each { QualityTool tool ->
                    // tool language only affects auto registration because if plugin registered manually
                    // it is still must be configured
                    boolean autoRegister = langs.intersect(tool.supportedLanguages).size() > 0
                    tool.configure(context, autoRegister)
                }
            }
        }
    }

    protected Provider<ConfigsService> initConfigService(Project project, QualityExtension extension) {
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
        return configs
    }

    protected Provider<TasksListenerService> initListenerService(Project project, QualityExtension extension,
                                                                 Provider<ConfigsService> configs) {
        // Service created per project because otherwise it is impossible to initialize tasks map properly
        // (singleton service created in the first project and configurations from other modules not persisted)
        Provider<TasksListenerService> tasksListener = project.gradle.sharedServices.registerIfAbsent(
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
        return tasksListener
    }

    protected void addTasks(Project project, QualityExtension extension, Provider<ConfigsService> configsService) {
        project.tasks.register('initQualityConfig', InitQualityConfigTask) {
            it.configs.set(configsService)
        }
        project.tasks.register('qualityToolVersions', QualityToolVersionsTask) {
            it.checkstyleVersion.set(project.provider {
                extension.checkstyle.get() ? extension.checkstyleVersion.get() : 'disabled'
            })

            it.pmdVersion.set(project.provider {
                extension.pmd.get() ? extension.pmdVersion.get() : 'disabled'
            })

            it.spotBugsVersion.set(project.provider {
                extension.spotbugs.get() ? extension.spotbugsVersion.get() : 'disabled'
            })

            it.codeNarcVersion.set(project.provider {
                extension.codenarc.get() ? extension.codenarcVersion.get() : 'disabled'
            })
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
    protected List<ProjectLang> detectLangs(Project project, QualityExtension extension) {
        List<ProjectLang> langs = []
        if (extension.autoRegistration.get()) {
            if ((extension.sourceSets.get()
                    .find { it.java.srcDirs.find { it.exists() } }) != null) {
                langs.add(ProjectLang.Java)
            }
            if (project.plugins.findPlugin(GroovyPlugin) && (extension.sourceSets.get()
                    .find { it.groovy.srcDirs.find { it.exists() } }) != null) {
                langs.add(ProjectLang.Groovy)
            }
        }
        return langs
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
}
