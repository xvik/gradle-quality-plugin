package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 01.09.2016
 */
@IgnoreIf({jvm.java8})
class ConsoleReportsDisableKitTest extends AbstractKitTest {

    def "Check java and groovy checks disable"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                consoleReporting = false
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')

        when: "run check task with both sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs ended with exit code 1')
        result.output.contains('PMD rule violations were found')

        then: "no console reporting performed"
        !result.output.contains('[Formatting | ClassJavadoc] sample.(GSample.groovy:3)  [priority 2]') // codenarc
        !result.output.contains('[Javadoc | JavadocType] sample.(Sample.java:3)') // checkstyle
        !result.output.contains('[Performance | URF_UNREAD_FIELD] sample.(Sample.java:8)  [priority 2]') // spotbugs
        !result.output.contains('[Unused Code | UnusedPrivateField] sample.(Sample.java:5)') // pmd

        then: "spotbugs html report generated"
        file('build/reports/spotbugs/main.html').exists()
    }

}