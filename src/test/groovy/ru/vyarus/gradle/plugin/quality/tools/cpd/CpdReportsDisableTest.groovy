package ru.vyarus.gradle.plugin.quality.tools.cpd

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 10.11.2019
 */
class CpdReportsDisableTest extends AbstractKitTest {

    def "Check reports disable"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'de.aaschmid.cpd' version '3.5'
                id 'ru.vyarus.quality'
            }            

            quality {
                pmd false
                checkstyle false
                spotbugs false
                strict false
                consoleReporting false
                htmlReports false
            }

            repositories {            
                mavenCentral();
            }
        """)

        fileFromClasspath('src/main/java/sample/cpd/Struct1.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct1.java')
        fileFromClasspath('src/main/java/sample/cpd/Struct2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/cpd/Struct2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "cpd detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CPD found duplicate code')
        !result.output.contains('java duplicates were found by CPD')

        and: "xml report generated but not html"
        file('build/reports/cpd/cpdCheck.xml').exists()
        !file('build/reports/cpd/cpdCheck.html').exists()
    }
}
