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
class ConfCacheCpdKitTest extends AbstractKitTest {

    def "Check cpd integration"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.5'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd = false
                checkstyle = false
                spotbugs = false
                strict = false
            }

            repositories {            
                mavenCentral();
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')
        fileFromClasspath('src/main/java/sample/cpd/OtherStruct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct1.java')
        fileFromClasspath('src/main/java/sample/cpd/OtherStruct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "cpd detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('3 java duplicates were found by CPD')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.output.contains('3 java duplicates were found by CPD')
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
    }

}
