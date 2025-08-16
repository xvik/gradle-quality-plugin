package ru.vyarus.gradle.plugin.quality.tool.pmd

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdPlugin
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
import ru.vyarus.gradle.plugin.quality.tool.pmd.report.PmdReporter

/**
 * Pmd support.
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 * @see PmdPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings('GetterMethodCouldBeProperty')
class PmdTool implements QualityTool {
    static final String NAME = 'pmd'

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

        return [
                service.resolvePmdConfig(true)
        ]
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.pmd.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(PmdPlugin, register) {
            configurePmd(context.project, context.extension, context.configs, context)
        }
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new PmdReporter()
    }

    private void configurePmd(Project project, QualityExtension extension, Provider<ConfigsService> configs,
                              ToolContext context) {
        project.configure(project) {
            pmd {
                toolVersion = extension.pmdVersion.get()
                ignoreFailures = !extension.strict.get()
                ruleSets = []
                ruleSetFiles = files(configs.get().resolvePmdConfig(false).absolutePath)
                sourceSets = extension.sourceSets.get()
            }
            // have to override dependencies declaration due to split in pmd 7
            // https://github.com/gradle/gradle/issues/24502
            dependencies {
                pmd("net.sourceforge.pmd:pmd-ant:${extension.pmdVersion.get()}")
                pmd("net.sourceforge.pmd:pmd-java:${extension.pmdVersion.get()}")
            }
            tasks.withType(Pmd).configureEach { task ->
                FileCollection excludeSources = extension.excludeSources
                List<String> sources = extension.exclude.get()
                doFirst {
                    // todo remove
                    configs.get().resolvePmdConfig()
                    // todo move out
                    QualityPlugin.applyExcludes(it as SourceTask, excludeSources, sources)
                }
                reports.xml.required.set(true)
                reports.html.required.set(extension.htmlReports.get())

                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
            }
        }
        context.applyEnabledState(Pmd)
        context.groupQualityTasks(toolName)
    }
}
