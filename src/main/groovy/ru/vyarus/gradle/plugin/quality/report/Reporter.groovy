package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc

/**
 * Reporter is responsible for printing violations into console and possible html report generation.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @param <T>    task descriptor type
 */
@CompileStatic
interface Reporter<T extends TaskDesc> {

    /**
     * New line symbol.
     */
    static final String NL = String.format('%n')

    /**
     * Called after quality tool task to report violations.
     *
     * @param task quality task descriptor
     * @param sourceSet task source set (main or test)
     */
    void report(T task, String sourceSet)
}
