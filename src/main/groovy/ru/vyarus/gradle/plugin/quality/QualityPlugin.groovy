package ru.vyarus.gradle.plugin.quality

import com.github.spotbugs.SpotBugsPlugin
import com.github.spotbugs.SpotBugsTask
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.*
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.compile.JavaCompile
import ru.vyarus.gradle.plugin.quality.report.*
import ru.vyarus.gradle.plugin.quality.task.InitQualityConfigTask
import ru.vyarus.gradle.plugin.quality.util.DurationFormatter
import ru.vyarus.gradle.plugin.quality.util.FindbugsUtils
import ru.vyarus.gradle.plugin.quality.util.SpotbugsUtils

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
 * Spotbugs is activated instead of findbugs, but it still possible to disable spotbugs and use findbugs instead
 * (findbugs support is marked as deprecated now and will be removed someday to avoid confusion).
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
 * @see com.github.spotbugs.SpotBugsPlugin
 */
@CompileStatic
class QualityPlugin implements Plugin<Project> {

    private static final String QUALITY_TASK = 'checkQuality'

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)
            addInitConfigTask(project)

            project.afterEvaluate {
                configureGroupingTasks(project)

                Context context = createContext(project, extension)
                ConfigLoader configLoader = new ConfigLoader(project)

                configureJavac(project, extension)
                applyCheckstyle(project, extension, configLoader, context.registerJavaPlugins)
                applyPMD(project, extension, configLoader, context.registerJavaPlugins)
                // enable only if findbugs wasn't registered manually
                applySpotbugs(project, extension, configLoader,
                        context.registerJavaPlugins && !project.plugins.hasPlugin(FindBugsPlugin))
                // enable only if spotbugs wasn't registered neither automatically nor manually
                applyFindbugs(project, extension, configLoader,
                        context.registerJavaPlugins && !SpotbugsUtils.isPluginEnabled(project))
                configureAnimalSniffer(project, extension)
                applyCodeNarc(project, extension, configLoader, context.registerGroovyPlugins)
            }
        }
    }

    private void addInitConfigTask(Project project) {
        project.tasks.create('initQualityConfig', InitQualityConfigTask)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureGroupingTasks(Project project) {
        // create checkQualityMain, checkQualityTest (quality tasks for all source sets)
        // using all source sets and not just declared in extension to be able to run quality plugins
        // on source sets which are not included in check task run (e.g. run quality on tests time to time)
        project.sourceSets.each { SourceSet set ->
            project.tasks.create(set.getTaskName(QUALITY_TASK, null)).with {
                group = 'verification'
                description = "Run quality plugins for $set.name source set"
            }
        }
    }

    private void configureJavac(Project project, QualityExtension extension) {
        if (!extension.lintOptions) {
            return
        }
        project.tasks.withType(JavaCompile) { JavaCompile t ->
            t.options.compilerArgs.addAll(extension.lintOptions.collect { "-Xlint:$it" as String })
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applyCheckstyle(Project project, QualityExtension extension, ConfigLoader configLoader,
                                 boolean register) {
        configurePlugin(project,
                extension.checkstyle,
                register,
                CheckstylePlugin) {
            project.configure(project) {
                checkstyle {
                    showViolations = false
                    toolVersion = extension.checkstyleVersion
                    ignoreFailures = !extension.strict
                    configFile = configLoader.resolveCheckstyleConfig(false)
                    sourceSets = extension.sourceSets
                }
                tasks.withType(Checkstyle) {
                    doFirst {
                        configLoader.resolveCheckstyleConfig()
                        applyExcludes(it, extension)
                    }
                }
            }
            configurePluginTasks(project, extension, Checkstyle, 'checkstyle', new CheckstyleReporter(configLoader))
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applyPMD(Project project, QualityExtension extension, ConfigLoader configLoader,
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
                tasks.withType(Pmd) {
                    doFirst {
                        configLoader.resolvePmdConfig()
                        applyExcludes(it, extension)
                    }
                }
            }
            configurePluginTasks(project, extension, Pmd, 'pmd', new PmdReporter())
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applySpotbugs(Project project, QualityExtension extension, ConfigLoader configLoader,
                               boolean register) {
        configurePlugin(project,
                extension.spotbugs,
                register,
                SpotBugsPlugin) {
            project.configure(project) {
                spotbugs {
                    toolVersion = extension.spotbugsVersion
                    ignoreFailures = !extension.strict
                    effort = extension.spotbugsEffort
                    reportLevel = extension.spotbugsLevel
                    excludeFilter = configLoader.resolveSpotbugsExclude(false)
                    sourceSets = extension.sourceSets
                }

                tasks.withType(SpotBugsTask) {
                    doFirst {
                        configLoader.resolveSpotbugsExclude()
                        // spotbugs does not support exclude of SourceTask, so appending excluded classes to
                        // xml exclude filter
                        if (extension.exclude || extension.excludeSources) {
                            SpotbugsUtils.replaceExcludeFilter(it, extension, logger)
                        }
                    }
                    reports {
                        xml {
                            enabled true
                            withMessages true
                        }
                    }
                }
            }
            configurePluginTasks(project, extension, SpotBugsTask, 'spotbugs', new SpotbugsReporter(configLoader))
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @Deprecated
    private void applyFindbugs(Project project, QualityExtension extension, ConfigLoader configLoader,
                               boolean register) {
        configurePlugin(project,
                // ignored if spotbugs enabled
                extension.findbugs && !extension.spotbugs,
                register,
                FindBugsPlugin) {
            project.configure(project) {
                findbugs {
                    toolVersion = extension.findbugsVersion
                    ignoreFailures = !extension.strict
                    effort = extension.findbugsEffort
                    reportLevel = extension.findbugsLevel
                    excludeFilter = configLoader.resolveFindbugsExclude(false)
                    sourceSets = extension.sourceSets
                }

                tasks.withType(FindBugs) {
                    doFirst {
                        configLoader.resolveFindbugsExclude()
                        // findbugs does not support exclude of SourceTask, so appending excluded classes to
                        // xml exclude filter
                        if (extension.exclude || extension.excludeSources) {
                            FindbugsUtils.replaceExcludeFilter(it, extension, logger)
                        }
                    }
                    reports {
                        xml {
                            enabled true
                            withMessages true
                        }
                    }
                }
            }
            configurePluginTasks(project, extension, FindBugs, 'findbugs', new FindbugsReporter(configLoader))
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applyCodeNarc(Project project, QualityExtension extension, ConfigLoader configLoader,
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
                tasks.withType(CodeNarc) {
                    doFirst {
                        configLoader.resolveCodenarcConfig()
                        applyExcludes(it, extension)
                    }
                    reports {
                        xml.enabled = true
                        html.enabled = true
                    }
                }
            }
            configurePluginTasks(project, extension, CodeNarc, 'codenarc', new CodeNarcReporter())
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureAnimalSniffer(Project project, QualityExtension extension) {
        project.plugins.withId('ru.vyarus.animalsniffer') {
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
                    it.class.classLoader.loadClass('ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer'))
            groupQualityTasks(project, 'animalsniffer')
        }
    }

    private void applyReporter(Project project, String type, Reporter reporter, boolean consoleReport) {
        boolean generatesHtmlReport = HtmlReportGenerator.isAssignableFrom(reporter.class)
        if (!consoleReport && !generatesHtmlReport) {
            // nothing to do at all
            return
        }
        // in multi-project reporter registered for each project, but all gets called on task execution in any module
        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            if (task.name.startsWith(type) && project == task.project) {

                String taskType = task.name[type.length()..-1].toLowerCase()
                if (generatesHtmlReport) {
                    (reporter as HtmlReportGenerator).generateHtmlReport(project, taskType)
                }
                if (consoleReport) {
                    long start = System.currentTimeMillis()
                    reporter.report(task.project, taskType)
                    String duration = DurationFormatter.format(System.currentTimeMillis() - start)
                    task.project.logger.info("[plugin:quality] $type reporting executed in $duration")
                }
            }
        }
    }

    /**
     * Detects available source folders in configured source sets to understand
     * what sources are available: groovy, java or both. Based on that knowledge
     * appropriate plugins could be registered.
     *
     * @param project
     * @param extension
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    private Context createContext(Project project, QualityExtension extension) {
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
    private void configurePlugin(Project project, boolean enabled, boolean register, Class plugin, Closure config) {
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

    /**
     * Applies reporter, enabled state control and checkQuality* grouping tasks.
     *
     * @param project project instance
     * @param extension extension instance
     * @param taskType task class
     * @param task task base name
     * @param reporter plugin specific reporter instance
     */
    private void configurePluginTasks(Project project, QualityExtension extension,
                                      Class taskType, String task, Reporter reporter) {
        applyReporter(project, task, reporter, extension.consoleReporting)
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
    private void applyEnabledState(Project project, QualityExtension extension, Class task) {
        if (!extension.enabled) {
            project.gradle.taskGraph.whenReady {
                Task called = project.gradle.taskGraph.allTasks.last()
                (project.tasks.withType(task) as TaskCollection<Task>).each { t ->
                    // enable task only if it's called directly or through grouping task
                    t.enabled = called == t || called.name.startsWith(QUALITY_TASK)
                }
            }
        }
    }

    /**
     * Applies exclude path patterns to quality tasks.
     * Note: this does not apply to animalsniffer; for findbugs this appliance is useless, see custom support above.
     *
     * @param task quality task
     * @param extension extension instance
     */
    private void applyExcludes(SourceTask task, QualityExtension extension) {
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
    private void groupQualityTasks(Project project, String task) {
        // each quality plugin generate separate tasks for each source set
        // assign plugin tasks to source set grouping quality task
        project.sourceSets.each {
            Task pluginTask = project.tasks.findByName(it.getTaskName(task, null))
            if (pluginTask) {
                project.tasks.getByName(it.getTaskName(QUALITY_TASK, null)).dependsOn << pluginTask
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
