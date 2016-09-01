package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Reporter is responsible for printing violations into console and possible html report generation.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 */
@CompileStatic
interface Reporter {

    /**
     * New line symbol wrapped with invisible whitespace to avoid truncation.
     */
    String NL = String.format('\u200B%n\u200B')

    /**
     * Called after quality tool task to report violations.
     *
     * @param project project instance
     * @param type task type (main or test)
     */
    void report(Project project, String type)
}
