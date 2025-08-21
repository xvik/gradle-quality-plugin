package ru.vyarus.gradle.plugin.quality.cache


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest
import spock.lang.IgnoreIf
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 19.08.2025
 */
@IgnoreIf({jvm.java8})
class ConfAndBuildCachesKitTest extends AbstractKitTest {

    @TempDir File cacheDir

    def "Check java and groovy checks"() {
        setup:
        // build cache will survive within test only!!
        // spotbugs task cache key depends on project name!!!
        file("settings.gradle") << """  
            rootProject.name='my-project'
            buildCache {
                local {
                    directory = new File('${cacheDir.canonicalPath.replace('\\', '\\\\')}')
                }
            }
"""
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
            }

            quality {
                strict = false
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
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        when: "run check task with both sources with build cache"
        BuildResult result = run('check', '--build-cache', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.task(":codenarcMain").outcome == TaskOutcome.SUCCESS
        result.task(":checkstyleMain").outcome == TaskOutcome.SUCCESS
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.task(":pmdMain").outcome == TaskOutcome.SUCCESS
        result.output.contains('CodeNarc violations were found in 2 files')
        result.output.contains('Checkstyle rule violations were found in 2 files')
        result.output.contains('SpotBugs violations were found in 2 files')
        result.output.contains('PMD rule violations were found in 2 files')


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('check', '--build-cache', '--configuration-cache', '--configuration-cache-problems=warn')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":codenarcMain").outcome == TaskOutcome.UP_TO_DATE
        result.task(":checkstyleMain").outcome == TaskOutcome.UP_TO_DATE
        result.task(":spotbugsMain").outcome == TaskOutcome.UP_TO_DATE
        result.task(":pmdMain").outcome == TaskOutcome.UP_TO_DATE
        result.output.contains('CodeNarc violations were found in 2 files')
        result.output.contains('Checkstyle rule violations were found in 2 files')
        result.output.contains('SpotBugs violations were found in 2 files')
        result.output.contains('PMD rule violations were found in 2 files')
    }
}
