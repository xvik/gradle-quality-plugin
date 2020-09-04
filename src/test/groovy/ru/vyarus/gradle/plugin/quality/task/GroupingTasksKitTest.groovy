package ru.vyarus.gradle.plugin.quality.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest


/**
 * @author Vyacheslav Rusakov
 * @since 01.09.2016
 */
class GroupingTasksKitTest extends AbstractKitTest {

    def "Check java and groovy checks disable"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
                sourceSets = [project.sourceSets.main, project.sourceSets.test]
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')

        when: "run main quality check"
        BuildResult result = run('checkQualityMain')

        then: "all plugins detect violations"
        result.task(":checkQualityMain").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        when: "run test quality check"
        result = run('checkQualityTest')

        then: "all plugins detect violations"
        result.task(":checkQualityTest").outcome == TaskOutcome.UP_TO_DATE
    }


}