package ru.vyarus.gradle.plugin.quality.tool

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.QualityPlugin
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.service.TasksListenerService
import ru.vyarus.gradle.plugin.quality.task.CopyConfigsTask

import java.util.concurrent.Callable

/**
 * Tool registration context. Provide utility methods for tools configuration.
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 */
@CompileStatic
class ToolContext {

    final Project project
    final QualityExtension extension
    final Provider<ConfigsService> configs
    private final Provider<TasksListenerService> listenerService
    final List<ProjectSources> languages
    final TaskProvider<CopyConfigsTask> configsTask

    // used to collect data during configuration (stored later in listener service)
    final List<TaskDesc> qualityTasks
    final Map<String, Object> reportersData

    @SuppressWarnings('ParameterCount')
    ToolContext(Project project, QualityExtension extension,
                Provider<ConfigsService> configs,
                Provider<TasksListenerService> listenerService,
                List<ProjectSources> languages,
                List<TaskDesc> qualityTasks,
                Map<String, Object> reportersData,
                TaskProvider<CopyConfigsTask> configsTask) {
        this.project = project
        this.extension = extension
        this.configs = configs
        this.listenerService = listenerService
        this.languages = languages
        // important to pass instances here, because they are already configured in listener service, which would
        // cache values for configuration cache
        this.qualityTasks = qualityTasks
        this.reportersData = reportersData
        this.configsTask = configsTask
    }

    /**
     * Plugins may be registered manually and in this case plugin will also be configured, but only
     * when plugin support not disabled by quality configuration. If plugin not registered and
     * sources auto detection allow registration - it will be registered and then configured.
     *
     * @param plugin plugin class
     * @param register true to automatically register plugin
     * @param action plugin configuration closure
     */
    void withPlugin(Class plugin, boolean register, Closure action) {
        if (register) {
            // register plugin automatically
            project.plugins.apply(plugin)
            project.logger.info("[plugin:quality] Plugin $plugin.simpleName auto-applied in project $project.name ")
        }
        project.plugins.withType(plugin).configureEach {
            action.call()
        }
    }

    /**
     * Store quality tool task descriptor so custom reported could be executed after tool task execution.
     *
     * @param task task instance
     * @param taskDesc created task descriptor
     */
    void registerTaskForReport(Task task, TaskDesc taskDesc) {
        qualityTasks.add(taskDesc)

        // extra var required for proper conf.cache
        Provider<TasksListenerService> listener = listenerService
        task.doLast {
            // can't use task desc directly because under conf cache it would be a different instance then stored
            // in service
            listener.get().execute(taskDesc.path)
        }
    }

    /**
     * If quality tasks are disabled in configuration ({@code quality.enabled = false})
     * then disabling tasks. Anyway, task must not be disabled if called directly
     * or through grouping quality task (e.g. checkQualityMain).
     * NOTE: if, for example, checkQualityMain is called after some other task
     * (e.g. someTask.dependsOn checkQualityMain) then quality tasks will be disabled!
     * Motivation is: plugins are disabled for a reason and could be enabled only when called
     * directly (because obviously user wants quality task(s) to run).
     *
     * @param project project instance
     * @param extension extension instance
     * @param task quality plugin task class
     */
    void applyEnabledState(Class task) {
        if (!extension.enabled.get()) {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                project.tasks.withType(task).configureEach { Task t ->
                    // last task on stack obtained only on actual task usage
                    List<Task> tasks = graph.allTasks
                    Task called = tasks != null && !tasks.empty ? tasks.last() : null
                    // enable task only if it's called directly or through grouping task
                    t.enabled = called != null && (called == t || called.name.startsWith(QualityPlugin.QUALITY_TASK))
                }
            }
        }
    }

    /**
     * Quality plugins register tasks for each source set. Declared affected source sets
     * only affects which tasks will 'check' depend on.
     * Grouping tasks allow to call quality tasks, not included to 'check'.
     *
     * @param tool tool name (usually used as task base name)
     */
    void groupQualityTasks(String tool) {
        // each quality plugin generate separate tasks for each source set
        // assign plugin tasks to source set grouping quality task
        project.extensions.getByType(JavaPluginExtension).sourceSets.each {
            TaskProvider pluginTask = project.tasks.named(it.getTaskName(tool, null))
            if (pluginTask) {
                project.tasks.named(it.getTaskName(QualityPlugin.QUALITY_TASK, null))
                        .configure { it.dependsOn pluginTask }
            }
        }
    }

    /**
     * Store custom data for tool reporter. Provided action would be called just once!
     * <p>
     * Returned data must be serializable because it would be stored under listener service properties.
     *
     * @param tool tool name
     * @param data custom data provider
     */
    void storeReporterData(String tool, Callable<Object> data) {
        synchronized (reportersData) {
            if (!reportersData.containsKey(tool)) {
                reportersData.put(tool, data.call())
            }
        }
    }

    /**
     * Applies exclude path patterns to quality tasks.
     * Note: this does not apply to animalsniffer. For spotbugs this appliance is useless, see custom support above.
     * <p>
     * The method is static because it is referenced from runtime.
     *
     * @param task quality task
     * @param excludeSources sources to exclude
     * @param exclude exclude patterns
     */
    void applyExcludes(SourceTask task, FileCollection excludeSources, List<String> exclude) {
        if (excludeSources) {
            // directly excluded sources
            task.source = task.source - excludeSources
        }
        if (exclude) {
            // exclude by patterns (relative to source roots)
            task.exclude exclude
        }
    }

    /**
     * Decide what config file to use: either user provided from custom user directory or copied tmp config
     * from plugin jar. Note that config would be copied with a special task  - here only correct target selection
     * is required.
     *
     * @param path file path
     * @return file
     */
    File resolveConfigFile(String path) {
        return resolveRegularConfigFile(path).asFile
    }

    /**
     * @param path file path
     * @return path to config file in user directory (if user manually provide it)
     */
    File userConfigFile(String path) {
        return userRegularConfigFile(path).asFile
    }

    /**
     * Required only for cases when both user-declared config and config in tmp folder exists (meaning user config
     * was copied and modified).
     *
     * @param path file path
     * @return path to config file in user directory (if user manually provide it)
     */
    File tempConfigFile(String path) {
        return tempRegularConfigFile(path).asFile
    }

    /**
     * @param path file path
     * @return regular file (required by some plugins like spotbugs)
     */
    RegularFile resolveRegularConfigFile(String path) {
        return configs.get().resolveConfigFile(path)
    }

    /**
     * @param path file path
     * @return regular file (required by some plugins like spotbugs)
     */
    RegularFile userRegularConfigFile(String path) {
        return configs.get().userConfigFile(path)
    }

    /**
     * Required only for cases when both user-declared config and config in tmp folder exists (meaning user config
     * was copied and modified).
     *
     * @param path file path
     * @return config file located in temporary directory
     */
    RegularFile tempRegularConfigFile(String path) {
        return configs.get().tempConfigFile(path)
    }
}
