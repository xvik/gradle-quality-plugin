package ru.vyarus.gradle.plugin.quality.spotbugs;

import com.github.spotbugs.snom.SpotBugsBasePlugin;
import com.github.spotbugs.snom.internal.SpotBugsTaskFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import ru.vyarus.gradle.plugin.quality.QualityExtension;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Plugin used as a replacement to {@link com.github.spotbugs.snom.SpotBugsPlugin} because original plugin
 * assigns ALL spotbugs tasks to check which is wrong! Removing existing task dependencies is error prone and
 * it's better to just create new plugin.
 * <p>
 * NOTE: main spotbugs functionality is inside spotbugs-main plugin and this plugin IS USED. Moreover,
 * if this base plugin is already applied, then custom plugin would not be applied to avoid duplicate spotbugs
 * activation.
 * <p>
 * It is JAVA class because groovy class is impossible to compile, thanks to android plugin dependency.
 *
 * @author Vyacheslav Rusakov
 * @since 09.05.2020
 */
public class CustomSpotBugsPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        if (project.getPlugins().hasPlugin(SpotBugsBasePlugin.class)) {
            // assume plugin registered manually
            return;
        }

        project.getPlugins().apply(SpotBugsBasePlugin.class);
        final QualityExtension quality = project.getExtensions().findByType(QualityExtension.class);
        final List<String> requiredTasks = quality.getSourceSets().stream()
                .map(it -> it.getTaskName("spotbugs", null))
                .collect(Collectors.toList());

        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME)
                .configure(task -> task.dependsOn(requiredTasks));

        new SpotBugsTaskFactory().generate(project);
    }
}
