package ru.vyarus.gradle.plugin.quality.report

import org.gradle.api.Project

/**
 * Reporting utils.
 *
 * @author Vyacheslav Rusakov
 * @since 16.11.2015
 */
@SuppressWarnings('DuplicateStringLiteral')
class ReportUtils {

    /**
     * Converts java source file absolute path into class reference.
     * Use configured java source roots to properly detect class package.
     *
     * @param project project instance
     * @param type execution type (main or test)
     * @param file absolute path to java source file
     * @return java class reference
     */
    static String extractJavaClass(Project project, String type, String file) {
        String name = new File(file).canonicalPath
        project.sourceSets[type].java.srcDirs.each {
            if (name.startsWith(it.canonicalPath)) {
                name = name[it.canonicalPath.length() + 1..-1] // remove sources dir prefix
            }
        }
        name = name[0..name.lastIndexOf('.') - 1] // remove extension
        name.replaceAll('\\\\|/', '.')
    }

    /**
     * Unescapes html string.
     * Uses XmlSlurper as the simplest way.
     *
     * @param html html
     * @return raw string without html specific words
     */
    static String unescapeHtml(String html) {
        new XmlSlurper().parseText("<t>${html.trim().replaceAll('\\&nbsp;', '')}</t>")
    }

    /**
     * @param file file
     * @return file link to use in console output
     */
    static String toConsoleLink(File file) {
        String path = file.canonicalPath.replaceAll('\\\\', '/')
        // remove trailing slash for linux
        return "file:///${path.startsWith('/') ? path[1..-1] : path}"
    }
}
