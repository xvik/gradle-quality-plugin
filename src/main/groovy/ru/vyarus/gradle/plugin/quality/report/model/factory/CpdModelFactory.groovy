package ru.vyarus.gradle.plugin.quality.report.model.factory

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import ru.vyarus.gradle.plugin.quality.report.model.CpdTaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc

/**
 * Model factory for cpd reporter.
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
@CompileStatic(TypeCheckingMode.SKIP)
class CpdModelFactory extends ModelFactory {

    @Override
    TaskDesc buildDesc(Task task, String tool) {
        CpdTaskDesc desc = new CpdTaskDesc(tool: tool)
        desc.with {
            reportsDir = task.project.extensions.cpd.reportsDir
            language = task.language
            // cpd use different naming convention
            sourceSet = task.name
            // no relation to exact source set - more complex structure is required (see "sources")
            sourceDirs = []
            rootProjectPath = task.project.rootDir.canonicalPath
            // need to know all sources in current project and sub modules
            sources = getSources(task.project)
        }
        fillDefaultProps(task, desc)
        return desc
    }

    private Map<String, List<String>> getSources(Project project) {
        Map<String, List<String>> res = [:]
        List<String> sources = collectProjectSources(project)
        if (!sources.empty) {
            res.put('', sources)
        }
        collectSubModules(project, res)
        return res
    }

    private void collectSubModules(Project project, Map<String, List<String>> res) {
        for (Project sub : project.subprojects) {
            List<String> sources = collectProjectSources(sub)
            if (!sources.empty) {
                // apply module path to easily identify class location
                res.put(sub.path, sources)
            }
            collectSubModules(sub, res)
        }
    }

    private List<String> collectProjectSources(Project project) {
        List<String> sources = []
        if (project.plugins.findPlugin(JavaBasePlugin) != null) {
            for (SourceSet set : project.sourceSets) {
                sources.addAll(set.allJava.srcDirs*.canonicalPath)
            }
        }
        return sources
    }
}
