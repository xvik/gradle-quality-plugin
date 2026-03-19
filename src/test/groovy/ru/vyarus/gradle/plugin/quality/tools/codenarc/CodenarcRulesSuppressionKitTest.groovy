package ru.vyarus.gradle.plugin.quality.tools.codenarc

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 19.03.2026
 */
class CodenarcRulesSuppressionKitTest extends AbstractKitTest {

    @IgnoreIf({jvm.java8})
    def "Check codenarc rules suppression"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                suppressCodenarcRules = ['DuplicateStringLiteral', 'UnnecessaryGString']
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with groovy sources"
        BuildResult result = run('check')

        then: "plugin detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')

        then: "suppressions applied"
        result.output.contains('[quality] suppressed codenarc rules: DuplicateStringLiteral, UnnecessaryGString')
        !result.output.contains('[Dry | DuplicateStringLiteral]')

        then: "html report generated"
        file('build/reports/codenarc/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
    }
}
