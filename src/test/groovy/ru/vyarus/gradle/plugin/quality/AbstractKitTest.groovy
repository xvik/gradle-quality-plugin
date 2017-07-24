package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
abstract class AbstractKitTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def build(String file) {
        buildFile << (file + """
            // reduce memory usage for travis
            afterEvaluate {
                tasks.withType(JavaForkOptions) {
                    minHeapSize = '128m'
                }
            }
""")
    }

    File file(String path) {
        new File(testProjectDir.root, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target << getClass().getResourceAsStream(source).text
    }

    def debug() {
        file('gradle.properties') << "org.gradle.jvmargs=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
    }

    String projectName() {
        return testProjectDir.root.getName()
    }

    GradleRunner gradle(String... commands) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments((commands + ['--stacktrace']) as String[])
                .withPluginClasspath()
                .forwardOutput()
    }

    BuildResult run(String... commands) {
        return gradle(commands).build()
    }

    BuildResult runFailed(String... commands) {
        return gradle(commands).buildAndFail()
    }

    BuildResult runVer(String gradleVersion, String... commands) {
        return gradle(commands).withGradleVersion(gradleVersion).build()
    }

    BuildResult runFailedVer(String gradleVersion, String... commands) {
        return gradle(commands).withGradleVersion(gradleVersion).buildAndFail()
    }
}
