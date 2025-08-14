package ru.vyarus.gradle.plugin.quality.report.model

import groovy.transform.CompileStatic

/**
 * Spotbugs task info, required for reporter.
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic
class SpotbugsTaskDesc extends TaskDesc {

    @Serial
    private static final long serialVersionUID = 1

    String reportsDir
}
