package ru.vyarus.gradle.plugin.quality.report.model

import groovy.transform.CompileStatic

/**
 * Task information, required for building reports (because task itself can't be used).
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic
class TaskDesc implements Serializable {

    @Serial
    private static final long serialVersionUID = 1

    // task path
    String path
    // task name
    String name
    // tool name
    String tool
    // source set name
    String sourceSet
    // report execution marker
    boolean executed

    // current project path
    String projectPath
    // path to xml report
    String xmlReportPath
    // source directories (from task's source set)
    List<String> sourceDirs
}
