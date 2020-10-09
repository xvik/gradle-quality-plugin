package ru.vyarus.gradle.plugin.quality.util

import com.github.spotbugs.snom.SpotBugsTask
import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFile
import ru.vyarus.gradle.plugin.quality.ConfigLoader
import ru.vyarus.gradle.plugin.quality.QualityExtension

import java.util.concurrent.Callable

/**
 * Previously, exclusion config was calculated in doFirst hook of spotbugs task (just before task execution),
 * but the new spotbugs plugin use gradle properties and now value can't be set in doFirst callback!
 * Have to use property provider instead. This provider is called multiple times and in different time
 * (before all tasks execution).
 * <p>
 * One side effect of this shift caused by clean task which removes temporary copied configs, so we have now to
 * always check for already generated config existence and re-create it if removed. Overall, callback tries to
 * avoid redundant actions as much as possible.
 * <p>
 * Temporary exclusion file is generated ahead of time (when it can't be checked for sure) because
 * it is called too early (so rely only on specified exclusions and not on how they apply).
 * In warse case temporary exclusion would be a copy of default exclusion file (if no actual exclusions
 * performed).
 *
 * @author Vyacheslav Rusakov
 * @since 12.05.2020
 */
@CompileStatic
class SpotbugsExclusionConfigProvider implements Callable<RegularFile> {

    SpotBugsTask task
    ConfigLoader loader
    QualityExtension extension

    // required to avoid duplicate calculations (because provider would be called multiple times)
    File computed

    SpotbugsExclusionConfigProvider(SpotBugsTask task, ConfigLoader loader, QualityExtension extension) {
        this.task = task
        this.loader = loader
        this.extension = extension
    }

    @Override
    RegularFile call() throws Exception {
        // exists condition required because of possible clean task which will remove prepared
        // default file and so it must be created again (damn props!!!)
        if (computed == null || !computed.exists()) {
            // if any exclusions configured, tmp file would be created ahead of time (most likely it would be required)
            computed = SpotbugsUtils.excludesFile(
                    // NOTE: here we can't check if task has pre-configured custom file because it would lock property
                    // so have to always use default file
                    task, extension, loader.resolveSpotbugsExclude(true))
        }
        return { -> computed } as RegularFile
    }
}
