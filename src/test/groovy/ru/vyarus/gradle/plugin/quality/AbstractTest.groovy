package ru.vyarus.gradle.plugin.quality

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
abstract class AbstractTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    Project project(Closure<Project> config = null) {
        projectBuilder(config).build()
    }

    ExtendedProjectBuilder projectBuilder(Closure<Project> root = null) {
        new ExtendedProjectBuilder().root(testProjectDir.root, root)
    }

    File file(String path) {
        new File(testProjectDir.root, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target << getClass().getResourceAsStream(source).text
    }

    static class ExtendedProjectBuilder {
        Project root

        ExtendedProjectBuilder root(File dir, Closure<Project> config = null) {
            assert root == null, "Root project already declared"
            Project project = ProjectBuilder.builder()
                    .withProjectDir(dir).build()
            if (config) {
                project.configure(project, config)
            }
            root = project
            return this
        }

        /**
         * Direct child of parent project
         *
         * @param name child project name
         * @param config optional configuration closure
         * @return builder
         */
        ExtendedProjectBuilder child(String name, Closure<Project> config = null) {
            return childOf(null, name, config)
        }

        /**
         * Direct child of any registered child project
         *
         * @param projectRef name of required parent module (gradle project reference format: `:some:deep:module`)
         * @param name child project name
         * @param config optional configuration closure
         * @return builder
         */
        ExtendedProjectBuilder childOf(String projectRef, String name, Closure<Project> config = null) {
            assert root != null, "Root project not declared"
            Project parent = projectRef == null ? root : root.project(projectRef)
            File folder = parent.file(name)
            if (!folder.exists()) {
                folder.mkdir()
            }
            Project project = ProjectBuilder.builder()
                    .withName(name)
                    .withProjectDir(folder)
                    .withParent(parent)
                    .build()
            if (config) {
                project.configure(project, config)
            }
            return this
        }

        /**
         * Evaluate configuration.
         *
         * @return root project
         */
        Project build() {
            if (root.subprojects) {
                root.evaluationDependsOnChildren()
            }
            root.evaluate()
            return root
        }
    }
}
