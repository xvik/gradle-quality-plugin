package ru.vyarus.gradle.plugin.quality.tool.javac

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import ru.vyarus.gradle.plugin.quality.tool.ProjectSources
import ru.vyarus.gradle.plugin.quality.tool.QualityTool
import ru.vyarus.gradle.plugin.quality.tool.ToolContext

/**
 * Java lint options support.
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings('GetterMethodCouldBeProperty')
class JavacTool implements QualityTool {
    static final String NAME = 'javac'

    @Override
    String getToolName() {
        return NAME
    }

    @Override
    List<ProjectSources> getAutoEnableForSources() {
        return []
    }

    @Override
    List<String> getConfigs() {
        return []
    }

    @Override
    void configure(ToolContext context, boolean register) {
        List<String> extraOptions = context.extension.lintOptions.get()
        if (!extraOptions) {
            return
        }
        context.project.tasks.withType(JavaCompile).configureEach { JavaCompile t ->
            t.options.compilerArgumentProviders.add({
                extraOptions.collect { "-Xlint:$it" as String }
            } as CommandLineArgumentProvider)
        }
    }
}
