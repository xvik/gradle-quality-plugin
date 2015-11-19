package ru.vyarus.gradle.plugin.quality.report

import org.gradle.api.Project

/**
 * Prints pmd errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov 
 * @since 12.11.2015
 */
class PmdReporter implements Reporter {

    @Override
    void report(Project project, String type) {
        project.with {
            File reportFile = file("${extensions.pmd.reportsDir}/${type}.xml")

            if (!reportFile.exists()) {
                return
            }
            Node result = new XmlParser().parse(reportFile)
            int cnt = result.file.violation.size()
            if (cnt > 0) {
                logger.error "\n$cnt PMD rule violations were found in ${result.file.size()} files"

                result.file.each { file ->
                    String name = ReportUtils.extractJavaClass(project, type, file.@name)
                    file.violation.each { violation ->
                        String srcPos = violation.@beginline == violation.@endline ?
                                violation.@beginline : "${violation.@beginline}-${violation.@endline}"
                        logger.error "\n[${violation.@ruleset} | ${violation.@rule}] ${name}:${srcPos}" +
                                "\n  ${violation.text().trim()}" +
                                "\n  ${violation.@externalInfoUrl}"
                    }
                }
            }
        }
    }
}
