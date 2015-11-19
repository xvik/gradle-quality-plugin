package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
class CheckstyleReporterTest extends AbstractKitTest {

    def "Check checkstyle report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            task testReport() << {
                sourceSets.main.java.srcDir 'C:/Users/xvik/AppData/Local/Temp/junit1006517273776660000/src/main/java/'
                new ru.vyarus.gradle.plugin.quality.report.CheckstyleReporter(
                    new ru.vyarus.gradle.plugin.quality.ConfigLoader(project)
                ).report(project, 'main')
            }
        """)
        file('src/main/java').mkdirs()
        fileFromClasspath('build/reports/checkstyle/main.xml', '/ru/vyarus/gradle/plugin/quality/report/checkstyle/main.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.standardError

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
       error.replaceAll("\r", "") == """
6 Checkstyle rule violations were found in 2 files

[Misc | NewlineAtEndOfFile] sample.Sample:0
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile

[Javadoc | JavadocType] sample.Sample:3
  Missing a Javadoc comment.
  http://checkstyle.sourceforge.net/config_javadoc.html#JavadocType

[Misc | UncommentedMain] sample.Sample:11
  Uncommented main method found.
  http://checkstyle.sourceforge.net/config_misc.html#UncommentedMain

[Misc | NewlineAtEndOfFile] sample.Sample2:0
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile

[Javadoc | JavadocType] sample.Sample2:3
  Missing a Javadoc comment.
  http://checkstyle.sourceforge.net/config_javadoc.html#JavadocType

[Misc | UncommentedMain] sample.Sample2:11
  Uncommented main method found.
  http://checkstyle.sourceforge.net/config_misc.html#UncommentedMain

Checkstyle HTML report: file:///${testProjectDir.root.canonicalPath.replaceAll('\\\\', '/')}/build/reports/checkstyle/main.html
""" as String
    }
}