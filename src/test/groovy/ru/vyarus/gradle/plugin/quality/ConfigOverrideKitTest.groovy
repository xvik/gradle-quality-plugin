package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
class ConfigOverrideKitTest extends AbstractKitTest {

    def "Check config override"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
                configDir 'config'
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('config/checkstyle/checkstyle.xml', '/ru/vyarus/quality/config/checkstyle/checkstyle.xml')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "custom config used"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !file('build/quality-configs/checkstyle/checkstyle.xml').exists()

    }
}