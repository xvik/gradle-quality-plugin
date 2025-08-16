package ru.vyarus.gradle.plugin.quality.tool.spotbugs.report

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc

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
