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
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.cpd.report.CpdReporter
import ru.vyarus.gradle.plugin.quality.tool.cpd.report.CpdTaskDescFactory

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
@SuppressWarnings(['GetterMethodCouldBeProperty', 'PropertyName'])
class CpdTool implements QualityTool {
    static final String NAME = 'cpd'

    static final String cpd_xsl = 'cpd/cpdhtml.xslt'

    TaskDescFactory factory = new CpdTaskDescFactory()

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectSources> getAutoEnableForSources() {
        return ProjectSources.values()
    }

    @Override
    List<String> getConfigs() {
        return [cpd_xsl]
    }

    @Override
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs) {
        if (project.plugins.hasPlugin(CpdUtils.CPD_PLUGIN)) {
            return 'CPD: ' + extension.cpd.get() ? 'enabled' : 'disabled'
        }
        return null
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new CpdReporter(configs)
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.cpd.get()) {
            return
        }
        configureCpd(context.project, context.extension, context)
    }

    @SuppressWarnings('MethodSize')
    void configureCpd(Project project, QualityExtension extension, ToolContext context) {
        CpdUtils.findAndConfigurePlugin(project) { Project prj, Plugin plugin ->
            boolean sameModuleDeclaration = prj == project
            // STAGE1 for multi-module project this part applies by all modules with quality plugin enabled
            prj.configure(prj) {
                cpd {
                    // special case for single-module projects
                    if (sameModuleDeclaration && context.languages == [ProjectSources.Groovy]) {
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
                            ToolContext.applyExcludes(it as SourceTask, excludeSources, sources)
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

            Class<Task> cpdTaskType = plugin.class.classLoader.loadClass('de.aaschmid.gradle.plugins.cpd.Cpd')
            // reports applied for all registered cpd tasks
            prj.tasks.withType(cpdTaskType).configureEach { task ->
                task.dependsOn(context.configsTask)
                reports.xml.required.set(true)
                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
            }
            // cpd plugin recommendation: module check must also run cpd (check module changes for duplicates)
            // grouping tasks (checkQualityMain) are not affected because cpd applied to all source sets
            // For single module projects simply make sure check will trigger cpd
            project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME)
                    .configure { it.dependsOn prj.tasks.withType(cpdTaskType) }

            // cpd disabled together with all quality plugins
            // yes, it's not completely normal that module could disable root project task, but it would be much
            // simpler to use like that (because quality plugin assumed to be applied in subprojects section)
            context.applyEnabledState(cpdTaskType)
        }
    }
}
