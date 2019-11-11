package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import org.gradle.api.Task

/**
 * Responsible for html report generation for plugins not supporting that directly.
 *
 * @author Vyacheslav Rusakov
 * @since 01.09.2016
 * @param < T >  target task type
 */
@CompileStatic
interface HtmlReportGenerator<T extends Task> {

    /**
     * Called after quality tool task to generate html report.
     *
     * @param task quality task to generate report for
     * @param type task type (main or test)
     */
    void generateHtmlReport(T task, String type)
}
