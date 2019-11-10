package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 28.08.2016
 */
class QualityTasksDisableKitTest extends AbstractKitTest {

    def "Check java and groovy checks disable"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                enabled = false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')

        when: "run check task with both sources"
        BuildResult result = run('check')

        then: "all plugins disabled"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        !result.output.contains('CodeNarc rule violations were found')
        !result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('SpotBugs rule violations were found')
        !result.output.contains('PMD rule violations were found')
    }

    def "Check direct task call"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                enabled = false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run checkstyle task directly"
        BuildResult result = runFailed('checkstyleMain')

        then: "direct call performed check"
        result.task(":checkstyleMain").outcome == TaskOutcome.FAILED
        result.output.contains('Checkstyle rule violations were found')
    }

    def "Check task call through grouping task"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                enabled = false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run grouping task"
        BuildResult result = runFailed('checkQualityMain')

        then: "direct quality task executed"
        result.task(":checkstyleMain").outcome == TaskOutcome.FAILED
        result.output.contains('Checkstyle rule violations were found')
    }
}