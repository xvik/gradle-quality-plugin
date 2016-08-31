package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Responsible for html report generation for plugins not supporting that directly (findbugs).
 *
 * @author Vyacheslav Rusakov
 * @since 01.09.2016
 */
@CompileStatic
interface HtmlReportGenerator {

    /**
     * Called after quality tool task to generate html report.
     *
     * @param project project instance
     * @param type task type (main or test)
     */
    void generateHtmlReport(Project project, String type)
}
