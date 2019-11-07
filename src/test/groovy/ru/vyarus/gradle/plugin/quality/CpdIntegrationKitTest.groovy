package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 07.11.2019
 */
class CpdIntegrationKitTest extends AbstractKitTest {

    def "Check cpd integration"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.0'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd false
                checkstyle false
                spotbugs false
                strict false
            }

            repositories {            
                jcenter(); mavenCentral();
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CPD found duplicate code. See the report at')
    }

    def "Check cpd disable"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.0'
                id 'ru.vyarus.quality'
            }

            quality {
                enabled = false
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":cpdCheck").outcome == TaskOutcome.SKIPPED
    }
}
