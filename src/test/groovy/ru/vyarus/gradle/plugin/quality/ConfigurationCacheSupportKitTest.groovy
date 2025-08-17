package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 05.02.2024
 */
class ConfigurationCacheSupportKitTest extends AbstractKitTest {

    def "Check java and groovy checks"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
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
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with both sources"
        BuildResult result = run('check', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'check')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':check').outcome == TaskOutcome.UP_TO_DATE

    }

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

    def "Check tools version info"() {

        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }
            
            quality {
                fallbackToCompatibleToolVersion = true
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')

        when: "run main quality check"
        BuildResult result = run('qualityToolVersions', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "cache record not exists"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Java version:')
        result.output.contains('Gradle version:')
        result.output.contains('Checkstyle:')

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'qualityToolVersions')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Java version:')
        result.output.contains('Gradle version:')
        result.output.contains('Checkstyle:')
    }

}
