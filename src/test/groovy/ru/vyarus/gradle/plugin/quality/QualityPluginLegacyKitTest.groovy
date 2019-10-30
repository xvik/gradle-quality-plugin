package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 21.09.2017
 */
class QualityPluginLegacyKitTest extends AbstractKitTest {

    def "Check java and groovy checks with gradle 5.1 (previous)"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with both sources"
        BuildResult result = runVer('5.1','check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs rule violations were found')
        result.output.contains('PMD rule violations were found')
    }
}
