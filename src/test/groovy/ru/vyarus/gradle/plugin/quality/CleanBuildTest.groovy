package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 11.12.2015
 */
class CleanBuildTest extends AbstractKitTest {

    def "Check clean build java run"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
            }

            repositories {
                mavenCentral() //required for testKit run
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run clean build"
        BuildResult result = run('clean', 'build')

        then: "temp files are in place"
        true
    }

    def "Check clean build groovy run"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
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

        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run clean build"
        BuildResult result = run('clean', 'build')

        then: "temp files are in place"
        true
    }
}