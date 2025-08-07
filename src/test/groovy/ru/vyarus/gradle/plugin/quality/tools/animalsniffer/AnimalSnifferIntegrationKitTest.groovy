package ru.vyarus.gradle.plugin.quality.tools.animalsniffer

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 24.12.2015
 */
class AnimalSnifferIntegrationKitTest extends AbstractKitTest {

    def "Check animalsniffer integration"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer' version '2.0.1'
                id 'ru.vyarus.quality'
            }
            
            java {
                sourceCompatibility = 1.8
            }

            quality {
                strict = false
                spotbugs = false
                pmd = false
                checkstyle = false
            }

            repositories {            
                mavenCentral();
            }
            dependencies {
                signature 'org.codehaus.mojo.signature:java15:1.0@signature'
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('4 AnimalSniffer violations were found in 2 files')
    }

    def "Check animalsniffer disable"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer' version '2.0.1'
                id 'ru.vyarus.quality'
            }

            quality {
                enabled = false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":animalsnifferMain").outcome == TaskOutcome.SKIPPED
    }
}