package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * @author Vyacheslav Rusakov
 * @since 28.08.2018
 */
class BuildCacheSupportKitTest extends AbstractKitTest {

    @Rule
    final TemporaryFolder cacheDir = new TemporaryFolder()
    @Rule
    final TemporaryFolder relocatedDir = new TemporaryFolder()

    def "Check java and groovy checks"() {
        setup:
        // build cache will survive within test only!!
        // spotbugs task cache key depends on project name!!!
        file("settings.gradle") << """  
            rootProject.name='my-project'
            buildCache {
                local(DirectoryBuildCache) {
                    directory = new File('${cacheDir.root.canonicalPath.replace('\\', '\\\\')}')
                }
            }
"""
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                jcenter() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/main/java/sample/Sample2.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample2.java')
        fileFromClasspath('src/main/groovy/sample/GSample.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample.groovy')
        fileFromClasspath('src/main/groovy/sample/GSample2.groovy', '/ru/vyarus/gradle/plugin/quality/groovy/sample/GSample2.groovy')

        // create complete project copy
        new AntBuilder().copy(todir: relocatedDir.root) {
            fileset(dir: testProjectDir.root)
        }

        when: "run check task with both sources with build cache"
        BuildResult result = run('check', '--build-cache')

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


        when: "run other project from cache"
        result = gradle(relocatedDir.root, 'check', '--build-cache').build()

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":codenarcMain").outcome == TaskOutcome.FROM_CACHE
        result.task(":checkstyleMain").outcome == TaskOutcome.FROM_CACHE
        result.task(":spotbugsMain").outcome == TaskOutcome.FROM_CACHE
        result.task(":pmdMain").outcome == TaskOutcome.FROM_CACHE
        result.output.contains('CodeNarc violations were found in 2 files')
        result.output.contains('Checkstyle rule violations were found in 2 files')
        result.output.contains('SpotBugs violations were found in 2 files')
        result.output.contains('PMD rule violations were found in 2 files')
    }


    // CI case when project checked out into different dirs
    def "Check relocation"() {
        setup:
        // build cache will survive within test only!!
        file("settings.gradle") << """
            buildCache {
                local(DirectoryBuildCache) {
                    directory = new File('${cacheDir.root.canonicalPath.replace('\\', '\\\\')}')
                }
            }
"""
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            quality {
                strict false
            }

            repositories {
                jcenter() //required for testKit run
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
        BuildResult result = run('check', '--build-cache')

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


        when: "run check from cache"
        result = run('clean', 'check', '--build-cache')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.UP_TO_DATE
        result.task(":codenarcMain").outcome == TaskOutcome.FROM_CACHE
        result.task(":checkstyleMain").outcome == TaskOutcome.FROM_CACHE
        result.task(":spotbugsMain").outcome == TaskOutcome.FROM_CACHE
        result.task(":pmdMain").outcome == TaskOutcome.FROM_CACHE
        result.output.contains('CodeNarc violations were found in 2 files')
        result.output.contains('Checkstyle rule violations were found in 2 files')
        result.output.contains('SpotBugs violations were found in 2 files')
        result.output.contains('PMD rule violations were found in 2 files')
    }
}
