package ru.vyarus.gradle.plugin.quality.tools.cpd

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

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
                findbugs false
                strict false
            }

            repositories {            
                jcenter(); mavenCentral();
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')
        fileFromClasspath('src/main/java/sample/cpd/OtherStruct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct1.java')
        fileFromClasspath('src/main/java/sample/cpd/OtherStruct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "cpd detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('4 duplicates were found by CPD')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
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

        then: "task skipped"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":cpdCheck").outcome == TaskOutcome.SKIPPED
    }

    def "Check multi-module cpd integration"() {
        setup:
        debug()
        build("""         
            plugins {
                id 'ru.vyarus.quality'
                id 'de.aaschmid.cpd' version '3.0'
            }
            
            allprojects {
                repositories {
                    jcenter() //required for testKit run
                }
            }

            subprojects {
                apply plugin: 'java'
                apply plugin: 'ru.vyarus.quality'

                quality {
                    strict = false
                    checkstyle = false
                    findbugs = false
                    pmd = false
                    spotbugs = false
                }               
            }
        """)

        file('settings.gradle') << "include 'mod1', 'mod2'"

        fileFromClasspath('mod1/src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('mod2/src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')
        fileFromClasspath('mod1/src/main/java/sample/cpd/OtherStruct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct1.java')
        fileFromClasspath('mod2/src/main/java/sample/cpd/OtherStruct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "cpd detect violations"
        result.task(":cpdCheck").outcome == TaskOutcome.SUCCESS
        result.output.contains('4 duplicates were found by CPD')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
    }
}
