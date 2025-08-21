package ru.vyarus.gradle.plugin.quality.cache

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 19.08.2025
 */
class ConfCacheExcludeSourcesKitTest extends AbstractKitTest {

    def "Check main plugins sources exclusion"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
                groovy {
                    srcDir 'build/generated/groovy'
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
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('build/generated/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with java and groovy sources"
        BuildResult result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "all plugins detect violations only in 1 file"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "2 Checkstyle rule violations were found in 1 files"
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
        output.contains "6 PMD rule violations were found in 1 files"
        output.contains "15 (0 / 6 / 9) CodeNarc violations were found in 1 files"

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')
        output = result.output

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        output.contains "2 Checkstyle rule violations were found in 1 files"
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
        output.contains "6 PMD rule violations were found in 1 files"
        output.contains "15 (0 / 6 / 9) CodeNarc violations were found in 1 files"
    }
}
