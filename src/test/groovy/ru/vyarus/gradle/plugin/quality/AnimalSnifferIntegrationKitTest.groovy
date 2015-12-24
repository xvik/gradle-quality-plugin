package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

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
                id 'ru.vyarus.animalsniffer'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
    }
}