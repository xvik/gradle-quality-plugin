package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc

/**
 * Reporter is responsible for printing violations into console and possible html report generation.
 * <p>
 * Due to configuration cache, reporter can't perform any project-related actions and so all required data
 * must be pre-loaded under configuration time. For this purpose, the plugin maintains a map of extra data
 * (filled by quality tools) and used during reporter creation. This map is stored as listener service parameter
 * and so being cached (and available under configuration cache).
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
