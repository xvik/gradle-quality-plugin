package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires

/**
 * @author Vyacheslav Rusakov
 * @since 27.07.2025
 */
@Requires({jvm.java8})
class Java8QualityPluginKitTest extends AbstractKitTest {

    def "Check java checks without spotbugs"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
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
        !result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        then: "all html reports generated"
        !file('build/reports/checkstyle/main.html').exists()
        !file('build/reports/spotbugs/main.html').exists()
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }


    def "Check java checks with spotbugs 5.x"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '5.2.5'
            }

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
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
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        then: "all html reports generated"
        !file('build/reports/checkstyle/main.html').exists()
        file('build/reports/spotbugs/main.html').exists()
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }
}
