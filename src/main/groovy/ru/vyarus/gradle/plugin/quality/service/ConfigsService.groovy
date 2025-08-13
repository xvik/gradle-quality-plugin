package ru.vyarus.gradle.plugin.quality.service

import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

/**
 * Service, responsible for configuration files management. Loads configuration files either from custom configs
 * directory (quality.configDir) or from classpath. When loading from classpath all files are copied (and cached)
 * in build/quality-configs/.
 * <p>
 * To avoid problems with clean task, default configs are copied only before actual task run. All config file
 * getters contains optional parameter copyDefaultConfig to prevent actual file copying on configuration phase.
 *
 * @author Vyacheslav Rusakov
 * @since 02.08.2025
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class ConfigsService implements BuildService<Params>, OperationCompletionListener {

    private final String checkstyle = 'checkstyle/checkstyle.xml'
    private final String checkstyleSuppressions = 'checkstyle/suppressions.xml'
    private final String pmd = 'pmd/pmd.xml'
    private final String cpdXsl = 'cpd/cpdhtml.xslt'
    private final String spotbugsExclude = 'spotbugs/exclude.xml'
    private final String codenarc = 'codenarc/codenarc.xml'

    private final Logger logger = Logging.getLogger(ConfigsService)

    File resolveCheckstyleConfig(boolean copyDefaultFile = true) {
        resolve(checkstyle, copyDefaultFile)
    }

    File resolveCheckstyleConfigDir() {
        // used for ${config_loc} property definition (through checkstyle.configDirectory property)
        File path = new File(parameters.configDir.get().asFile, checkstyle).parentFile
        // if custom directory exists, use it, otherwise fall back to generated directory
        // because gradle 7 requires configured directory existence
        // (default file copying forced to create checkstyle directory and avoid gradle complains)
        return path.exists() ? path : resolveCheckstyleConfig(true).parentFile
    }

    File resolvePmdConfig(boolean copyDefaultFile = true) {
        resolve(pmd, copyDefaultFile)
    }

    File resolveCpdXsl(boolean copyDefaultFile = true) {
        resolve(cpdXsl, copyDefaultFile)
    }

    File resolveSpotbugsExclude(boolean copyDefaultFile = true) {
        resolve(spotbugsExclude, copyDefaultFile)
    }

    File resolveCodenarcConfig(boolean copyDefaultFile = true) {
        resolve(codenarc, copyDefaultFile)
    }

    /**
     * Copies default configs into configured user directory.
     *
     * @param override override filed
     */
    void initUserConfigs(boolean override) {
        [checkstyle, checkstyleSuppressions, pmd, cpdXsl, codenarc, spotbugsExclude].each {
            copyConfig(parameters.configDir.get().asFile, it, override)
        }
    }

    @Override
    @SuppressWarnings('EmptyMethodInAbstractClass')
    void onFinish(FinishEvent event) {
        // not used - just a way to prevent killing service too early
    }

    private File resolve(String path, boolean copyDefaultFile) {
        // look custom user file first
        File target = new File(parameters.configDir.get().asFile, path)
        boolean userFile = target.exists()
        // show message only just before task execution, not during configuration phase (avoid duplicate message)
        if (userFile && copyDefaultFile) {
            logger.info('[plugin:quality] Using custom quality configuration: {}', target.toString())
        }

        File tmpDir = parameters.tempDir.get().asFile
        return userFile ?
                target : (copyDefaultFile ? copyConfig(tmpDir, path, false) : new File(tmpDir, path)) as File
    }

    @SuppressWarnings('SynchronizedOnThis')
    // internal class so no monitor steal possible
    private File copyConfig(File parent, String path, boolean override) {
        File target = new File(parent, path)
        if (target.exists() && !override) {
            return target
        }
        synchronized (this) {
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

    interface Params extends BuildServiceParameters {
        RegularFileProperty getConfigDir();
        RegularFileProperty getTempDir();
    }
}
