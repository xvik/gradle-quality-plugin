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
class ConfCacheToolsInfoTaskKitTest extends AbstractKitTest {

    def "Check tools version info"() {

        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }
            
            quality {
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
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')

        when: "run main quality check"
        BuildResult result = run('qualityToolVersions', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "cache record not exists"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Java version:')
        result.output.contains('Gradle version:')
        result.output.contains('Checkstyle:')

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'qualityToolVersions')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Java version:')
        result.output.contains('Gradle version:')
        result.output.contains('Checkstyle:')
    }
}
