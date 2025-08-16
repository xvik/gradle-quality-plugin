package ru.vyarus.gradle.plugin.quality.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import ru.vyarus.gradle.plugin.quality.tool.ProjectLang

/**
 * Task shows versions of currently used quality tools.
 *
 * @author Vyacheslav Rusakov
 * @since 07.08.2025
 */
@CompileStatic
abstract class QualityToolVersionsTask extends DefaultTask {

    @Input
    abstract Property<String> getCheckstyleVersion()
    @Input
    abstract Property<String> getPmdVersion()
    @Input
    abstract Property<String> getSpotBugsVersion()
    @Input
    abstract Property<String> getCodeNarcVersion()
    @Internal
    abstract ListProperty<ProjectLang> getLanguages()

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

        if (languages.get().contains(ProjectLang.Java)) {
            println 'Checkstyle: ' + checkstyleVersion.get()
            println 'PMD: ' + pmdVersion.get()
            println 'SpotBugs: ' + spotBugsVersion.get()
        }
        if (languages.get().contains(ProjectLang.Groovy)) {
            println 'CodeNarc: ' + codeNarcVersion.get()
        }
    }
}
