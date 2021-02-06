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
                id 'de.aaschmid.cpd' version '3.1'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd false
                checkstyle false
                spotbugs false
                strict false
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
        BuildResult result = run('check')

        then: "cpd detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('4 java duplicates were found by CPD')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
    }

    def "Check source scope reduce"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.1'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd false
                checkstyle false
                spotbugs false
                strict false
            }

            repositories {            
                mavenCentral();
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')
        fileFromClasspath('src/test/java/sample/cpd/OtherStruct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct1.java')
        fileFromClasspath('src/test/java/sample/cpd/OtherStruct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "cpd detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('1 java duplicates were found by CPD')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
    }

    def "Check cpd disable"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.1'
                id 'ru.vyarus.quality'
            }

            quality {
                enabled = false
            }

            repositories {
                mavenCentral() //required for testKit run
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
        build("""         
            plugins {
                id 'ru.vyarus.quality'
                id 'de.aaschmid.cpd' version '3.1'
            }
            
            allprojects {
                repositories {
                    mavenCentral() //required for testKit run
                }
            }

            subprojects {
                apply plugin: 'java'
                apply plugin: 'ru.vyarus.quality'

                quality {
                    strict = false
                    checkstyle = false
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
        result.output.contains('4 java duplicates were found by CPD')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
    }

    def "Check groovy only sources detection"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'de.aaschmid.cpd' version '3.1'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd false
                checkstyle false
                spotbugs false
                codenarc = false
                strict false
            }                          

            repositories {
                mavenCentral() //required for testKit run
            }
            
            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/groovy/sample/cpd/GStruct1.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/cpd/GStruct1.groovy')
        fileFromClasspath('src/main/groovy/sample/cpd/GStruct2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/cpd/GStruct2.groovy')

        when: "run check task with groovy sources"
        BuildResult result = run('cpdCheck')

        then: "cpd detect violations"
        result.task(":cpdCheck").outcome == TaskOutcome.SUCCESS
        result.output.contains('1 groovy duplicates were found by CPD')
        result.output.contains('sample.cpd.(GStruct1.groovy:6)')
    }
}
