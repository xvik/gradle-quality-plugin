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

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    List<File> pluginClasspath

    def setup() {
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }
        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def build(String file) {
        buildFile << file
    }

    File file(String path) {
        new File(testProjectDir.root, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target << getClass().getResourceAsStream(source).text
    }

    String projectName() {
        return testProjectDir.root.getName()
    }

    GradleRunner gradle(String... commands) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments((commands + ['--stacktrace']) as String[])
                .withPluginClasspath(pluginClasspath)
    }

    BuildResult run(String... commands) {
        BuildResult result = gradle(commands).build()
        // for debug
        println result.getStandardOutput()
        println result.getStandardError()
        return result
    }
}
