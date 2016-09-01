package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import ru.vyarus.gradle.plugin.quality.report.ReportUtils

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
class FindbugsReporterTest extends AbstractKitTest {

    def "Check findbugs report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            task testReport() << {
                new ru.vyarus.gradle.plugin.quality.report.FindbugsReporter(
                    new ru.vyarus.gradle.plugin.quality.ConfigLoader(project)
                ).report(project, 'main')
            }
        """)
        file('src/main/java').mkdirs()
        fileFromClasspath('build/reports/findbugs/main.xml', '/ru/vyarus/gradle/plugin/quality/report/findbugs/main.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        error.replaceAll("\r", '').contains """
2 (0 / 2 / 0) FindBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:8)  [priority 2]
\t>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.

[Performance | URF_UNREAD_FIELD] sample.(Sample2.java:8)  [priority 2]
\t>> Unread field: sample.Sample2.sample
  This field is never read. Consider removing it from the class.

Findbugs HTML report: file:///${ReportUtils.noRootFilePath(testProjectDir.root)}/build/reports/findbugs/main.html
""" as String
    }
}