package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 09.11.2021
 */
class ShowStackTraceOptionTest extends AbstractKitTest {

    def "Check default behavior"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
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

        then: "spotbugs detect violations, but don't print stack trace"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains("1 (0 / 1 / 0) SpotBugs violations were found in 1 files")
        !output.contains("org.gradle.api.GradleException: 1 SpotBugs violations were found. See the report at:")
    }

    def "Check original spotbugs behavior"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                spotbugsShowStackTraces true
                checkstyle false
                pmd false
                strict false
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

        then: "spotbugs detect violations and print stack trace"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "org.gradle.api.GradleException: 1 SpotBugs violations were found.See the report at:"
    }
}
