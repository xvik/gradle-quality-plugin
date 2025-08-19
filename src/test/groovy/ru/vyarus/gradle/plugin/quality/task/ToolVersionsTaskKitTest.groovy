package ru.vyarus.gradle.plugin.quality.task

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf
import spock.lang.Requires

/**
 * @author Vyacheslav Rusakov
 * @since 07.08.2025
 */
@IgnoreIf({ jvm.java8 })
class ToolVersionsTaskKitTest extends AbstractKitTest {

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
        BuildResult result = run('qualityToolVersions')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Java version:')
        result.output.contains('Gradle version:')
        result.output.contains('Checkstyle:')
    }

    @Requires({jvm.java11Compatible})
    def "Check disabled tools version info"() {

        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }
            
            quality {
                checkstyle=false
                pmd=false
                spotbugs=false
                codenarc=false
                
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
        BuildResult result = run('qualityToolVersions')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        unifyString(result.output).contains("""Java version: ${JavaVersion.current()}
Gradle version: ${GradleVersion.current().version}
Checkstyle: disabled
PMD: disabled
SpotBugs: disabled
CodeNarc: disabled
""")
    }

    def "Check tools version info with groovy only sources"() {

        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')

        when: "run main quality check"
        BuildResult result = run('qualityToolVersions')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Java version:')
        result.output.contains('Gradle version:')
        !result.output.contains('Checkstyle:')
        result.output.contains('CodeNarc:')
    }

    @IgnoreIf({ jvm.java17Compatible })
    @Requires({ jvm.java11Compatible })
    def "Check checkstyle fallback on java 11"() {

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

        when: "run main quality check"
        BuildResult result = run('qualityToolVersions')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle: 10.26.1')
    }

    @IgnoreIf({ jvm.java17Compatible })
    @Requires({ jvm.java11Compatible })
    def "Check checkstyle auto enable on java 11"() {

        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }
            
            quality {
               checkstyleVersion = '10.26.1' 
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run main quality check"
        BuildResult result = run('qualityToolVersions')

        then: "all plugins detect violations"
        result.task(":qualityToolVersions").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle: 10.26.1')
    }
}
