package ru.vyarus.gradle.plugin.quality.tool.animalsniffer

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext

/**
 * Animalsniffer support.
 *
 * @author Vyacheslav Rusakov
 * @since 16.08.2025
 * @see ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferPlugin
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings('GetterMethodCouldBeProperty')
class AnimalsnifferTool implements QualityTool {

    static final String NAME = 'animalsniffer'
    static final String ANIMALSNIFFER_PLUGIN = 'ru.vyarus.animalsniffer'

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectSources> getAutoEnableForSources() {
        // for all languages (used manually apply plugin when required)
        return ProjectSources.values()
    }

    @Override
    List<String> getConfigs() {
        return []
    }

    @Override
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs) {
        if (project.plugins.hasPlugin(ANIMALSNIFFER_PLUGIN)) {
            if (extension.animalsniffer.get()) {
                boolean versionSet = extension.animalsnifferVersion.present
                return versionSet ? "AnimalSniffer: ${extension.animalsnifferVersion.get()}" : null
            }
            return 'AnimalSniffer: disabled'
        }
        return null
    }

    @Override
    void configure(ToolContext context, boolean register) {
        Project project = context.project
        QualityExtension extension = context.extension

        if (!extension.animalsniffer.get()) {
            return
        }

        project.plugins.withId(ANIMALSNIFFER_PLUGIN) { plugin ->
            project.configure(project) {
                animalsniffer {
                    ignoreFailures = !extension.strict.get()
                    sourceSets = extension.sourceSets.get()
                }
                if (extension.animalsnifferVersion.present) {
                    animalsniffer.toolVersion = extension.animalsnifferVersion.get()
                }
            }
            context.applyEnabledState(plugin.class.classLoader
                    .loadClass('ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer'))
            context.groupQualityTasks(toolName)
        }
    }
}
