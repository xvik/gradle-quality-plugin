package ru.vyarus.gradle.plugin.quality.report

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.ConfigLoader

/**
 * Prints checkstyle errors (from xml report) into console and generates html report using custom xsl.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 */
@SuppressWarnings('DuplicateStringLiteral')
class CheckstyleReporter implements Reporter {

    ConfigLoader configLoader

    CheckstyleReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader
    }

    @Override
    void report(Project project, String type) {
        project.with {
            File reportFile = file("${extensions.checkstyle.reportsDir}/${type}.xml")
            if (!reportFile.exists()) {
                return
            }

            Node result = new XmlParser().parse(reportFile)
            int cnt = result.file.error.size()
            if (cnt > 0) {
                int filesCnt = result.file.findAll { it.error.size() > 0 }.size()
                logger.error "\n$cnt Checkstyle rule violations were found in $filesCnt files"

                result.file.each { file ->
                    String name = ReportUtils.extractJavaClass(project, type, file.@name)

                    file.error.each {
                        String check = extractCheckName(it.@source)
                        String group = extractGroupName(it.@source)
                        String srcPointer = it.@column ? "(${it.@line}:${it.@column})" : it.@line
                        logger.error "\n[${group.capitalize()} | $check] $name:$srcPointer" +
                                "\n  ${it.@message}" +
                                "\n  http://checkstyle.sourceforge.net/config_${group}.html#$check"
                    }
                }

                String htmlReportPath = "${extensions.checkstyle.reportsDir}/${type}.html"
                ant.xslt(in: reportFile,
                        style: configLoader.checkstyleXsl,
                        out: htmlReportPath
                )
                String htmlReportUrl = ReportUtils.toConsoleLink(file(htmlReportPath))
                logger.error "\nCheckstyle HTML report: $htmlReportUrl"
            }
        }
    }

    private String extractCheckName(String source) {
        String check = source
        check = check[check.lastIndexOf('.') + 1..-1]
        check[0..(check.length() - 1 - 'Check'.length())]
    }

    private String extractGroupName(String source) {
        String[] path = source.split('\\.')
        String group = path[path.length - 2]
        if (group == 'checks') {
            group = 'misc'
        }
        return group
    }
}
