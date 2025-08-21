package ru.vyarus.gradle.plugin.quality.tool.checkstyle

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
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
@SuppressWarnings(['GetterMethodCouldBeProperty', 'PropertyName'])
class CheckstyleTool implements QualityTool {

    static final String NAME = 'checkstyle'

    static final String checkstyle_config = 'checkstyle/checkstyle.xml'
    static final String checkstyle_suppressions = 'checkstyle/suppressions.xml'

    private final TaskDescFactory factory = new TaskDescFactory()

    static File resolveConfigsDir(ToolContext context) {
        // used for ${config_loc} property definition (through checkstyle.configDirectory property)
        File path = context.userConfigFile(checkstyle_config).parentFile
        // if custom directory exists, use it, otherwise fall back to generated directory
        // because gradle 7 requires configured directory existence
        // (default file copying forced to create checkstyle directory and avoid gradle complains)
        return path.exists() ? path : context.resolveConfigFile(checkstyle_config).parentFile
    }

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
        return [checkstyle_config, checkstyle_suppressions]
    }

    @Override
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs) {
        if (langs.contains(ProjectSources.Java)) {
            return 'Checkstyle: ' + (extension.checkstyle.get() ? extension.checkstyleVersion.get() : 'disabled')
        }
        return null
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new CheckstyleReporter()
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.checkstyle.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(CheckstylePlugin, register) {
            configureCheckstyle(context.project, context.extension, context)
        }
    }

    private void configureCheckstyle(Project project, QualityExtension extension, ToolContext context) {
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
                configFile = context.resolveConfigFile(checkstyle_config)
                // this is required for ${config_loc} variable, but this will ALWAYS point to
                // gradle/config/checkstyle/ (or other configured config location dir) because custom
                // configuration files may be only there
                configDirectory = resolveConfigsDir(context)
                sourceSets = extension.sourceSets.get()
            }

            tasks.withType(Checkstyle).configureEach { Task task ->
                task.dependsOn(context.configsTask)

                reports.xml.required.set(true)
                reports.html.required.set(extension.htmlReports.get())

                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
                context.applyExcludes(task as SourceTask, extension.excludeSources, extension.exclude.get())
            }
        }
        context.applyEnabledState(Checkstyle)
        context.groupQualityTasks(toolName)
    }
}
