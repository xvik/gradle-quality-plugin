package ru.vyarus.gradle.plugin.quality.tool.checkstyle

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.QualityPlugin
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectLang
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.checkstyle.report.CheckstyleReporter

/**
 * Checkstyle support.
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 * @see CheckstylePlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings('GetterMethodCouldBeProperty')
class CheckstyleTool implements QualityTool {

    static final String NAME = 'checkstyle'

    private final TaskDescFactory factory = new TaskDescFactory()

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

        return  [
                service.resolveCheckstyleConfig(true),
                service.resolveCheckstyleConfigDir(),
        ]
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.checkstyle.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(CheckstylePlugin, register) {
            configureCheckstyle(context.project, context.extension, context.configs, context)
        }
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new CheckstyleReporter()
    }

    private void configureCheckstyle(Project project, QualityExtension extension, Provider<ConfigsService> configs,
                                     ToolContext context) {
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
                FileCollection excludeSources = extension.excludeSources
                List<String> sources = extension.exclude.get()
                doFirst {
                    // todo remove
                    configs.get().resolveCheckstyleConfig()
                    // todo move out
                    QualityPlugin.applyExcludes(it as SourceTask, excludeSources, sources)
                }
                reports.xml.required.set(true)
                reports.html.required.set(extension.htmlReports.get())

                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
            }
        }
        context.applyEnabledState(Checkstyle)
        context.groupQualityTasks(toolName)
    }
}
