package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 15.03.2017
 */
@IgnoreIf({jvm.java8})
class ExcludeKitTest extends AbstractKitTest {

    def "Check main plugins exclusion"() {
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
                exclude '**/Sample2.java', 
                        '**/GSample2.groovy'
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

        when: "run check task with java and groovy sources"
        BuildResult result = run('check')

        then: "all plugins detect violations only in 1 file"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "2 Checkstyle rule violations were found in 1 files"
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
        output.contains "6 PMD rule violations were found in 1 files"
        output.contains "15 (0 / 6 / 9) CodeNarc violations were found in 1 files"
    }

    def "Check empty spotbugs exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                exclude '**/Sample2.groovy'
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

        when: "run spotbugs"
        BuildResult result = run('spotbugsMain')

        then: "no exclusions"
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "SpotBugs violations were found in 2 files"
    }

    def "Check spotbugs exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                exclude '**/Sample2.java'
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

        when: "run spotbugs"
        BuildResult result = run('spotbugsMain')

        then: "all plugins detect violations only in 1 file"
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
    }

    def "Check animalsniffer exclusion ignore"() {

        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.animalsniffer' version '2.0.1'
                id 'ru.vyarus.quality'
            }
            
            java {
                sourceCompatibility = 1.8
            }

            quality {
                strict = false
                spotbugs = false
                pmd = false
                checkstyle = false
                // animalsniffer operates on sources so use universal pattern which might affect
                exclude '**/sample/*'
            }

            repositories {
                mavenCentral();
            }            
            dependencies {
                signature 'org.codehaus.mojo.signature:java15:1.0@signature'
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "exclusions are not applied to animalsniffer"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains "4 AnimalSniffer violations were found in 2 files"
    }
}
