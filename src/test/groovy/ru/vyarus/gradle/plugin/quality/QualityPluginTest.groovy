package ru.vyarus.gradle.plugin.quality

import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.plugins.quality.CodeNarcPlugin
import org.gradle.api.plugins.quality.FindBugsPlugin
import org.gradle.api.plugins.quality.PmdPlugin
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * @author Vyacheslav Rusakov 
 * @since 12.11.2015
 */
class QualityPluginTest extends AbstractTest {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    def "Check plugins registration"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'
        }

        then: "plugins registered"
        project.plugins.findPlugin(CheckstylePlugin)
        project.plugins.findPlugin(PmdPlugin)
        project.plugins.findPlugin(FindBugsPlugin)
        !project.plugins.findPlugin(CodeNarcPlugin)

        then: "task installed"
        project.tasks.initQualityConfig
    }

    def "Check plugins registration fo groovy"() {

        when: "apply plugin"
        Project project = project {
            apply plugin: 'groovy'
            apply plugin: 'ru.vyarus.quality'
        }

        then: "plugins registered"
        !project.plugins.findPlugin(CheckstylePlugin)
        !project.plugins.findPlugin(PmdPlugin)
        !project.plugins.findPlugin(FindBugsPlugin)
        project.plugins.findPlugin(CodeNarcPlugin)
    }

    def "Check plugins disable options"() {

        when: "disable all plugins in config"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'

            quality {
                checkstyle false
                pmd false
                findbugs false
                codenarc false
            }
        }

        then: "plugins not registered"
        !project.plugins.findPlugin(CheckstylePlugin)
        !project.plugins.findPlugin(PmdPlugin)
        !project.plugins.findPlugin(FindBugsPlugin)
        !project.plugins.findPlugin(CodeNarcPlugin)
    }
}