package ru.vyarus.gradle.plugin.quality.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.quality.ConfigLoader

/**
 * Task copies default configs to user directory (quality.configDir) for customization.
 * By default, does not override existing files.
 * Registered as 'initQualityConfig'.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @see ru.vyarus.gradle.plugin.quality.QualityPlugin for registration
 */
@CompileStatic
class InitQualityConfigTask extends DefaultTask {

    @Input
    boolean override

    InitQualityConfigTask() {
        group  = 'build setup'
        description = 'Copies default quality plugin configuration files for customization'
    }

    @TaskAction
    void run() {
        new ConfigLoader(project).initUserConfigs(override)
    }
}
