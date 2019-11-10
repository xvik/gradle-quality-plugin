package ru.vyarus.gradle.plugin.quality.tools.cpd

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 08.11.2019
 */
class CpdExcludesKitTest extends AbstractKitTest {

    def "Check cpd exclusions"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.0'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
                checkstyle false
                spotbugs false
                pmd false                
                exclude '**/Struct2.java', '**/OtherStruct2.java' 
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')
        fileFromClasspath('src/main/java/sample/cpd/OtherStruct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct1.java')
        fileFromClasspath('src/main/java/sample/cpd/OtherStruct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/OtherStruct2.java')

        when: "run check task"
        BuildResult result = run('check')

        then: "no issues detected - exclusions applied"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        !output.contains("CPD found duplicate code. See the report at")
    }

    def "Check cpd sources exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.0'
                id 'ru.vyarus.quality'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }

            quality {
                strict = false
                checkstyle false
                spotbugs false
                pmd false     
                excludeSources = fileTree('build/generated/')
            }

            repositories {
                jcenter() //required for testKit run
            }
            
            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('build/generated/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')

        when: "run check task"
        BuildResult result = run('check')

        then: "cpd violation not detected"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        !output.contains("CPD found duplicate code. See the report at")
    }

}
