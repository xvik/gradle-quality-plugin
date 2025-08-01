package ru.vyarus.gradle.plugin.quality.util

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider

/**
 * Cpd configuration helper utils.
 *
 * @author Vyacheslav Rusakov
 * @since 07.11.2019
 */
@CompileStatic
class CpdUtils {

    /**
     * In case of multi-module projects cpd most likely will be applied in the root project, but quality plugin is
     * applied on subproject level. In such case we can't attach cpd task into checkMain task and can't apply
     * enabled state control (because it's not normal when submodule configures root project).
     * Still, it is ok to configure pmd version to be in sync and remove restricted source.
     * <p>
     * Checking all parent modules in case if cpd declared in some middle parent module.
     *
     * @param project project where quality plugin declared
     * @param configuration cpd plugin configuration closure (accept cpd declaration project and plugin instance)
     */
    static void findAndConfigurePlugin(Project project, Closure configuration) {
        // for single module projects this will execute once
        // for multi-module project - entire parents chain will be checked
        Project current = project
        while (current != null) {
            current.plugins.withId('de.aaschmid.cpd') {
                configuration.call(current, it)
            }
            current = current.parent
        }
    }

    /**
     * @param project cpd plugin declaration project
     * @return cpdCheck task provider
     */
    static TaskProvider<SourceTask> findCpdTask(Project project) {
        return project.tasks.named('cpdCheck') as TaskProvider<SourceTask>
    }

    /**
     * By default, cpd lookup all project source sets. To unify behaviour with other quality plugins,
     * source sets, not configured for quality checks, should be also excluded from cpd check.
     * <p>
     * In case of multi-module project, most likley cpd will be configred in root project and quality plugin
     * for all subprojects. So by default cpdCheck task (in root project) will scan all sourceSets in submodules.
     * Each sub project, containing qulaity plugin will exclude not needed sources from root task and so
     * for each sub project everything would be correct (and so correct overall).
     *
     * @param project quality plugin declaration project
     * @param cpdCheck cpdCheck task (probably from parent project)
     * @param qualitySets configured source sets for quality tasks
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static void unifyCpdSources(Project project, TaskProvider<SourceTask> cpdCheck, Collection<SourceSet> qualitySets) {
        project.plugins.withType(JavaBasePlugin) {
            project.sourceSets.all { sourceSet ->
                if (!qualitySets.contains(sourceSet)) {
                    project.logger.info('[plugin:quality] Removing {}/{} source set directories from CPD: {}',
                            project.name, sourceSet.name, sourceSet.allSource.srcDirs)
                    // by default task configure all source set contents, so just remove not needed sets
                    // note that it is important to use allSource and not allJava because otherwise only java
                    // files will be excluded and other files (e.g. groovy) will remain causing troubles
                    cpdCheck.configure {
                        it.source = it.source - sourceSet.allSource
                    }
                }
            }
        }
    }

    /**
     * In case of multi-module projects, many sub modules may be declared with quality plugin. Some configuration
     * must be applied only once for cpd plugin (assuming declared in root module).
     *
     * @param project project where cpd plugin is declared
     * @return true if cpd plugin was not configured yet, false otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static boolean isCpdAlreadyConfigured(Project project) {
        if (project.findProperty('cpdReportConfigured')) {
            return true
        }
        project.ext.cpdReportConfigured = true
        return false
    }
}
