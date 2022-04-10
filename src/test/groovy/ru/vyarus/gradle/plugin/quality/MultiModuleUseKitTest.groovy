package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import ru.vyarus.gradle.plugin.quality.report.ReportUtils

/**
 * @author Vyacheslav Rusakov
 * @since 24.08.2016
 */
class MultiModuleUseKitTest extends AbstractKitTest {

    def "Check java checks"() {
        setup:
        build("""
            plugins {
                    id 'ru.vyarus.quality'
            }

            subprojects {
                apply plugin: 'java'
                apply plugin: 'ru.vyarus.quality'

                quality {
                    strict false
                    pmd = false
                    spotbugs = false
                }

                repositories {
                    mavenCentral() //required for testKit run
                }
            }
        """)

        file('settings.gradle') << "include 'mod1', 'mod2'"

        fileFromClasspath('mod1/src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('mod2/src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check for both modules"
        BuildResult result = run('check')

        then: "violations detected in module only"
        unifyString(result.output).replaceAll("Total time: .*", "")
                // only on jdk 8
                .replaceAll("WARNING: checkstyle-backport-jre8 .*\n", "")
                .trim().startsWith("""> Task :mod1:compileJava
> Task :mod1:processResources NO-SOURCE
> Task :mod1:classes

> Task :mod1:checkstyleMain
Checkstyle rule violations were found. See the report at: file:///tmp/junit6300057182805361069/mod1/build/reports/checkstyle/main.html
Checkstyle files with violations: 1
Checkstyle violations by severity: [error:2]


2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:1)
  File does not end with a newline.
  http://checkstyle.sourceforge.io/config_misc.html#NewlineAtEndOfFile

[Javadoc | MissingJavadocType] sample.(Sample.java:6)
  Missing a Javadoc comment.
  http://checkstyle.sourceforge.io/config_javadoc.html#MissingJavadocType


> Task :mod1:compileTestJava NO-SOURCE
> Task :mod1:processTestResources NO-SOURCE
> Task :mod1:testClasses UP-TO-DATE
> Task :mod1:test NO-SOURCE
> Task :mod1:check
> Task :mod2:compileJava
> Task :mod2:processResources NO-SOURCE
> Task :mod2:classes

> Task :mod2:checkstyleMain
Checkstyle rule violations were found. See the report at: file:///tmp/junit6300057182805361069/mod2/build/reports/checkstyle/main.html
Checkstyle files with violations: 1
Checkstyle violations by severity: [error:2]


2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:1)
  File does not end with a newline.
  http://checkstyle.sourceforge.io/config_misc.html#NewlineAtEndOfFile

[Javadoc | MissingJavadocType] sample.(Sample.java:6)
  Missing a Javadoc comment.
  http://checkstyle.sourceforge.io/config_javadoc.html#MissingJavadocType


> Task :mod2:compileTestJava NO-SOURCE
> Task :mod2:processTestResources NO-SOURCE
> Task :mod2:testClasses UP-TO-DATE
> Task :mod2:test NO-SOURCE
> Task :mod2:check

BUILD SUCCESSFUL""".replaceAll("tmp/junit6300057182805361069", ReportUtils.noRootFilePath(testProjectDir)))
    }
}