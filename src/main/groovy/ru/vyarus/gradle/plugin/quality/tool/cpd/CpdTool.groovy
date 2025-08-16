package ru.vyarus.gradle.plugin.quality.tool.cpd

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.QualityPlugin
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.tool.cpd.report.CpdTaskDescFactory
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectLang
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.cpd.report.CpdReporter

/**
 * CPD support. Requires cpd plugin manual activation.
 * <p>
 * In contrast to other tools, CPD plugin use single task for all source sets.
 *
 * @author Vyacheslav Rusakov
 * @since 16.08.2025
 * @see de.aaschmid.gradle.plugins.cpd.CpdPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings('GetterMethodCouldBeProperty')
class CpdTool implements QualityTool {
    static final String NAME = 'cpd'

    TaskDescFactory factory = new CpdTaskDescFactory()

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectLang> getSupportedLanguages() {
        return ProjectLang.values()
    }

    @Override
    Set<File> copyConfigs(Provider<ConfigsService> configs, QualityExtension extension) {
        ConfigsService service = configs.get()

        return  [
                service.resolveCpdXsl(true)
        ]
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.cpd.get()) {
            return
        }
        configureCpd(context.project, context.extension, context.configs, context)
    }

    @SuppressWarnings('MethodSize')
    void configureCpd(Project project, QualityExtension extension, Provider<ConfigsService> configs,
                      ToolContext context) {
        CpdUtils.findAndConfigurePlugin(project) { Project prj, Plugin plugin ->
            boolean sameModuleDeclaration = prj == project
            // STAGE1 for multi-module project this part applies by all modules with quality plugin enabled
            prj.configure(prj) {
                cpd {
                    // special case for single-module projects
                    if (sameModuleDeclaration && context.languages == [ProjectLang.Groovy]) {
                        language = 'groovy'
                    }
                    toolVersion = extension.pmdVersion.get()
                    // assuming that in case of multi-module project quality plugin is applied in subprojects section
                    // and so it is normal that subproject configures root project
                    // Otherwise, side effect is possible that first configured module with quality plugin determines
                    // root project flag value
                    ignoreFailures = !extension.strict.get()
                }
                // only default task is affected
                tasks.named('cpdCheck').configure {
                    boolean unifySources = extension.cpdUnifySources.get()
                    FileCollection excludeSources = extension.excludeSources
                    List<String> sources = extension.exclude.get()
                    doFirst {
                        if (unifySources) {
                            // todo move away
                            QualityPlugin.applyExcludes(it as SourceTask, excludeSources, sources)
                        }
                    }
                }
            }
            // cpdCheck is always declared by cpd plugin
            TaskProvider<SourceTask> cpdCheck = CpdUtils.findCpdTask(prj)
            if (extension.cpdUnifySources.get()) {
                // exclude sources, not declared for quality checks in quality plugin declaration project
                CpdUtils.unifyCpdSources(project, cpdCheck, extension.sourceSets.get())
            }

            // STAGE2 for multi-module project everything below must be applied just once
            if (CpdUtils.isCpdAlreadyConfigured(prj)) {
                return
            }

            Class<Task> cpdTasksType = plugin.class.classLoader.loadClass('de.aaschmid.gradle.plugins.cpd.Cpd')
            // reports applied for all registered cpd tasks
            prj.tasks.withType(cpdTasksType).configureEach { task ->
                reports.xml.required.set(true)
                doFirst {
                    // todo remove
                    configs.get().resolveCpdXsl()
                }
                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
            }
            // cpd plugin recommendation: module check must also run cpd (check module changes for duplicates)
            // grouping tasks (checkQualityMain) are not affected because cpd applied to all source sets
            // For single module projects simply make sure check will trigger cpd
            project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME)
                    .configure { it.dependsOn prj.tasks.withType(cpdTasksType) }

            // cpd disabled together with all quality plugins
            // yes, it's not completely normal that module could disable root project task, but it would be much
            // simpler to use like that (because quality plugin assumed to be applied in subprojects section)
            context.applyEnabledState(cpdTasksType)
        }
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new CpdReporter(configs)
    }
}
