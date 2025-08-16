package ru.vyarus.gradle.plugin.quality.tool.spotbugs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.report.SpotbugsReporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.report.SpotbugsTaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectLang
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.util.FileUtils

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
@SuppressWarnings('GetterMethodCouldBeProperty')
class SpotbugsTool implements QualityTool {
    static final String NAME = 'spotbugs'

    TaskDescFactory factory = new SpotbugsTaskDescFactory()

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectLang> getSupportedLanguages() {
        return [ProjectLang.Java]
    }

    @Override
    Set<File> copyConfigs(Provider<ConfigsService> configs, QualityExtension extension) {
        ConfigsService service = configs.get()

        return [
                service.resolveSpotbugsExclude(true)
        ]
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
            configureSpotbugs(plugin, spotbugsTaskType, context.project, context.extension, context.configs, context)
        }
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        new SpotbugsReporter(param as Map<String, String>)
    }

    @SuppressWarnings(['AbcMetric', 'MethodSize', 'ParameterCount'])
    private void configureSpotbugs(Class<? extends Plugin> plugin, Class<? extends Task> spotbugsTaskType,
                                   Project project, QualityExtension extension, Provider<ConfigsService> configs,
                                   ToolContext context) {
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
                SourceSet set = FileUtils.findMatchingSet(toolName, task.name, project.sourceSets)
                // apt is a special dir, not mentioned in sources!
                File aptGenerated = (task.project.tasks.findByName(set.compileJavaTaskName) as JavaCompile)
                        .options.generatedSourceOutputDirectory.get().asFile
                Set<File> setSourceDirs = set.allJava.srcDirs
                Integer rank = extension.spotbugsMaxRank.get()
                List<String> excludes = extension.exclude.get()
                FileCollection excludeSources = extension.excludeSources
                ObjectFactory objectFactory = project.objects
                doFirst {
                    // todo remove
                    configs.get().resolveSpotbugsExclude()
                    // todo move away
                    // it is not possible to substitute filter file here (due to locked Property)
                    // but possible to update already configured file (it must be already a temp file here)
                    SpotbugsUtils.replaceExcludeFilter(it, aptGenerated, setSourceDirs,
                            rank, excludes, excludeSources, objectFactory)
                }
                // have to use this way instead of doFirst hook, because nothing else will work (damn props!)
                excludeFilter.set(project.provider(new SpotbugsExclusionConfigProvider(
                        task, configs, extension
                )))
                // read plugin error descriptions under configuration time
                context.storeReporterData(toolName) { SpotbugsReporter.resolvePluginsChecks(project) }
                reports {
                    xml {
                        required.set(true)
                    }
                    html {
                        required.set(extension.htmlReports.get())
                    }
                }
                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
            }
        }

        context.applyEnabledState(spotbugsTaskType)
        context.groupQualityTasks(toolName)
    }
}
