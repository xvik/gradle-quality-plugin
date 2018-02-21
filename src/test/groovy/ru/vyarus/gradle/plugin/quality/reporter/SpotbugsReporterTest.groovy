package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import ru.vyarus.gradle.plugin.quality.report.ReportUtils

/**
 * @author Vyacheslav Rusakov
 * @since 20.02.2018
 */
class SpotbugsReporterTest extends AbstractKitTest {

    def "Check spotbugs report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            task testReport() << {
                new ru.vyarus.gradle.plugin.quality.report.SpotbugsReporter(
                    new ru.vyarus.gradle.plugin.quality.ConfigLoader(project)
                ).report(project, 'main')
            }
        """)
        file('src/main/java').mkdirs()
        fileFromClasspath('build/reports/spotbugs/main.xml', '/ru/vyarus/gradle/plugin/quality/report/spotbugs/main.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        error.replaceAll("\r", '').contains """
2 (0 / 2 / 0) SpotBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:11)  [priority 2]
\t>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.

[Performance | URF_UNREAD_FIELD] sample.(Sample2.java:11)  [priority 2]
\t>> Unread field: sample.Sample2.sample
  This field is never read. Consider removing it from the class.

SpotBugs HTML report: file:///${ReportUtils.noRootFilePath(testProjectDir.root)}/build/reports/spotbugs/main.html
""" as String
    }
}
