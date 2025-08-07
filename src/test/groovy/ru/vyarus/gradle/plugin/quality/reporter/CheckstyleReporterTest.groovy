package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
@IgnoreIf({jvm.java8})
class CheckstyleReporterTest extends AbstractKitTest {

    def "Check checkstyle report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }
            
            quality {
                fallbackToCompatibleToolVersion = true
            }

            task testReport() {
                doLast {
                    new ru.vyarus.gradle.plugin.quality.report.CheckstyleReporter().report(checkstyleMain, 'main')
                }
            }
        """)
        file('src/main/java').mkdirs()
        String report = getClass().getResourceAsStream('/ru/vyarus/gradle/plugin/quality/report/checkstyle/main.xml').text
                .replaceAll('\\$\\{srcRoot\\}', file('src/main/java').canonicalPath.replaceAll('\\\\', '\\\\\\\\'))
        File target = file('build/reports/checkstyle/main.xml')
        target.parentFile.mkdirs()
        target << report

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        unifyString(error).contains """
4 Checkstyle rule violations were found in 2 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:0)
  File does not end with a newline.
  https://checkstyle.sourceforge.io/checks/misc/newlineatendoffile.html

[Javadoc | JavadocType] sample.(Sample.java:6)
  Missing a Javadoc comment.
  https://checkstyle.sourceforge.io/checks/javadoc/javadoctype.html

[Misc | NewlineAtEndOfFile] sample.(Sample2.java:0)
  File does not end with a newline.
  https://checkstyle.sourceforge.io/checks/misc/newlineatendoffile.html

[Javadoc | JavadocType] sample.(Sample2.java:6)
  Missing a Javadoc comment.
  https://checkstyle.sourceforge.io/checks/javadoc/javadoctype.html
""" as String
    }
}