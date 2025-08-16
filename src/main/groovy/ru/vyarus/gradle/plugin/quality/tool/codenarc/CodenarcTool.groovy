package ru.vyarus.gradle.plugin.quality.tool.codenarc

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.plugins.quality.CodeNarcPlugin
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
import ru.vyarus.gradle.plugin.quality.tool.codenarc.report.CodeNarcReporter

/**
 * Codenarc support.
 *
 * @author Vyacheslav Rusakov
 * @since 16.08.2025
 * @see CodeNarcPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings('GetterMethodCouldBeProperty')
class CodenarcTool implements QualityTool {
    static final String NAME = 'codenarc'

    private static final String CODENARC_GROOVY4 = '-groovy-4.0'

    TaskDescFactory factory = new TaskDescFactory()

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectLang> getSupportedLanguages() {
        return [ProjectLang.Groovy]
    }

    @Override
    Set<File> copyConfigs(Provider<ConfigsService> configs, QualityExtension extension) {
        ConfigsService service = configs.get()

        return  [
                service.resolveCodenarcConfig(true)
        ]
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.codenarc.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(CodeNarcPlugin, register) {
            configureCodenarc(context.project, context.extension, context.configs, context)
        }
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new CodeNarcReporter(param as Properties)
    }

    private void configureCodenarc(Project project, QualityExtension extension, Provider<ConfigsService> configs,
                                   ToolContext context) {
        project.configure(project) {
            codenarc {
                toolVersion = extension.codenarcVersion.get()
                ignoreFailures = !extension.strict.get()
                configFile = configs.get().resolveCodenarcConfig(false)
                sourceSets = extension.sourceSets.get()
            }
            if (extension.codenarcGroovy4.get() && !extension.codenarcVersion.get().endsWith(CODENARC_GROOVY4)) {
                // since codenarc 3.1 different groovy4-based jar could be used
                dependencies {
                    codenarc "org.codenarc:CodeNarc:${extension.codenarcVersion.get()}$CODENARC_GROOVY4"
                }
            }
            tasks.withType(CodeNarc).configureEach { task ->
                FileCollection excludeSources = extension.excludeSources
                List<String> sources = extension.exclude.get()
                doFirst {
                    // toda remove
                    configs.get().resolveCodenarcConfig()
                    // todo move away
                    QualityPlugin.applyExcludes(it as SourceTask, excludeSources, sources)
                }
                  // read codenarc properties under configuration phase
                context.storeReporterData(toolName) { CodeNarcReporter.loadCodenarcProperties(project) }
                reports.xml.required.set(true)
                reports.html.required.set(extension.htmlReports.get())
                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
            }
        }
        context.applyEnabledState(CodeNarc)
        context.groupQualityTasks(toolName)
    }
}
