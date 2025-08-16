package ru.vyarus.gradle.plugin.quality.tool.spotbugs.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Task
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory

/**
 * Model factory for spotbugs reporter.
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic(TypeCheckingMode.SKIP)
class SpotbugsTaskDescFactory extends TaskDescFactory {

    @Override
    TaskDesc buildDesc(Task task, String tool) {
        SpotbugsTaskDesc desc = new SpotbugsTaskDesc(tool: tool)
        desc.reportsDir = task.project.extensions.spotbugs.reportsDir.get()
        fillDefaultProps(task, desc)
        return desc
    }
}
