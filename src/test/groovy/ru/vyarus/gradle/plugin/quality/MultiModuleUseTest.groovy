package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import ru.vyarus.gradle.plugin.quality.report.ReportUtils

/**
 * @author Vyacheslav Rusakov
 * @since 24.08.2016
 */
class MultiModuleUseTest extends AbstractKitTest {

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
                    findbugs = false
                    pmd = false
                }

                repositories {
                    jcenter() //required for testKit run
                }
            }
        """)

        file('settings.gradle') << "include 'mod1', 'mod2'"

        fileFromClasspath('mod1/src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('mod2/src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check for both modules"
        BuildResult result = run('check')

        then: "violations detected in module only"
        result.output.replaceAll("Total time: .*", "").replaceAll("\r", "").trim() == """:mod1:compileJava
:mod1:processResources UP-TO-DATE
:mod1:classes
:mod1:checkstyleMain
Checkstyle rule violations were found. See the report at: file:///tmp/junit6300057182805361069/mod1/build/reports/checkstyle/main.xml

2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:0)
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile

[Javadoc | JavadocType] sample.(Sample.java:3)
  Missing a Javadoc comment.
  http://checkstyle.sourceforge.net/config_javadoc.html#JavadocType

Checkstyle HTML report: file:///tmp/junit6300057182805361069/mod1/build/reports/checkstyle/main.html
:mod1:compileTestJava UP-TO-DATE
:mod1:processTestResources UP-TO-DATE
:mod1:testClasses UP-TO-DATE
:mod1:test UP-TO-DATE
:mod1:check
:mod2:compileJava
:mod2:processResources UP-TO-DATE
:mod2:classes
:mod2:checkstyleMain
Checkstyle rule violations were found. See the report at: file:///tmp/junit6300057182805361069/mod2/build/reports/checkstyle/main.xml

2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:0)
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile

[Javadoc | JavadocType] sample.(Sample.java:3)
  Missing a Javadoc comment.
  http://checkstyle.sourceforge.net/config_javadoc.html#JavadocType

Checkstyle HTML report: file:///tmp/junit6300057182805361069/mod2/build/reports/checkstyle/main.html
:mod2:compileTestJava UP-TO-DATE
:mod2:processTestResources UP-TO-DATE
:mod2:testClasses UP-TO-DATE
:mod2:test UP-TO-DATE
:mod2:check

BUILD SUCCESSFUL""".replaceAll("tmp/junit6300057182805361069", ReportUtils.noRootFilePath(testProjectDir.root))
    }
}