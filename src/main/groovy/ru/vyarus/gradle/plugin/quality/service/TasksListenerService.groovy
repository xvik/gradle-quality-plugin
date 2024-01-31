package ru.vyarus.gradle.plugin.quality.service

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import ru.vyarus.gradle.plugin.quality.ConfigLoader
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.CheckstyleReporter
import ru.vyarus.gradle.plugin.quality.report.CodeNarcReporter
import ru.vyarus.gradle.plugin.quality.report.CpdReporter
import ru.vyarus.gradle.plugin.quality.report.HtmlReportGenerator
import ru.vyarus.gradle.plugin.quality.report.PmdReporter
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.SpotbugsReporter
import ru.vyarus.gradle.plugin.quality.util.DurationFormatter

import java.util.concurrent.ConcurrentHashMap

/**
 * Gradle build service, responsible for printing console reports (and manual html reports generation, where required).
 * <p>
 * Before, {@code project.gradle.taskGraph.afterTask} was used, which is now deprecated (due to build cache
 * incompatibility). Instead, plugin now use {@code doLast} callback and {@link OperationCompletionListener}
 * to track failed tasks (where {@code doLast} might not be called). In most cases, output would be the same as
 * before, but, in some cases, console report may appear NOT directly after the task (because
 * {@link OperationCompletionListener} DOES NOT guarantee it).
 * <p>
 * Also, in some cases, reporters would be called now not from gradle-managed thread and so can't resolve
 * project configurations or anything else, not allowed by non-gradle thread. To overcome this, reporter is now
 * notified with registered tasks and can resolve all required data at that moment.
 *
 * @author Vyacheslav Rusakov
 * @since 30.01.2024
 */
@CompileStatic
abstract class TasksListenerService implements BuildService<BuildServiceParameters.None>,
        OperationCompletionListener {

    private final Map<String, Reporter> reporters = [:]
    private final Map<String, TaskDesc> targetTasks = new ConcurrentHashMap<>()
    private QualityExtension extension

    /**
     * Initialize service with config loader and extensions configuration instances.
     *
     * @param loader config loader
     * @param extension extensions config
     */
    void init(ConfigLoader loader, QualityExtension extension) {
        this.extension = extension

        reporters['checkstyle'] = new CheckstyleReporter()
        reporters['codenarc'] = new CodeNarcReporter()
        reporters['cpd'] = new CpdReporter(loader)
        reporters['pmd'] = new PmdReporter()
        reporters['spotbugs'] = new SpotbugsReporter()
    }

    /**
     * Each quality task is registered directly to properly apply reporting.
     * <p>
     * {@code useFullTaskName} is required for CPD task only, which use single task for all source sets.
     *
     * @param task task instance
     * @param type task type (to reference reporter and resolve source set)
     * @param useFullTaskName (true to use task name for a report file name instead of source set name)
     */
    void register(Task task, String type, boolean useFullTaskName) {
        targetTasks.put(task.path, new TaskDesc(task: task, type: type, useFullTaskName: useFullTaskName))
        // this is the only way to print report DIRECTLY AFTER the task output (like it was before)
        // but, if task execution fails, doLast block would not be called at all, and in this case build service
        // task listener would log it
        task.doLast {
            execute(task.path)
        }
        // the only way to get access for configurations or other sensitive staff for reporter
        // (executed in non-gradle thread and so without ability to access configurations)
        reporters[type].init(task)
    }

    /**
     * Called on task finish.
     * <p>
     * Limitation: call EXACTLY after task execution is NOT guaranteed by gradle and so output may appear not directly
     * after task. That's why {@code doLast} is used for exact output position, and only when it doesn't called
     * build service listener is used as a fallback.
     *
     * @param finishEvent finish event
     */
    @Override
    void onFinish(FinishEvent finishEvent) {
        if (finishEvent instanceof TaskFinishEvent) {
            TaskFinishEvent taskEvent = (TaskFinishEvent) finishEvent
            execute(taskEvent.descriptor.taskPath)
        }
    }

    // synchronized to at least not mix different reports (still report might be shown not near the task)
    synchronized void execute(String taskPath) {
        TaskDesc desc = targetTasks.get(taskPath)
        if (desc != null && !desc.executed) {
            desc.executed = true
            reportTask(desc.task, desc.type, reporters[desc.type], desc.useFullTaskName)
        }
    }

    private void reportTask(Task task, String type, Reporter reporter, boolean useFullTaskName) {
        boolean generatesHtmlReport = extension.htmlReports && HtmlReportGenerator.isAssignableFrom(reporter.class)
        if (!extension.consoleReporting && !generatesHtmlReport) {
            // nothing to do at all
            return
        }

        String taskType = useFullTaskName ? task.name : task.name[type.length()..-1].toLowerCase()
        if (generatesHtmlReport) {
            (reporter as HtmlReportGenerator).generateHtmlReport(task, taskType)
        }
        if (extension.consoleReporting) {
            long start = System.currentTimeMillis()
            reporter.report(task, taskType)
            String duration = DurationFormatter.format(System.currentTimeMillis() - start)
            task.project.logger.info("[plugin:quality] $type reporting executed in $duration")
        }
    }

    static class TaskDesc {
        Task task
        String type
        boolean useFullTaskName
        boolean executed
    }
}
