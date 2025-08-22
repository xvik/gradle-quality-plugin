package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.Requires

/**
 * @author Vyacheslav Rusakov
 * @since 20.08.2025
 */
@Requires({jvm.java11Compatible})
class SpotbugsExcludeFilesChangeKitTest extends AbstractKitTest {

    def "Check main plugins sources exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }

            quality {
                strict = false
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

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('build/generated/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java and groovy sources"
        BuildResult result = run('spotbugsMain')

        then: "all plugins detect violations only in 1 file"
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.output.contains "SpotBugs violations were found in 1 files"

        when: "change configuration"
        buildFile.delete()
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }

            quality {
                strict = false
                // exclude different file with different method to trigger filter file change
                exclude '**/Sample.java'
                fallbackToCompatibleToolVersion = true
            }

            repositories {
                mavenCentral() //required for testKit run
            }
            
            dependencies {
                implementation localGroovy()
            }
        """)
        result = run('spotbugsMain')

        then: "config was re-generated"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.SUCCESS
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.output.contains "SpotBugs violations were found in 1 files"
    }
}
