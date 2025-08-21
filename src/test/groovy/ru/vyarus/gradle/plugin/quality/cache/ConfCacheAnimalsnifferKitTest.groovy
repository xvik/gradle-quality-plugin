package ru.vyarus.gradle.plugin.quality.cache

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 19.08.2025
 */
@IgnoreIf({jvm.java8})
class ConfCacheAnimalsnifferKitTest extends AbstractKitTest {

    def "Check animalsniffer integration"() {
        setup:
        build("""
            plugins {
                id 'java'
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
        BuildResult result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "violations detected"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('4 AnimalSniffer violations were found in 2 files')


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "cache used, but animalsniffer tasks didn't output"
        result.output.contains('Reusing configuration cache.')
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":animalsnifferMain").outcome == TaskOutcome.UP_TO_DATE
        !result.output.contains('4 AnimalSniffer violations were found in 2 files')
    }

}
