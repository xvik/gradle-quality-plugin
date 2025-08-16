package ru.vyarus.gradle.plugin.quality.service

import groovy.transform.CompileStatic
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.vyarus.gradle.plugin.quality.QualityPlugin
import ru.vyarus.gradle.plugin.quality.report.HtmlReportGenerator
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.util.DurationFormatter

/**
 * Gradle build service, responsible for printing console reports (and manual html reports generation, where required).
 * Service started per project to be able to properly cache configuration state in parameters.
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
@SuppressWarnings(['AbstractClassWithoutAbstractMethod', 'LoggerWithWrongModifiers',
        'AbstractClassWithPublicConstructor'])
abstract class TasksListenerService implements BuildService<Params>, OperationCompletionListener {
    private final Logger logger = LoggerFactory.getLogger(TasksListenerService)

    private final Map<String, Reporter> reporters = [:]

    TasksListenerService() {
        // reporters must be initialized independently because under configuration cache plugin would not be created
        // at all, but reporters still need to be initialized
        QualityPlugin.TOOLS.each {
            reporters[it.toolName] = it.createReporter(
                    parameters.reportersData.get()[it.toolName], parameters.configsService)
        }
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
    @SuppressWarnings('Instanceof')
    void onFinish(FinishEvent finishEvent) {
        if (finishEvent instanceof TaskFinishEvent) {
            TaskFinishEvent taskEvent = (TaskFinishEvent) finishEvent
            execute(taskEvent.descriptor.taskPath)
        }
    }

    // synchronized to at least not mix different reports (still report might be shown not near the task)
    @SuppressWarnings('SynchronizedMethod')
    synchronized void execute(String taskPath) {
        TaskDesc desc = parameters.qualityTasks.get().find { it.path == taskPath }
        if (desc != null && !desc.executed) {
            desc.executed = true
            reportTask(desc, reporters[desc.tool])
        }
    }

    private void reportTask(TaskDesc task, Reporter reporter) {
        boolean generatesHtmlReport = parameters.htmlReports.get()
                && HtmlReportGenerator.isAssignableFrom(reporter.class)
        if (!parameters.consoleReporting.get() && !generatesHtmlReport) {
            // nothing to do at all
            return
        }

        if (generatesHtmlReport) {
            (reporter as HtmlReportGenerator).generateHtmlReport(task, task.sourceSet)
        }
        if (parameters.consoleReporting.get()) {
            long start = System.currentTimeMillis()
            reporter.report(task, task.sourceSet)
            String duration = DurationFormatter.format(System.currentTimeMillis() - start)
            logger.info("[plugin:quality] $task.tool reporting executed in $duration")
        }
    }

    // it is important to NOT use service in configuration phase: otherwise paramteres
    // would initialize early and would not contain all required state (and so would not
    // work properly under conf.cache)
    interface Params extends BuildServiceParameters {
        Property<ConfigsService> getConfigsService()
        Property<Boolean> getHtmlReports()
        Property<Boolean> getConsoleReporting()
        // parameter used for quality tasks state aggregation (and its persistence)
        ListProperty<TaskDesc> getQualityTasks()
        // additional data, resolved during configuration phase, required for reports
        MapProperty<String, Object> getReportersData()
    }
}
