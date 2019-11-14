package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 20.02.2018
 */
class SpotbugsPluginsKitTest extends AbstractKitTest {

    def "Check spotbugs plugins"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'com.github.spotbugs'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
            }

            repositories { mavenCentral() }
            dependencies {
                spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('SpotBugs rule violations were found')
    }

    def "Check spotbugs plugins 2"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
            }

            repositories { mavenCentral() }
            afterEvaluate {
                dependencies {
                    spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'
                }
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('SpotBugs rule violations were found')
    }

    def "Check spotbugs plugins shortcut syntax"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
                
                spotbugsPlugin 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.0'
                spotbugsPlugin 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'
            }

            repositories { mavenCentral() }            
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('SpotBugs rule violations were found')

//        cleanup:
//        new File("spotbugs.xml") << file('build/reports/spotbugs/main.xml').text
    }
}
