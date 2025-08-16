package ru.vyarus.gradle.plugin.quality.report.model

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Task
import org.gradle.api.reporting.Report
import org.gradle.api.tasks.SourceSet

/**
 * Copies all task information for reporter (because task itself can't be preserved).
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic
class TaskDescFactory {

    /**
     * Build task descriptor for reporter.
     *
     * @param task task instance
     * @param tool tool name
     * @return task descriptor
     */
    TaskDesc buildDesc(Task task, String tool) {
        TaskDesc res = new TaskDesc(tool: tool)
        fillDefaultProps(task, res)
        return res
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void fillDefaultProps(Task task, TaskDesc desc) {
        desc.name = task.name
        desc.path = task.path
        desc.projectPath = task.project.projectDir.absoluteFile.canonicalPath
        if (desc.sourceSet == null) {
            desc.sourceSet = task.name[desc.tool.length()..-1].toLowerCase()
        }
        desc.xmlReportPath = ((task.reports.findByName('xml')) as Report).outputLocation.get()
                .asFile.absoluteFile.canonicalPath
        if (desc.sourceDirs == null) {
            desc.sourceDirs = (task.project.sourceSets[desc.sourceSet] as SourceSet)
                    .allJava.srcDirs*.canonicalPath
        }
    }
}
