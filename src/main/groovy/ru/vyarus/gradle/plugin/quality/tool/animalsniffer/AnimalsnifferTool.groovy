package ru.vyarus.gradle.plugin.quality.tool.animalsniffer

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.ProjectLang
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

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectLang> getSupportedLanguages() {
        // for all languages (used manually apply plugin when required)
        return ProjectLang.values()
    }

    @Override
    Set<File> copyConfigs(Provider<ConfigsService> configs, QualityExtension extension) {
        return []
    }

    @Override
    void configure(ToolContext context, boolean register) {
        Project project = context.project
        QualityExtension extension = context.extension

        project.plugins.withId('ru.vyarus.animalsniffer') { plugin ->
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
