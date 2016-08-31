package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.*
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.reporting.DurationFormatter
import ru.vyarus.gradle.plugin.quality.report.*
import ru.vyarus.gradle.plugin.quality.task.InitQualityConfigTask

/**
 * Quality plugin enables and configures quality plugins for java and groovy projects.
 * Plugin must be registered after java or groovy plugins, otherwise wil do nothing.
 * <p>
 * Java project is detected by presence of java sources. In this case Checkstyle, PMD and FindBugs plugins are
 * activated. Also, additional javac lint options are activated to show more warnings during compilation.
 * <p>
 * If groovy plugin enabled, CodeNarc plugin activated.
 * <p>
 * All plugins are configured to produce xml and html reports. For checkstyle and findbugs html reports
 * generated manually (gradle 2.10 added checkstyle html report by default, plugin have to disable it to grant
 * consistent behaviour). All plugins violations are printed into console in unified format which makes console
 * output good enough for fixing violations.
 * <p>
 * Plugin may be configured with 'quality' closure. See {@link QualityExtension} for configuration options.
 * <p>
 * By default plugin use bundled quality plugins configurations. These configs could be copied into project
 * with 'initQualityConfig' task (into quality.configDir directory). These custom configs will be used in
 * priority with fallback to default config if config not found.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @see CodeNarcPlugin
 * @see CheckstylePlugin
 * @see PmdPlugin
 * @see FindBugsPlugin
 */
@CompileStatic
class QualityPlugin implements Plugin<Project> {

    private static final DurationFormatter DURATION_FORMAT = new DurationFormatter()

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)
            addInitConfigTask(project)

            project.afterEvaluate {
                Context context = createContext(project, extension)
                ConfigLoader configLoader = new ConfigLoader(project)

                configureJavac(project, extension)
                applyCheckstyle(project, extension, configLoader, context.registerJavaPlugins)
                applyPMD(project, extension, configLoader, context.registerJavaPlugins)
                applyFindbugs(project, extension, configLoader, context.registerJavaPlugins)
                configureAnimalSniffer(project, extension)
                applyCodeNarc(project, extension, configLoader, context.registerGroovyPlugins)
            }
        }
    }

    private void addInitConfigTask(Project project) {
        project.tasks.create('initQualityConfig', InitQualityConfigTask)
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
                    }
                }
            }
            applyReporter(project, 'checkstyle', new CheckstyleReporter(configLoader), extension.consoleReporting)
            applyEnabledState(project, extension, Checkstyle)
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
                    ruleSetFiles = files(configLoader.resolvePmdConfig(false).absolutePath)
                    sourceSets = extension.sourceSets
                }
                tasks.withType(Pmd) {
                    doFirst {
                        configLoader.resolvePmdConfig()
                    }
                }
            }
            applyReporter(project, 'pmd', new PmdReporter(), extension.consoleReporting)
            applyEnabledState(project, extension, Pmd)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applyFindbugs(Project project, QualityExtension extension, ConfigLoader configLoader,
                               boolean register) {
        configurePlugin(project,
                extension.findbugs,
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
                    }
                    reports {
                        xml {
                            enabled true
                            withMessages true
                        }
                    }
                }
            }
            applyReporter(project, 'findbugs', new FindbugsReporter(configLoader), extension.consoleReporting)
            applyEnabledState(project, extension, FindBugs)
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
                    }
                    reports {
                        xml.enabled = true
                        html.enabled = true
                    }
                }
            }
            applyReporter(project, 'codenarc', new CodeNarcReporter(), extension.consoleReporting)
            applyEnabledState(project, extension, CodeNarc)
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
                    String duration = DURATION_FORMAT.format(System.currentTimeMillis() - start)
                    task.project.logger.info("[plugin:quality] $type reporting executed in $duration")
                }
            }
        }
    }

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

    private void applyEnabledState(Project project, QualityExtension extension, Class task) {
        if (!extension.enabled) {
            project.gradle.taskGraph.whenReady {
                Task called = project.gradle.taskGraph.allTasks.last()
                (project.tasks.withType(task) as TaskCollection<Task>).each { t ->
                    // enable task only if it's called directly
                    t.enabled = called == t
                }
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
