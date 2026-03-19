package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 19.03.2026
 */
class SpotbugsRulesSuppressionKitTest extends AbstractKitTest {

    @IgnoreIf({jvm.java8})
    def "Check spotbugs rules suppression"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                checkstyle = false
                pmd = false
                strict = false
                fallbackToCompatibleToolVersion = true
                suppressSpotbugsRules = ['URF_UNREAD_FIELD']
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
        !result.output.contains('SpotBugs violations were found')

        then: "suppressions applied"
        result.output.contains('[quality] suppressed spotbugs rules: URF_UNREAD_FIELD')
        !result.output.contains('[Performance | URF_UNREAD_FIELD]')

        then: "all html reports generated"
        file('build/reports/spotbugs/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('SpotBugs violations were found')
    }
}
