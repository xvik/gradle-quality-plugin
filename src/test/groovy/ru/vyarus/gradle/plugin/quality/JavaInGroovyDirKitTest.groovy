package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 02.09.2016
 */
class JavaInGroovyDirKitTest extends AbstractKitTest {

    def "Check java and groovy checks disable"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'checkstyle'
                id 'ru.vyarus.quality'
            }

            quality {
                strict = false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        // java in groovy folder
        fileFromClasspath('src/main/groovy/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run main quality check"
        BuildResult result = run('checkstyleMain')

        then: "violations detected"
        result.output.contains('Checkstyle rule violations were found')

        then: "correct package build"
        result.output.contains('[Misc | NewlineAtEndOfFile] sample.(Sample.java:1)')
    }
}