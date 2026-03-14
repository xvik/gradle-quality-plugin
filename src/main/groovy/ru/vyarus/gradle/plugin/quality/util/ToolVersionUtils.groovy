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
    // checkstyle 13 requires java 21 - 12.3.1 the latest compatible version
    static final String CHECKSTYLE_JAVA17 = '12.3.1'
    // checkstyle 11 requires java 17 = 10.26.1 the latest compatible version
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
        return isCheckstyleCompatible(version, JavaVersion.current())
    }

    static boolean isCheckstyleCompatible(String version, JavaVersion current) {
        int ver = version[0..version.indexOf('.') - 1] as int

        // checkstyle 13 require java 21
        return (ver >= 13 && current.isCompatibleWith(JavaVersion.VERSION_21))
                // checkstyle 11 - 12 require java 17
                || (ver <= 12 && current.isCompatibleWith(JavaVersion.VERSION_17))
                // checkstyle 10 requires java 11
                || (ver == 10 && current.isCompatibleWith(JavaVersion.VERSION_11))
    }

    /**
     * Method returns DEFAULT checkstyle version according to current jdk (and only if fallback enabled).
     *
     * @param fallback true if fallback enabled
     * @param version current default version
     * @return version compatible with jdk or provided version as is (if fallback disabled)
     */
    static String getCompatibleCheckstyleVersion(boolean fallback, String version) {
        return getCompatibleCheckstyleVersion(fallback, version, JavaVersion.current())
    }

    static String getCompatibleCheckstyleVersion(boolean fallback, String version, JavaVersion current) {
        if (fallback && !isCheckstyleCompatible(version, current)) {
            return current.isCompatibleWith(JavaVersion.VERSION_17) ? CHECKSTYLE_JAVA17 : CHECKSTYLE_JAVA11
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
        return isSpotbugsCompatible(version, JavaVersion.current())
    }

    static boolean isSpotbugsCompatible(String version, JavaVersion current) {
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
        return getCompatibleSpotbugsVersion(fallback, version, JavaVersion.current())
    }

    static String getCompatibleSpotbugsVersion(boolean fallback, String version, JavaVersion current) {
        if (fallback && !current.isCompatibleWith(JavaVersion.VERSION_11)) {
            return SPOTBUGS_JAVA8
        }
        return version
    }
}
