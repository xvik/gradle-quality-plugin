package ru.vyarus.gradle.plugin.quality.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import ru.vyarus.gradle.plugin.quality.service.ConfigsService

import javax.inject.Inject

/**
 * Task copies required config files for quality tools if they are not provided in user configs directory.
 * <p>
 * It is also possible to use doFirst/doLast blocks to generate or modify files: task output covers entire directory
 * so it would be cached properly.
 *
 * @author Vyacheslav Rusakov
 * @since 17.08.2025
 */
@CompileStatic
@CacheableTask
@SuppressWarnings('AbstractClassWithPublicConstructor')
abstract class CopyConfigsTask extends DefaultTask {

    // re-init config if plugin version changes (to update defaults)
    @Input
    abstract Property<String> getPluginVersion()
    // required to re-init configs in case of exclusions change (important for spotbugs, which generates config file)
    @Input
    @Optional
    abstract ListProperty<String> getExclude()
    // can't directly depend on quality.excludeSources because gradle would complain in some cases (due to possible
    // implicit task dependency when other task output used for configuration). But still, configs task must be
    // invalidated after excludeSources change, so using synthetic hash to detect configuration changes.
    @Input
    @Optional
    abstract Property<String> getExcludeSources()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    abstract DirectoryProperty getConfigDir()

    @OutputDirectory
    abstract DirectoryProperty getTempDir()

    // list of user files that must be copied into temp dir for modification (used by spotbugs)
    @Input
    @Optional
    abstract ListProperty<String> getModifiableFiles()

    @Internal
    abstract Property<ConfigsService> getConfigsService()

    @Inject
    abstract FileSystemOperations getFs()

    CopyConfigsTask() {
        // to avoid duplicate configuration in plugin (task needs to know these properties for proper caching)
        configDir.set(configsService.map { it.parameters.configDir.get() })
        tempDir.set(configsService.map { it.parameters.tempDir.get() })

        description = 'Prepares configuration files for quality tools'
    }

    @TaskAction
    void run() {
        // delete previously generated files because user may specify a new custom file
        // (and so generated version should not exist)
        if (tempDir.get().asFile.exists()) {
            fs.delete { it.delete(tempDir.get()) }
        }

        File targetDir = tempDir.get().asFile

        configsService.get().configFileNames.each {
            File userConfig = configDir.get().file(it).asFile
            if (userConfig.exists()) {
                logger.info("User-provided quality config used: $it")
                return
            }
            configsService.get().copyDefaultConfig(targetDir, it, false)
            logger.info("Copied default quality config: $it")
        }

        // copy modifiable user configs
        modifiableFiles.get().each {
            configsService.get().copyUserConfigForModification(it)
            logger.info("User config copied for modifications: $it")
        }
    }
}
