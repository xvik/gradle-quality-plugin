package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 06.11.2019
 */
@IgnoreIf({jvm.java8})
class SpotbugsRankChangeKitTest extends AbstractKitTest {

    def "Check spotbugs run with all ranks"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
            }

            repositories { mavenCentral() }
        """)

        fileFromClasspath('src/main/java/sample/Sample4.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample4.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('2 (0 / 2 / 0) SpotBugs violations were found in 1 files')
    }

    def "Check spotbugs run with lowered rank"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
                spotbugsMaxRank = 14
            }

            repositories { mavenCentral() }
        """)

        fileFromClasspath('src/main/java/sample/Sample4.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample4.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('1 (0 / 1 / 0) SpotBugs violations were found in 1 files')
    }

    def "Check rank validation"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
                spotbugsMaxRank = 30
            }

            repositories { mavenCentral() }
        """)

        fileFromClasspath('src/main/java/sample/Sample4.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample4.java')

        when: "run check task with java sources"
        BuildResult result = runFailed('check')

        then: "all plugins detect violations"
        result.output.contains('spotbugsMaxRank may be only between 1 and 20, but it is set to 30 ')
    }
}
