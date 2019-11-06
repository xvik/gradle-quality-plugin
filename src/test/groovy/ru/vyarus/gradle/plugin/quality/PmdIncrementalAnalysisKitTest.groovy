package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 05.11.2019
 */
class PmdIncrementalAnalysisKitTest extends AbstractKitTest {

    def "Check pmd incremental mode enabling"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            
                pmdIncremental = true
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check task with java sources"
        BuildResult result = run('pmdMain')

        then: "all plugins detect violations"
        result.task(":pmdMain").outcome == TaskOutcome.SUCCESS
    }

    def "Check pmd incremental mode not enabled on older gradle"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            
                pmdIncremental = true
            }

            repositories {
                jcenter() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check task with java sources"
        BuildResult result = runVer('5.1', 'pmdMain')

        then: "all plugins detect violations"
        result.task(":pmdMain").outcome == TaskOutcome.SUCCESS
        result.output.contains("WARNING: PMD incremental analysis option ignored, because it's supported only from gradle 5.6")
    }
}
