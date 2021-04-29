package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import org.gradle.api.Task

/**
 * Reporter is responsible for printing violations into console and possible html report generation.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @param <T>  task type
 */
@CompileStatic
interface Reporter<T extends Task> {

    /**
     * New line symbol.
     */
    String NL = String.format('%n')

    /**
     * Called after quality tool task to report violations.
     *
     * @param task quality task with violations
     * @param type task type (main or test)
     */
    void report(T task, String type)
}
