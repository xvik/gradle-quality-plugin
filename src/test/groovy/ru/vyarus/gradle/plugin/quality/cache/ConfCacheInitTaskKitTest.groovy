package ru.vyarus.gradle.plugin.quality.cache

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 19.08.2025
 */
@IgnoreIf({jvm.java8})
class ConfCacheInitTaskKitTest extends AbstractKitTest {

    def "Check configs init"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                configDir = 'config'
            }
        """)

        when: "run init configs task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'initQualityConfig')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "configs copied"
        result.task(':initQualityConfig').outcome == TaskOutcome.SUCCESS
        file('config/checkstyle/checkstyle.xml').exists()
        file('config/codenarc/codenarc.xml').exists()
        file('config/spotbugs/exclude.xml').exists()
        file('config/pmd/pmd.xml').exists()
        file('config/cpd/cpdhtml.xslt').exists()

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'initQualityConfig')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':initQualityConfig').outcome == TaskOutcome.SUCCESS
        file('config/checkstyle/checkstyle.xml').exists()
        file('config/codenarc/codenarc.xml').exists()
        file('config/spotbugs/exclude.xml').exists()
        file('config/pmd/pmd.xml').exists()
        file('config/cpd/cpdhtml.xslt').exists()
    }

}
