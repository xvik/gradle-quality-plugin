package ru.vyarus.gradle.plugin.quality.tools.pmd


import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 06.11.2019
 */
class PmdIncrementalTest extends AbstractTest {

    def "Check enabled by default"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'

            quality.pmdIncremental = false
        }

        then: "value not applied but true by default"
        project.pmd.incrementalAnalysis.get() == true

    }

    def "Check incremental value applied"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'

            quality.pmdIncremental = true
        }

        then: "value applied to pmd"
        project.pmd.incrementalAnalysis.get() == true

    }
}
