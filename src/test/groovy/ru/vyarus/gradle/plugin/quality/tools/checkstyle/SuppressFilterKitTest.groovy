package ru.vyarus.gradle.plugin.quality.tools.checkstyle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 04.02.2021
 */
class SuppressFilterKitTest extends AbstractKitTest {

    def "Check java checks"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
                spotbugs false
                pmd false
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        fileFromClasspath('gradle/config/checkstyle/suppressions.xml', '/ru/vyarus/gradle/plugin/quality/config/checkstyle/suppressions.xml')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "violations suppressed"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        // 1 issues suppressed by check in 1st file, 2 issues suppressed by message
        unifyString(result.output).contains """
1 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample2.java:1)
  File does not end with a newline.
  http://checkstyle.sourceforge.io/config_misc.html#NewlineAtEndOfFile
"""
    }
}
