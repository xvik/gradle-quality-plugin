package ru.vyarus.gradle.plugin.quality.tool.spotbugs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.report.SpotbugsReporter
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.report.SpotbugsTaskDescFactory
import ru.vyarus.gradle.plugin.quality.util.SourceSetUtils

/**
 * Spotbugs support. Spotbugs plugin must be registered manually (due its limitation to java 11, it can't be
 * provided by quality plugin as transitive dependency). Plugin may not be activated (apply false) - registration
 * in build classpath is enough (multi-module case when plugin must only be applied in the root project and
 * quality plugin would find and activate spotbugs in sub modules).
 * <p>
 * Default spotbugs plugin applies all tasks as "check" dependency. Quality plugin has to override check task
 * dependencies to fix that.
 * <p>
 * Source lebel exclusions implemented with exclusionFilter file, because spotbugs tasks are not
 * {@link org.gradle.api.tasks.SourceTask} like other quality plugins.
 *
 * @author Vyacheslav Rusakov
 * @since 16.08.2025
 * @see com.github.spotbugs.snom.SpotBugsPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings(['GetterMethodCouldBeProperty', 'PropertyName'])
class SpotbugsTool implements QualityTool {
    static final String NAME = 'spotbugs'

    static final String spotbugs_exclude = 'spotbugs/exclude.xml'

    TaskDescFactory factory = new SpotbugsTaskDescFactory()

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectSources> getAutoEnableForSources() {
        return [ProjectSources.Java]
    }

    @Override
    List<String> getConfigs() {
        return [spotbugs_exclude]
    }

    @Override
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs) {
        if (project.plugins.hasPlugin('com.github.spotbugs')) {
            return 'SpotBugs: ' + (extension.spotbugs.get() ? extension.spotbugsVersion.get() : 'disabled')
        }
        return null
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        new SpotbugsReporter(param as Map<String, String>)
    }

    @Override
    void configure(ToolContext context, boolean register) {
        // if plugin is not on classpath - do nothing; if plugin in classpath but not applied - apply it
        Class<? extends Plugin> plugin = SpotbugsUtils.findPluginClass(context.project)
        if (plugin == null) {
            return
        }
        SpotbugsUtils.validateRankSetting(context.extension.spotbugsMaxRank.get())

        Class<? extends Task> spotbugsTaskType = plugin.classLoader
                .loadClass('com.github.spotbugs.snom.SpotBugsTask') as Class<? extends Task>

        if (!context.extension.spotbugs.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(plugin, register) {
            configureSpotbugs(plugin, spotbugsTaskType, context.project, context.extension, context)
        }
    }

    @SuppressWarnings(['AbcMetric', 'MethodSize', 'ParameterCount'])
    private void configureSpotbugs(Class<? extends Plugin> plugin, Class<? extends Task> spotbugsTaskType,
                                   Project project, QualityExtension extension, ToolContext context) {
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

            // afterEvaluate used so here configuration should already be ok
            Integer rank = extension.spotbugsMaxRank.get()
            List<String> excludes = extension.exclude.get()
            FileCollection excludeSources = extension.excludeSources
            boolean filterModificationRequired = rank != null || excludes || excludeSources

            tasks.withType(spotbugsTaskType).configureEach { Task task ->
                task.dependsOn(context.configsTask)
                excludeFilter.set(project.provider {
                    // if rank or excludes configured, then exclude file must be updated dynamically
                    // (and so user config copied inside tmp)
                    return filterModificationRequired ?
                            context.tempRegularConfigFile(spotbugs_exclude)
                            : context.resolveRegularConfigFile(spotbugs_exclude)
                })

                reports {
                    xml {
                        required.set(true)
                    }
                    html {
                        required.set(extension.htmlReports.get())
                    }
                }
                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
                // read plugin error descriptions under configuration time
                context.storeReporterData(toolName) { SpotbugsReporter.resolvePluginsChecks(project) }
            }

            // dynamic spotbugs exclude file update (with rank and additional exclusions)
            // It's ok to update file in the scope of configs copying - output directory should be cached after
            context.configsTask.configure {
                if (!filterModificationRequired) {
                    // nothing to do
                    return
                }
                // request user file copying (for modification)
                modifiableFiles.add(spotbugs_exclude)

                // apt is a special dir, not mentioned in sources!
                Set<File> aptGenerated = []
                Set<File> sourceDirs = []
                SourceSetUtils.getSourceSets(project, extension.sourceSets.get()).each {
                    aptGenerated.add((project.tasks.findByName(it.compileJavaTaskName) as JavaCompile)
                            .options.generatedSourceOutputDirectory.get().asFile)
                    sourceDirs.addAll(it.allJava.srcDirs)
                }

                ObjectFactory objectFactory = project.objects
                // default or user file, copied into temp dir (for modification)
                File config = context.tempConfigFile(spotbugs_exclude)

                it.doLast {
                    // modify (already copied) excludes file to apply configured exclusions
                    SpotbugsUtils.replaceExcludeFilter(config,
                            aptGenerated, sourceDirs, rank, excludes, excludeSources, objectFactory)
                }
            }
        }

        context.applyEnabledState(spotbugsTaskType)
        context.groupQualityTasks(toolName)
    }
}
