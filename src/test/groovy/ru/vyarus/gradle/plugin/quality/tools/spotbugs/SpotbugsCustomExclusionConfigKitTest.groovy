package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.SpotbugsTool
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 20.08.2025
 */
@IgnoreIf({jvm.java8})
class SpotbugsCustomExclusionConfigKitTest extends AbstractKitTest {

    def "Check main plugins sources exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }

            quality {
                strict = false
                configDir = 'configs'
                excludeSources = fileTree('build/generated/')
                fallbackToCompatibleToolVersion = true
            }

            repositories {
                mavenCentral() //required for testKit run
            }
            
            dependencies {
                implementation localGroovy()
            }
        """)

        def exclude = file("configs/spotbugs/exclude.xml")
        exclude.parentFile.mkdirs()
        exclude << """<?xml version="1.0" encoding="UTF-8"?>
        <FindBugsFilter>                
            <Match>
                <Class name="foo.SomeFoo" />
            </Match>           
        </FindBugsFilter>
        """

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('build/generated/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')

        when: "prepare configs"
        BuildResult result = run('spotbugsMain')

        then: "user-provided spotbugs file modified"
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.output.contains "SpotBugs violations were found in 1 files"

        and: 'config overridden'
        File cfg = file("build/quality-configs/$SpotbugsTool.spotbugs_exclude")
        cfg.exists()
        unifyString(cfg.text.trim()) == """
        <?xml version="1.0" encoding="UTF-8"?>
        <FindBugsFilter>
          <Match>
            <Class name="foo.SomeFoo"/>
          </Match>
          <Match>
            <Class name="~sample\\.Sample2(\\\$.*)?"/>
          </Match>
        </FindBugsFilter>
        """.stripIndent().trim()

        when: "change configuration"
        buildFile.delete()
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }
            
            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }

            quality {
                strict = false
                configDir = 'configs'
                // exclude different file with different method to trigger filter file change
                exclude '**/Sample.java'
                fallbackToCompatibleToolVersion = true
            }

            repositories {
                mavenCentral() //required for testKit run
            }
            
            dependencies {
                implementation localGroovy()
            }
        """)
        result = run('spotbugsMain')

        then: "config was re-generated"
        result.task(":copyQualityConfigs").outcome == TaskOutcome.SUCCESS
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.output.contains "SpotBugs violations were found in 1 files"

        and: 'config valid'
        cfg.exists()
        unifyString(cfg.text.trim()) == """
        <?xml version="1.0" encoding="UTF-8"?>
        <FindBugsFilter>
          <Match>
            <Class name="foo.SomeFoo"/>
          </Match>
          <Match>
            <Class name="~sample\\.Sample(\\\$.*)?"/>
          </Match>
        </FindBugsFilter>
        """.stripIndent().trim()
    }
}
