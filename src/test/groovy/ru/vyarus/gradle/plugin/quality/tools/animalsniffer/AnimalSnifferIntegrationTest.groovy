package ru.vyarus.gradle.plugin.quality.tools.animalsniffer

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension
import ru.vyarus.gradle.plugin.quality.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 21.12.2015
 */
class AnimalSnifferIntegrationTest extends AbstractTest {

    def "Check animalsniffer integration"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.animalsniffer'
            apply plugin: 'ru.vyarus.quality'

            quality {
                animalsnifferVersion = '1.13'
            }
        }

        then: "animalsniffer configured"
        AnimalSnifferExtension extension = project.extensions.animalsniffer
        extension.sourceSets == [project.sourceSets.main]
        !extension.ignoreFailures
        extension.toolVersion == '1.13'
    }
}