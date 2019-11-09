package ru.vyarus.gradle.plugin.quality.tools.cpd

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 09.11.2019
 */
class MultipleCpdTasksKitTest extends AbstractKitTest {

    def "Check multiple cpd tasks reporting"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'de.aaschmid.cpd' version '3.0'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd false
                checkstyle false
                spotbugs false
                findbugs false
                codenarc = false
                strict false
            }   
            
            cpdCheck.exclude '*.groovy'                     
            
            task groovyCpdCheck(type:de.aaschmid.gradle.plugins.cpd.Cpd) {
                language = 'groovy'
                source = sourceSets.main.allGroovy 
            }
            tasks.check.dependsOn groovyCpdCheck

            repositories {
                jcenter() //required for testKit run
            }
            
            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')
        fileFromClasspath('src/main/groovy/sample/cpd/GStruct1.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/cpd/GStruct1.groovy')
        fileFromClasspath('src/main/groovy/sample/cpd/GStruct2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/cpd/GStruct2.groovy')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "cpd detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.task(":cpdCheck").outcome == TaskOutcome.SUCCESS
        result.task(":groovyCpdCheck").outcome == TaskOutcome.SUCCESS
        result.output.contains('1 java duplicates were found by CPD')
        result.output.contains('1 groovy duplicates were found by CPD')
        result.output.contains('sample.cpd.(Struct2.java:6)')
        result.output.contains('sample.cpd.(GStruct1.groovy:6)')

        and: "xml report generated"
        file('build/reports/cpd/cpdCheck.xml').exists()
        file('build/reports/cpd/cpdCheck.html').exists()
        file('build/reports/cpd/groovyCpdCheck.xml').exists()
        file('build/reports/cpd/groovyCpdCheck.html').exists()
    }
}
