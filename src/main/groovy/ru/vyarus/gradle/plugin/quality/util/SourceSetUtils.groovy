package ru.vyarus.gradle.plugin.quality.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet

/**
 * Source sets-related utils. Used as plugin use strings for source sets configuration.
 *
 * @author Vyacheslav Rusakov
 * @since 26.08.2025
 */
@CompileStatic
class SourceSetUtils {

    /**
     * Converts string configuration of source sets into real source set objects.
     *
     * @param project project
     * @param sets source set names
     * @return collection of matched source sets
     */
    static List<SourceSet> getSourceSets(Project project, Set<String> sets) {
        project.extensions.getByType(JavaPluginExtension).sourceSets.matching { SourceSet set ->
            sets.contains(set.name)
        }.asList()
    }
}
