package ru.vyarus.gradle.plugin.quality.util

import groovy.transform.CompileStatic
import org.gradle.api.JavaVersion

/**
 * Utility searches for compatible tool version for the current jdk.
 *
 * @author Vyacheslav Rusakov
 * @since 07.08.2025
 */
@CompileStatic
class ToolVersionUtils {
    static final String CHECKSTYLE_JAVA11 = '10.26.1'
    static final String SPOTBUGS_JAVA8 = '4.8.6'

    /**
     * Detects if configured version is compatible with current jdk. This is required for case when user manually
     * configures lower checkstyle version so plugin must not be disabled (if provided version is compatible).
     *
     * @param version version to check
     * @return true if version is compatible with current jdk
     */
    static boolean isCheckstyleCompatible(String version) {
        JavaVersion current = JavaVersion.current()
        if (current.isCompatibleWith(JavaVersion.VERSION_17)) {
            return true
        }
        if (current.isCompatibleWith(JavaVersion.VERSION_11)) {
            return version.startsWith('10')
        }
        return false
    }

    /**
     * Method returns DEFAULT checkstyle version according to current jdk (and only if fallback enabled).
     *
     * @param fallback true if fallback enabled
     * @param version current default version
     * @return version compatible with jdk or provided version as is (if fallback disabled)
     */
    static String getCompatibleCheckstyleVersion(boolean fallback, String version) {
        JavaVersion current = JavaVersion.current()
        if (fallback && !current.isCompatibleWith(JavaVersion.VERSION_17)) {
            return CHECKSTYLE_JAVA11
        }
        return version
    }

    /**
     * Detects if configured version is compatible with current jdk. This is required for case when user manually
     * configures lower spotbugs version so plugin must not be disabled (if provided version is compatible).
     *
     * @param version version to check
     * @return true if version is compatible with current jdk
     */
    static boolean isSpotbugsCompatible(String version) {
        JavaVersion current = JavaVersion.current()
        if (current.isCompatibleWith(JavaVersion.VERSION_11)) {
            return true
        }
        return version.startsWith('4.8')
    }

    /**
     * Method returns DEFAULT spotbugs version according to current jdk (and only if fallback enabled).
     *
     * @param fallback true if fallback enabled
     * @param version current default version
     * @return version compatible with jdk or provided version as is (if fallback disabled)
     */
    static String getCompatibleSpotbugsVersion(boolean fallback, String version) {
        JavaVersion current = JavaVersion.current()
        if (fallback && !current.isCompatibleWith(JavaVersion.VERSION_11)) {
            return SPOTBUGS_JAVA8
        }
        return version
    }
}
