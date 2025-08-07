package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import ru.vyarus.gradle.plugin.quality.report.ReportUtils
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 24.08.2016
 */
@IgnoreIf({jvm.java8})
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
                    strict = false
                    pmd = false
                    spotbugs = false
                    fallbackToCompatibleToolVersion = true
                }

                repositories {
                    mavenCentral() //required for testKit run
                }
            }
        """)

        file('settings.gradle') << "include 'mod1', 'mod2'"

        fileFromClasspath('mod1/src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('mod2/src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check for both modules"
        BuildResult result = run('check')

        then: "violations detected in module only"
        def out =  unifyString(result.output).replaceAll("Total time: .*", "")
                // only on jdk 8
                .replaceAll("WARNING: checkstyle-backport-jre8 .*\n", "")
                out.contains("""
> Task :mod1:checkstyleMain
Checkstyle rule violations were found. See the report at: file:///tmp/junit6300057182805361069/mod1/build/reports/checkstyle/main.html
Checkstyle files with violations: 1
Checkstyle violations by severity: [error:2]


2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:1)
  File does not end with a newline.
  https://checkstyle.sourceforge.io/checks/misc/newlineatendoffile.html

[Javadoc | MissingJavadocType] sample.(Sample.java:6)
  Missing a Javadoc comment.
  https://checkstyle.sourceforge.io/checks/javadoc/missingjavadoctype.html

""".replaceAll("tmp/junit6300057182805361069", ReportUtils.noRootFilePath(testProjectDir)))

        out.contains("""
> Task :mod2:checkstyleMain
Checkstyle rule violations were found. See the report at: file:///tmp/junit6300057182805361069/mod2/build/reports/checkstyle/main.html
Checkstyle files with violations: 1
Checkstyle violations by severity: [error:2]


2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample2.java:1)
  File does not end with a newline.
  https://checkstyle.sourceforge.io/checks/misc/newlineatendoffile.html

[Javadoc | MissingJavadocType] sample.(Sample2.java:6)
  Missing a Javadoc comment.
  https://checkstyle.sourceforge.io/checks/javadoc/missingjavadoctype.html

""".replaceAll("tmp/junit6300057182805361069", ReportUtils.noRootFilePath(testProjectDir)))
    }
}