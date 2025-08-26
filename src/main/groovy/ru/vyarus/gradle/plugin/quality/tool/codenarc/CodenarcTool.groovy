package ru.vyarus.gradle.plugin.quality.tool.codenarc

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.plugins.quality.CodeNarcPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.codenarc.report.CodeNarcReporter
import ru.vyarus.gradle.plugin.quality.util.SourceSetUtils

/**
 * Codenarc support.
 *
 * @author Vyacheslav Rusakov
 * @since 16.08.2025
 * @see CodeNarcPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings(['GetterMethodCouldBeProperty', 'PropertyName'])
class CodenarcTool implements QualityTool {
    static final String NAME = 'codenarc'

    static final String codenarc_config = 'codenarc/codenarc.xml'

    private static final String CODENARC_GROOVY4 = '-groovy-4.0'

    TaskDescFactory factory = new TaskDescFactory()

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectSources> getAutoEnableForSources() {
        return [ProjectSources.Groovy]
    }

    @Override
    List<String> getConfigs() {
        return [codenarc_config]
    }

    @Override
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs) {
        if (langs.contains(ProjectSources.Groovy)) {
            return 'CodeNarc: ' + (extension.codenarc.get() ? extension.codenarcVersion.get() : 'disabled')
        }
        return null
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new CodeNarcReporter(param as Properties)
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.codenarc.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(CodeNarcPlugin, register) {
            configureCodenarc(context.project, context.extension, context)
        }
    }

    private void configureCodenarc(Project project, QualityExtension extension, ToolContext context) {
        project.configure(project) {
            codenarc {
                toolVersion = extension.codenarcVersion.get()
                ignoreFailures = !extension.strict.get()
                configFile = context.resolveConfigFile(codenarc_config)
                sourceSets = SourceSetUtils.getSourceSets(project, extension.sourceSets.get())
            }
            if (extension.codenarcGroovy4.get() && !extension.codenarcVersion.get().endsWith(CODENARC_GROOVY4)) {
                // since codenarc 3.1 different groovy4-based jar could be used
                dependencies {
                    codenarc "org.codenarc:CodeNarc:${extension.codenarcVersion.get()}$CODENARC_GROOVY4"
                }
            }
            tasks.withType(CodeNarc).configureEach { Task task ->
                task.dependsOn(context.configsTask)

                reports.xml.required.set(true)
                reports.html.required.set(extension.htmlReports.get())

                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
                context.applyExcludes(task as SourceTask, extension.excludeSources, extension.exclude.get())
                // read codenarc properties under configuration phase
                context.storeReporterData(toolName) { CodeNarcReporter.loadCodenarcProperties(project) }
            }
        }
        context.applyEnabledState(CodeNarc)
        context.groupQualityTasks(toolName)
    }
}
