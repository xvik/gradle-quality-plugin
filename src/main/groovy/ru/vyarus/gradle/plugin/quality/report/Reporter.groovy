package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import org.gradle.api.Task

/**
 * Reporter is responsible for printing violations into console and possible html report generation.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @param <T>   task type
 */
@CompileStatic
interface Reporter<T extends Task> {

    /**
     * New line symbol.
     */
    static final String NL = String.format('%n')

    /**
     * In some cases, reporter would be executed outside of gradle threads and so any access to
     * project configurations would not be allowed ("was resolved from a thread not managed by Gradle" error).
     * To avoid this error, all initialization must be performed before actual processing.
     * <p>
     * IMPORTANT: all quality tasks are lazy and this method would be called only when quality task configuration
     * was triggered (ideally, before actual task execution).
     * <p>
     * Method would be called for EACH quality task.
     *
     * @param task quality task
     */
    @SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
    default void init(T task) { }

    /**
     * Called after quality tool task to report violations.
     *
     * @param task quality task with violations
     * @param type task type (main or test)
     */
    void report(T task, String type)
}
