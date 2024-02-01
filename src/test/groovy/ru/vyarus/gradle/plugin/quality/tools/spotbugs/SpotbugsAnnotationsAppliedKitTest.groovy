package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 01.02.2024
 */
class SpotbugsAnnotationsAppliedKitTest extends AbstractKitTest {

    def "Check spotbugs annotations applied"() {
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
        """)

        fileFromClasspath('src/main/java/sample/SBSuppressSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/SBSuppressSample.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('SpotBugs violations were found')
        result.output.contains('1 (0 / 1 / 0) SpotBugs violations were found in 1 files')
        !result.output.contains('NP_BOOLEAN_RETURN_NULL')
        result.output.contains('URF_UNREAD_FIELD')
    }

    def "Check spotbugs annotations not applied"() {
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
                spotbugsAnnotations false
            }

            repositories { mavenCentral() }
        """)

        fileFromClasspath('src/main/java/sample/SBSuppressSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/SBSuppressSample.java')

        when: "run check task with java sources"
        BuildResult result = runFailed('compileJava')

        then: "all plugins detect violations"
        result.task(":compileJava").outcome == TaskOutcome.FAILED
        result.output.contains('package edu.umd.cs.findbugs.annotations does not exist')
    }
}
