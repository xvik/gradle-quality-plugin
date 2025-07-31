package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 31.07.2025
 */
@IgnoreIf({jvm.java8})
class AutomaticSpotbugsActivationKitTest extends AbstractKitTest {

    def "Check spotbugs plugin not applied recognition"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN' apply false
            }

            quality {
                checkstyle = false
                pmd = false
                strict false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('SpotBugs violations were found')

        then: "all html reports generated"
        file('build/reports/spotbugs/main.html').exists()
    }


    def "Check automatic spotbugs activation in sub module"() {
        setup:
        build("""
            plugins {
                    id 'ru.vyarus.quality'
                    id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN' apply false
            }

            subprojects {
                apply plugin: 'java'
                apply plugin: 'ru.vyarus.quality'

                quality {
                    strict false
                    checkstyle = false
                    pmd = false
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
        def out =  unifyString(result.output)
        out.contains("""
> Task :mod1:spotbugsMain
SpotBugs ended with exit code 1

1 (0 / 1 / 0) SpotBugs violations were found in 1 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:11)  [priority 2 / rank 18]
\t>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.
""")
        out.contains("""
> Task :mod2:spotbugsMain
SpotBugs ended with exit code 1

1 (0 / 1 / 0) SpotBugs violations were found in 1 files

[Performance | URF_UNREAD_FIELD] sample.(Sample2.java:11)  [priority 2 / rank 18]
\t>> Unread field: sample.Sample2.sample
  This field is never read. Consider removing it from the class.
""")
    }
}
