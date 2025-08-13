package ru.vyarus.gradle.plugin.quality.report.model

import groovy.transform.CompileStatic

/**
 * Cpd task info, required for reporter.
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic
class CpdTaskDesc extends TaskDesc {

    String reportsDir
    String language
    String rootProjectPath
    // cpd does not rely on single source set and so it needs to know all sources (in all modules)
    Map<String, List<String>> sources
}
