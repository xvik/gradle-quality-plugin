package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 10.10.2020
 */
@IgnoreIf({jvm.java8})
class ManualClassesExclusionKitTest extends AbstractKitTest {

    def "Check manual spotbugs exclusion"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                checkstyle = false
                pmd = false
                strict = false
            }
            
            afterEvaluate {
                tasks.withType(com.github.spotbugs.snom.SpotBugsTask).configureEach {
                    classes = classes.filter { 
                        !it.path.endsWith('Sample2.class')
                    }
                }
            }

            repositories {
                mavenCentral() //required for testKit run
            }
            
            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java and groovy sources"
        BuildResult result = run('check')

        then: "spotbugs detect violations only in 1 file"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
    }
}
