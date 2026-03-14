package ru.vyarus.gradle.plugin.quality.tools.checkstyle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.Requires

/**
 * @author Vyacheslav Rusakov
 * @since 11.03.2026
 */
class Checkstyle13SupportKitTest extends AbstractKitTest {

    // checkstyle 13 works on java 21
    @Requires({jvm.java21})
    def "Check checkstyle 13 support"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
                checkstyleVersion = '13.1.0'
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
        result.output.contains('Checkstyle rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
    }

    // checkstyle 13 disabled on java < 21
    @Requires({jvm.java17})
    def "Check checkstyle 13 auto disable"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
                checkstyleVersion = '13.1.0'
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
        !file('build/reports/checkstyle/main.html').exists()
    }

    // checkstyle 12 works on java 17
    @Requires({jvm.java17})
    def "Check checkstyle 12 support"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
                checkstyleVersion = '12.3.1'
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
        result.output.contains('Checkstyle rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
    }
}
