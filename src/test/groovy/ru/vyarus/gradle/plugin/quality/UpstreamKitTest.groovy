package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * Minimal compatibility check with the latest gradle version.
 *
 * @author Vyacheslav Rusakov
 * @since 10.11.2019
 */
class UpstreamKitTest extends AbstractKitTest {


    public static final String GRADLE = '6.3'

    def "Check java checks"() {
        setup:
        build("""
            plugins {
                id 'java-library'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task"
        BuildResult result = runVer(GRADLE,'check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs rule violations were found')
        result.output.contains('PMD rule violations were found')
    }

    def "Check java and groovy checks"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with both sources"
        BuildResult result = runVer(GRADLE,'check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs rule violations were found')
        result.output.contains('PMD rule violations were found')
    }
}
