package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov 
 * @since 12.11.2015
 */
@IgnoreIf({jvm.java8})
class QualityPluginKitTest extends AbstractKitTest {

    def "Check java checks without spotbugs"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
        !file('build/reports/spotbugs/main.html').exists()
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }


    def "Check java checks with spotbugs 6.x"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
        file('build/reports/spotbugs/main.html').exists()
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }

    def "Check java checks with spotbugs 5.x"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '5.2.5'
            }

            quality {
                strict false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
        file('build/reports/spotbugs/main.html').exists()
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }

    def "Check java checks with java-library"() {
        setup:
        build("""
            plugins {
                id 'java-library'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

        then: "all html reports generated"
        file('build/reports/checkstyle/main.html').exists()
        file('build/reports/spotbugs/main.html').exists()
        file('build/reports/pmd/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }

    def "Check groovy checks"() {
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
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with groovy sources"
        BuildResult result = run('check')

        then: "plugin detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')

        then: "html report generated"
        file('build/reports/codenarc/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
    }

    def "Check groovy checks with java-library"() {
        setup:
        build("""
            plugins {
                id 'java-library'
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict false
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with groovy sources"
        BuildResult result = run('check')

        then: "plugin detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')

        then: "html report generated"
        file('build/reports/codenarc/main.html').exists()

        when: "run one more time"
        result = run('check', '--rerun-tasks')

        then: "ok"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
    }

    def "Check java and groovy checks"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict false
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
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc rule violations were found')
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')

//        cleanup:
//        new File("checkstyle.xml") <<  file('build/reports/checkstyle/main.xml').text
//        new File("spotbugs.xml") << file('build/reports/spotbugs/main.xml').text
//        new File("pmd.xml") << file('build/reports/pmd/main.xml').text
//        new File("codenarc.xml") << file('build/reports/codenarc/main.xml').text
    }

    def "Check plugins config override"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            repositories {
                mavenCentral() //required for testKit run
            }
            
            // because spotbugs plugin configuration approach changed 
            quality.spotbugs = false

            afterEvaluate {
                checkstyle {
                    sourceSets = []
                }
                pmd {
                    sourceSets = []
                }                
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "no violations"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        !result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('PMD rule violations were found')
    }
}