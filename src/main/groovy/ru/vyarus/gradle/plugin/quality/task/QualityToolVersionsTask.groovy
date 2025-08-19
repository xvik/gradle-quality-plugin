package ru.vyarus.gradle.plugin.quality.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

/**
 * Task shows versions of currently used quality tools.
 *
 * @author Vyacheslav Rusakov
 * @since 07.08.2025
 */
@CompileStatic
abstract class QualityToolVersionsTask extends DefaultTask {

    @Internal
    abstract ListProperty<String> getToolsInfo()

    @SuppressWarnings('AbstractClassWithPublicConstructor')
    QualityToolVersionsTask() {
        group = 'verification'
        description = 'Show configured quality tools versions'
    }

    @TaskAction
    @SuppressWarnings('Println')
    void run() {
        println 'Java version: ' + JavaVersion.current()
        println 'Gradle version: ' + GradleVersion.current().version

        // each tool renders its info line (if it's relevant to current project)
        toolsInfo.get().each { println it }
    }
}
