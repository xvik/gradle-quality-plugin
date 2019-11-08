package ru.vyarus.gradle.plugin.quality.tools.findbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 21.12.2015
 */
class FindbugsPluginsKitTest extends AbstractKitTest {

    def "Check findbugs plugins"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'findbugs'
                id 'ru.vyarus.quality'
            }
            
            sourceCompatibility = 1.8

            quality {
                checkstyle false
                pmd false
                strict false
                spotbugs false
            }

            repositories { mavenCentral() }
            dependencies {
                findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.6.0'
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('FindBugs rule violations were found')
    }

    def "Check findbugs plugins 2"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }
            
            sourceCompatibility = 1.8

            quality {
                checkstyle false
                pmd false
                strict false
                spotbugs false
            }

            repositories { mavenCentral() }
            afterEvaluate {
                dependencies {
                    findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.6.0'
                }
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('FindBugs rule violations were found')
    }
}