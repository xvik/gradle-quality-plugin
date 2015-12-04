package ru.vyarus.gradle.plugin.quality

import org.gradle.api.Project

/**
 * Loads configuration files either from custom configs directory (quality.configDir) or from classpath.
 * When loading from classpath all files are copied (and cached) in build/quality-configs/.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 */
class ConfigLoader {
    private final String checkstyle = 'checkstyle/checkstyle.xml'
    private final String checkstyleXsl = 'checkstyle/html-report-style.xsl'
    private final String pmd = 'pmd/pmd.xml'
    private final String findbugsExclude = 'findbugs/exclude.xml'
    private final String findbugsXsl = 'findbugs/html-report-style.xsl'
    private final String codenarc = 'codenarc/codenarc.xml'

    Project project
    File configDir
    File tmpConfigDir

    ConfigLoader(Project project) {
        this.project = project
    }

    File getCheckstyleConfig() {
        resolve(checkstyle)
    }

    File getCheckstyleXsl() {
        resolve(checkstyleXsl)
    }

    File getPmdConfig() {
        resolve(pmd)
    }

    File getFindbugsExclude() {
        resolve(findbugsExclude)
    }

    File getFindbugsXsl() {
        resolve(findbugsXsl)
    }

    File getCodenarcConfig() {
        resolve(codenarc)
    }

    /**
     * Copies default configs into configured user directory.
     *
     * @param override override filed
     */
    void initUserConfigs(boolean override) {
        init()
        [checkstyle, checkstyleXsl, pmd, findbugsExclude, findbugsXsl, codenarc].each {
            copyConfig(configDir, it, override)
        }
    }

    private File resolve(String path) {
        init()
        // look custom user file first
        File target = new File(configDir, path)
        return target.exists() ?
                target : copyConfig(tmpConfigDir, path, false)
    }

    private void init() {
        if (configDir == null) {
            // lazy resolution to make sure user configuration applied
            this.configDir = project.rootProject.file(project.extensions.findByType(QualityExtension).configDir)
            this.tmpConfigDir = project.file("$project.buildDir/quality-configs/")
        }
    }

    private copyConfig(File parent, String path, boolean override) {
        File target = new File(parent, path)
        if (target.exists() && !override) {
            return target
        }
        if (!target.parentFile.exists() && !target.parentFile.mkdirs()) {
            throw new IllegalStateException("Failed to create directories: $target.parentFile.absolutePath")
        }
        InputStream stream = getClass().getResourceAsStream("/ru/vyarus/quality/config/$path")
        if (stream == null) {
            throw new IllegalStateException("Default config file not found in classpath: $path")
        }
        target << stream.text
        return target
    }
}
