package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import ru.vyarus.gradle.plugin.quality.report.ReportUtils
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 20.02.2018
 */
@IgnoreIf({jvm.java8})
class SpotbugsReporterTest extends AbstractKitTest {

    def "Check spotbugs report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            task testReport() {
                doLast {
                    new ru.vyarus.gradle.plugin.quality.report.SpotbugsReporter(project)
                    .report(spotbugsMain, 'main')
                }
            }
        """)
        file('src/main/java').mkdirs()
        fileFromClasspath('build/reports/spotbugs/main.xml', '/ru/vyarus/gradle/plugin/quality/report/spotbugs/main.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        unifyString(error).contains """
2 (0 / 2 / 0) SpotBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:11)  [priority 2 / rank 18]
\t>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.

[Performance | URF_UNREAD_FIELD] sample.(Sample2.java:11)  [priority 2 / rank 18]
\t>> Unread field: sample.Sample2.sample
  This field is never read. Consider removing it from the class.

SpotBugs HTML report: file:///${ReportUtils.noRootFilePath(testProjectDir)}/build/reports/spotbugs/main.html
""" as String
    }

    def "Check spotbugs report with plugins"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'                               
            }

            quality {
                spotbugsPlugin 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.0'
                spotbugsPlugin 'com.mebigfatguy.fb-contrib:fb-contrib:7.6.4'
            }
            
            repositories {
                mavenCentral()
            }
            
            task testReport() {
                doLast {
                    new ru.vyarus.gradle.plugin.quality.report.SpotbugsReporter(project)
                    .report(spotbugsMain, 'main')
                }
            }
        """)
        file('src/main/java').mkdirs()
        fileFromClasspath('build/reports/spotbugs/main.xml', '/ru/vyarus/gradle/plugin/quality/report/spotbugs/mainWithPlugins.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        unifyString(error).contains """
4 (0 / 4 / 0) SpotBugs violations were found in 2 files

[fb-contrib project | Correctness | FCBL_FIELD_COULD_BE_LOCAL] sample.(Sample.java:11)  [priority 2 / rank 7]
\t>> Class sample.Sample defines fields that are used only as locals
  This class defines fields that are used in a locals only fashion,
  specifically private fields or protected fields in final classes that are accessed
  first in each method with a store vs. a load. This field could be replaced by one
  or more local variables.

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:11)  [priority 2 / rank 18]
\t>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.

[fb-contrib project | Correctness | FCBL_FIELD_COULD_BE_LOCAL] sample.(Sample2.java:11)  [priority 2 / rank 7]
\t>> Class sample.Sample2 defines fields that are used only as locals
  This class defines fields that are used in a locals only fashion,
  specifically private fields or protected fields in final classes that are accessed
  first in each method with a store vs. a load. This field could be replaced by one
  or more local variables.

[Performance | URF_UNREAD_FIELD] sample.(Sample2.java:11)  [priority 2 / rank 18]
\t>> Unread field: sample.Sample2.sample
  This field is never read. Consider removing it from the class.

SpotBugs HTML report: file:///${ReportUtils.noRootFilePath(testProjectDir)}/build/reports/spotbugs/main.html
""" as String
    }
}
