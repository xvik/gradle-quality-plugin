package ru.vyarus.gradle.plugin.quality.report.model.factory

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Task
import ru.vyarus.gradle.plugin.quality.report.model.SpotbugsTaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc

/**
 * Model factory for spotbugs reporter.
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic(TypeCheckingMode.SKIP)
class SpotbugsModelFactory extends ModelFactory {

    @Override
    TaskDesc buildDesc(Task task, String tool) {
        SpotbugsTaskDesc desc = new SpotbugsTaskDesc(tool: tool)
        desc.reportsDir = task.project.extensions.spotbugs.reportsDir.get()
        fillDefaultProps(task, desc)
        return desc
    }
}
