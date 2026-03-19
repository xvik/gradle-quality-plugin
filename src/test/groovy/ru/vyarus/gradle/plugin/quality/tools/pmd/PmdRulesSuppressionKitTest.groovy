package ru.vyarus.gradle.plugin.quality.tools.pmd

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 18.03.2026
 */
class PmdRulesSuppressionKitTest extends AbstractKitTest {

    @IgnoreIf({jvm.java8})
    def "Check pmd rules suppression"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle = false
                spotbugs = false            
                strict = false
                fallbackToCompatibleToolVersion = true
                suppressPmdRules = ['UnusedPrivateField', 'AvoidFieldNameMatchingTypeName', 'SingularField',]
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
        result.output.contains('PMD rule violations were found')

        then: "suppressions applied"
        result.output.contains('[quality] suppressed pmd rule: (category/java/bestpractices.xml) UnusedPrivateField')
        !result.output.contains('[Best Practices | UnusedPrivateField]')

        then: "all html reports generated"
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('PMD rule violations were found')
    }
}
