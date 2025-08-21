package ru.vyarus.gradle.plugin.quality.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.Requires

/**
 * @author Vyacheslav Rusakov
 * @since 20.08.2025
 */
@Requires({jvm.java17Compatible})
class CopyConfigsTaskCashingKitTest extends AbstractKitTest {

    def "Check configs task invalidation"() {
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
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run configs copying"
        BuildResult result = run('copyQualityConfigs')

        then: "copied"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.SUCCESS

        when: "run again"
        result = run('copyQualityConfigs')

        then: "copied"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.UP_TO_DATE

        when: "config modified"
        buildFile.delete()
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
                // it must trigger copy task 
                exclude '**/Sample2.java', 
                        '**/GSample2.groovy'
                fallbackToCompatibleToolVersion = true
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)
        result = run('copyQualityConfigs')

        then: "copied"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.SUCCESS

    }

    def "Check configs task invalidation after exclude files changes"() {
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
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run configs copying"
        BuildResult result = run('copyQualityConfigs')

        then: "copied"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.SUCCESS

        when: "run again"
        result = run('copyQualityConfigs')

        then: "copied"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.UP_TO_DATE

        when: "config modified"
        buildFile.delete()
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
                // it must trigger copy task 
                excludeSources = fileTree('build/generated/')
                fallbackToCompatibleToolVersion = true
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)
        result = run('copyQualityConfigs')

        then: "copied"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.SUCCESS

    }

}
