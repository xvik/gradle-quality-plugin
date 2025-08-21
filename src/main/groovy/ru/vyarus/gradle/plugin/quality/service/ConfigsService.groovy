package ru.vyarus.gradle.plugin.quality.service

import groovy.transform.CompileStatic
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import ru.vyarus.gradle.plugin.quality.QualityPlugin

import javax.inject.Inject
import java.nio.file.Files

/**
 * Service, responsible for configuration files management. Loads configuration files either from custom configs
 * directory (quality.configDir) or from classpath. When loading from classpath all files are copied (and cached)
 * in build/quality-configs/ (for multi module projects different dir for each module!).
 * <p>
 * Configs are actually copied by a special task, executed before quality tasks. This way gradle could cache
 * configs and correctly apply build and configuration caches.
 *
 * @author Vyacheslav Rusakov
 * @since 02.08.2025
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class ConfigsService implements BuildService<Params>, OperationCompletionListener {

    private static final Object SYNC = new Object()
    @Inject
    abstract ProviderFactory getProviderFactory()

    /**
     * Decide what config file to use: either user provided from custom user directory or copied tmp config
     * from plugin jar. Note that config would be copied with a special task  - here only correct target selection
     * is required.
     *
     * @param path file path
     * @return file
     */
    RegularFile resolveConfigFile(String path) {
        RegularFile userFile = userConfigFile(path)
        return userFile.asFile.exists() ? userFile : tempConfigFile(path)
    }

    /**
     * Return config located in user directory.
     *
     * @param path file path
     * @return file
     */
    RegularFile userConfigFile(String path) {
        return parameters.configDir.get().file(path)
    }

    /**
     * Return config located in temp directory.
     *
     * @param path file path
     * @return file
     */
    RegularFile tempConfigFile(String path) {
        return parameters.tempDir.get().file(path)
    }

    /**
     * Copies default configs into configured user directory.
     *
     * @param override override filed
     */
    void initUserConfigs(boolean override) {
        configFileNames.each {
            copyDefaultConfig(parameters.configDir.get().asFile, it, override)
        }
    }

    @Override
    @SuppressWarnings('EmptyMethodInAbstractClass')
    void onFinish(FinishEvent event) {
        // not used - just a way to prevent killing service too early
    }

    List<String> getConfigFileNames() {
        List<String> configs = []
        QualityPlugin.TOOLS.each {
            configs.addAll(it.configs)
        }
        return configs
    }

    @SuppressWarnings('SynchronizedOnThis')
    // internal class so no monitor steal possible
    File copyDefaultConfig(File parent, String path, boolean override) {
        File target = new File(parent, path)
        if (target.exists() && !override) {
            return target
        }
        synchronized (SYNC) {
            if (!target.exists() || override) {
                if (target.exists() && override) {
                    target.delete()
                }
                if (!target.parentFile.exists() && !target.parentFile.mkdirs()) {
                    throw new IllegalStateException("Failed to create directories: $target.parentFile.absolutePath")
                }
                InputStream stream = getClass().getResourceAsStream("/ru/vyarus/quality/config/$path")
                if (stream == null) {
                    throw new IllegalStateException("Default config file not found in classpath: $path")
                }
                target << stream.text
            }
            return target
        }
    }

    /**
     * Copy file, provided by user into tmp configs directory for modifications. Do nothing if user config not
     * available (assuming default config would be copied automatically).
     *
     * @param path file path
     * @return true when copied
     */
    boolean copyUserConfigForModification(String path) {
        File config = userConfigFile(path).asFile
        File tmpCfg = tempConfigFile(path).asFile
        // otherwise nothing to do - default config would be copied
        if (config.exists()) {
            tmpCfg.parentFile.mkdirs()
            Files.copy(config.toPath(), tmpCfg.toPath())
            return true
        }
        return false
    }

    interface Params extends BuildServiceParameters {
        DirectoryProperty getConfigDir();
        DirectoryProperty getTempDir();
    }
}
