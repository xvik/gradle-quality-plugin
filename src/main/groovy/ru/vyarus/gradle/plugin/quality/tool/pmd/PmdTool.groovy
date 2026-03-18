package ru.vyarus.gradle.plugin.quality.tool.pmd

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import org.gradle.util.GradleVersion
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext
import ru.vyarus.gradle.plugin.quality.tool.pmd.report.PmdReporter
import ru.vyarus.gradle.plugin.quality.util.SourceSetUtils

/**
 * Pmd support.
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 * @see PmdPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings(['GetterMethodCouldBeProperty', 'PropertyName'])
class PmdTool implements QualityTool {
    static final String NAME = 'pmd'

    static final String pmd_config = 'pmd/pmd.xml'

    private final TaskDescFactory factory = new TaskDescFactory()

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
        return [pmd_config]
    }

    @Override
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs) {
        if (langs.contains(ProjectSources.Java)) {
            return 'PMD: ' + (extension.pmd.get() ? extension.pmdVersion.get() : 'disabled')
        }
        return null
    }

    @Override
    Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        return new PmdReporter()
    }

    @Override
    void configure(ToolContext context, boolean register) {
        if (!context.extension.pmd.get()) {
            // do not configure even if manually registered
            return
        }
        context.withPlugin(PmdPlugin, register) {
            configurePmd(context.project, context.extension, context)
        }
    }

    @SuppressWarnings('MethodSize')
    private void configurePmd(Project project, QualityExtension extension, ToolContext context) {
        project.configure(project) {
            // if rules exclusion configured, then main config file must be updated dynamically
            // (and so user config copied inside tmp)
            boolean modificationRequired = !extension.suppressPmdRules.get().empty

            pmd {
                toolVersion = extension.pmdVersion.get()
                ignoreFailures = !extension.strict.get()
                ruleSets = []
                // this may be custom user file (gradle/config/pmd/pmd.xml) or default one
                // (in different location)
                ruleSetFiles = files((modificationRequired ?
                        context.tempConfigFile(pmd_config)
                        : context.resolveConfigFile(pmd_config)).absolutePath)
                sourceSets = SourceSetUtils.getSourceSets(project, extension.sourceSets.get())
            }
            if (GradleVersion.current() < GradleVersion.version('8.3')) {
                // have to override dependencies declaration due to split in pmd 7
                // https://github.com/gradle/gradle/issues/24502
                dependencies {
                    pmd("net.sourceforge.pmd:pmd-ant:${extension.pmdVersion.get()}")
                    pmd("net.sourceforge.pmd:pmd-java:${extension.pmdVersion.get()}")
                }
            }
            tasks.withType(Pmd).configureEach { task ->
                task.dependsOn(context.configsTask)

                reports.xml.required.set(true)
                reports.html.required.set(extension.htmlReports.get())

                context.registerTaskForReport(task, factory.buildDesc(task, toolName))
                context.applyExcludes(task as SourceTask, extension.excludeSources, extension.exclude.get())
            }
            // dynamic pmd config file update (remove some modules)
            // It's ok to update file in the scope of configs copying - output directory should be cached after
            context.configsTask.configure {
                if (!modificationRequired) {
                    return
                }
                // request user file copying (for modification)
                modifiableFiles.add(pmd_config)

                // default or user file, copied into temp dir (for modification)
                File config = context.tempConfigFile(pmd_config)

                // in order to apply additional exclusions, we need to know the exact rule source path
                Map<String, String> rules = PmdUtils.readExistingRules(project)

                it.doLast {
                    // modify (already copied) config file to apply configured exclusions
                    PmdUtils.mergeExcludes(config, rules, extension.suppressPmdRules.get())
                }
            }
        }
        context.applyEnabledState(Pmd)
        context.groupQualityTasks(toolName)
    }
}
