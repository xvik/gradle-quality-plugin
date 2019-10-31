package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 21.03.2017
 */
class ExcludeSourcesKitTest extends AbstractKitTest {

    def "Check main plugins sources exclusion"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
                groovy {
                    srcDir 'build/generated/groovy'
                }
            }

            quality {
                strict = false
                excludeSources = fileTree('build/generated/')
            }

            repositories {
                jcenter() //required for testKit run
            }
            
            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('build/generated/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('build/generated/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with java and groovy sources"
        BuildResult result = run('check')

        then: "all plugins detect violations only in 1 file"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "4 Checkstyle rule violations were found in 1 files"
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
        output.contains "7 PMD rule violations were found in 1 files"
        output.contains "15 (0 / 6 / 9) CodeNarc violations were found in 1 files"
    }

    def "Check both exclusions together for main plugins"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
                groovy {
                    srcDir 'build/generated/groovy'
                }
            }

            quality {
                strict = false
                exclude = ['**/Sample3.java', '**/GSample3.groovy']
                excludeSources = fileTree('build/generated/')
            }

            repositories {
                jcenter() //required for testKit run
            }
            
            dependencies {
                compile localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample3.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample3.java')
        fileFromClasspath('build/generated/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample3.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample3.groovy')
        fileFromClasspath('build/generated/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with java and groovy sources"
        BuildResult result = run('check')

        then: "all plugins detect violations only in 1 file"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        def output = result.output
        output.contains "4 Checkstyle rule violations were found in 1 files"
        output.contains "1 (0 / 1 / 0) SpotBugs violations were found in 1 files"
        output.contains "7 PMD rule violations were found in 1 files"
        output.contains "15 (0 / 6 / 9) CodeNarc violations were found in 1 files"
    }

    def "Check animalsniffer exclusion ignore"() {

        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer' version '1.5.0'
                id 'ru.vyarus.quality'
            }

            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }
            
            quality {
                strict false
                findbugs false
                pmd false
                checkstyle false
                excludeSources = fileTree('build/generated/')
            }

            repositories {
                jcenter(); mavenCentral();
            }            
            dependencies {
                signature 'org.codehaus.mojo.signature:java15:1.0@signature'
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('build/generated/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "exclusions are not applied to animalsniffer"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains "4 AnimalSniffer violations were found in 2 files"
    }
}
