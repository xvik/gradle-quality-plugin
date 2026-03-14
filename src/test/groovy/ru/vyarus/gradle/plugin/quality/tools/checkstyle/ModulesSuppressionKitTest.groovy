package ru.vyarus.gradle.plugin.quality.tools.checkstyle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 14.03.2026
 */
class ModulesSuppressionKitTest extends AbstractKitTest {

    @IgnoreIf({jvm.java8})
    def "Check rules suppression"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                suppressCheckstyleRules = ['NewlineAtEndOfFile', 'MissingJavadocType']
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
    }

}
