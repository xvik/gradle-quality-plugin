package ru.vyarus.gradle.plugin.quality.cache

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.Requires

/**
 * @author Vyacheslav Rusakov
 * @since 19.08.2025
 */
@Requires({jvm.java17Compatible})
class ConfCacheExclusionsKitTest extends AbstractKitTest{

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
        output.contains "2 Checkstyle rule violations were found in 1 files"
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
        output.contains "6 PMD rule violations were found in 1 files"
        output.contains "15 (0 / 6 / 9) CodeNarc violations were found in 1 files"
    }

}
