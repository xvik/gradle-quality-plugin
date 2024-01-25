package ru.vyarus.gradle.plugin.quality.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov 
 * @since 15.11.2015
 */
class InitQualityConfigTaskKitTest extends AbstractKitTest {

    def "Check configs init"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs'
            }

            quality {
                configDir = 'config'
            }
        """)

        when: "run init configs task"
        BuildResult result = run('initQualityConfig')

        then: "configs copied"
        result.task(':initQualityConfig').outcome == TaskOutcome.SUCCESS
        file('config/checkstyle/checkstyle.xml').exists()
        file('config/codenarc/codenarc.xml').exists()
        file('config/spotbugs/exclude.xml').exists()
        file('config/pmd/pmd.xml').exists()
        file('config/cpd/cpdhtml.xslt').exists()
    }
}
