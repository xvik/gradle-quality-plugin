package ru.vyarus.gradle.plugin.quality.tools.pmd

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 05.11.2019
 */
class PmdIncrementalAnalysisKitTest extends AbstractKitTest {

    def "Check pmd incremental mode enabled by default"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
                
                // does not affect enabled status in gradle  >= 6.4
                pmdIncremental = false
            }
            
            task validator {
                doFirst() {
                    if (!pmdMain.incrementalAnalysis.get()) {
                        throw new IllegalStateException('PMD incremental analysis not enabled');
                    }
                }
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check task with java sources"
        BuildResult result = run('pmdMain', 'validator')

        then: "all plugins detect violations"
        result.task(":pmdMain").outcome == TaskOutcome.SUCCESS
    }
}
