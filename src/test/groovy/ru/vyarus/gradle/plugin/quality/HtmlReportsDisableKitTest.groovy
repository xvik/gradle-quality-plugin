package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 12.07.2018
 */
class HtmlReportsDisableKitTest extends AbstractKitTest {

    def "Check java and groovy checks without html reports"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
                htmlReports false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with both sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs rule violations were found')
        result.output.contains('PMD rule violations were found')

        then: "html reports are not generated"
        !file('build/reports/codenarc/main.html').exists()
        !file('build/reports/checkstyle/main.html').exists()
        !file('build/reports/spotbugs/main.html').exists()
        !file('build/reports/pmd/main.html').exists()
    }


    def "Check findbugs html report disable"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }
            
            sourceCompatibility = 1.8

            quality {
                strict false
                spotbugs false
                htmlReports false
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with both sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('FindBugs rule violations were found')

        then: "html reports are not generated"
        !file('build/reports/findbugs/main.html').exists()
    }
}
