package ru.vyarus.gradle.plugin.quality

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.*
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
class QualityPlugin implements Plugin<Project> {

    private static final DurationFormatter DURATION_FORMAT = new DurationFormatter()

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            QualityExtension extension = project.extensions.create('quality', QualityExtension, project)
            addInitConfigTask(project)

            project.afterEvaluate {
                ConfigLoader configLoader = new ConfigLoader(project)
                boolean hasJavaSources = extension.sourceSets.find { it.java.srcDirs.find { it.exists() } }

                // activate java plugins only when java sources exists
                if (hasJavaSources) {
                    configureJavac(project, extension)
                    applyCheckstyle(project, extension, configLoader)
                    applyPMD(project, extension, configLoader)
                    applyFindbugs(project, extension, configLoader)
                    configureAnimalSniffer(project, extension)
                }
                applyCodeNarc(project, extension, configLoader)
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
        project.tasks.withType(JavaCompile) {
            it.options.compilerArgs.addAll(extension.lintOptions.collect { "-Xlint:$it" })
        }
    }

    private void applyCheckstyle(Project project, QualityExtension extension, ConfigLoader configLoader) {
        if (!extension.checkstyle) {
            return
        }
        project.plugins.apply(CheckstylePlugin)
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
                // disable default html report (supported from gradle 2.10)
                if (reports.enabledReportNames.contains('html')) {
                    reports.html.enabled = false
                }
            }
        }
        applyReporter(project, 'checkstyle', new CheckstyleReporter(configLoader))
    }

    private void applyPMD(Project project, QualityExtension extension, ConfigLoader configLoader) {
        if (!extension.pmd) {
            return
        }
        project.plugins.apply(PmdPlugin)
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
        applyReporter(project, 'pmd', new PmdReporter())
    }

    private void applyFindbugs(Project project, QualityExtension extension, ConfigLoader configLoader) {
        if (!extension.findbugs) {
            return
        }
        project.plugins.apply(FindBugsPlugin)
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
        applyReporter(project, 'findbugs', new FindbugsReporter(configLoader))
    }

    private void applyCodeNarc(Project project, QualityExtension extension, ConfigLoader configLoader) {
        if (!extension.codenarc) {
            return
        }
        // apply only if groovy enabled
        project.plugins.withType(GroovyPlugin) {
            boolean hasGroovySources = extension.sourceSets.find { it.groovy.srcDirs.find { it.exists() } }
            if (hasGroovySources) {
                project.plugins.apply(CodeNarcPlugin)
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
                applyReporter(project, 'codenarc', new CodeNarcReporter())
            }
        }
    }

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
        }
    }

    private void applyReporter(Project project, String type, Reporter reporter) {
        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            if (task.name.startsWith(type)) {
                long start = System.currentTimeMillis()

                reporter.report(project, task.name[type.length()..-1].toLowerCase())

                String duration = DURATION_FORMAT.format(System.currentTimeMillis() - start)
                project.logger.info("[plugin:quality] reporting executed in $duration")
            }
        }
    }
}
