package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask
import ru.vyarus.gradle.plugin.quality.ConfigLoader

/**
 * Prints CPD duplicates (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 08.11.2019
 */
@CompileStatic
class CpdReporter implements Reporter<SourceTask>, HtmlReportGenerator<SourceTask> {

    private static final String CODE_INDENT = '│'

    ConfigLoader configLoader

    CpdReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(SourceTask task, String type) {
        File reportFile = task.reports.xml.destination

        if (!reportFile.exists() || reportFile.length() == 0) {
            return
        }
        Node result = new XmlParser().parse(reportFile)
        int cnt = result.duplication.size()
        if (cnt > 0) {
            task.logger.error "$NL$cnt ${task.language} duplicates were found by CPD$NL"
            result.duplication.each { dupl ->
                int lines = dupl.@lines as Integer
                int start = 0
                boolean first = true
                StringBuilder msg = new StringBuilder()
                dupl.file.each { file ->
                    String filePath = file.@path
                    String sourceFile = ReportUtils.extractFile(filePath)
                    String name = ReportUtils.extractJavaPackage(task.project, filePath)
                    msg << "$name.($sourceFile:${file.@line})"
                    if (first) {
                        start = file.@line as Integer
                        msg << "  [${lines} lines / ${dupl.@tokens} tokens]$NL"
                        first = false
                    } else {
                        msg << NL
                    }
                }
                String maxNbSpace = String.valueOf(start + lines).replaceAll('.', ' ')
                String nbFmt = "%${maxNbSpace.length()}s"
                // identify code block
                msg << "$maxNbSpace$CODE_INDENT$NL"

                int codePos = start
                dupl.codefragment.text().eachLine {
                    msg << "${String.format(nbFmt, codePos++)}$CODE_INDENT    $it$NL"
                }

                task.logger.error "${msg.toString()}$NL"
            }
            // html report will be generated before console reporting
            String htmlReportUrl = ReportUtils.toConsoleLink(task.project
                    .file("${task.project.extensions.cpd.reportsDir}/${type}.html"))
            task.logger.error "CPD HTML report: $htmlReportUrl"
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void generateHtmlReport(SourceTask task, String type) {
        File reportFile = task.reports.xml.destination
        if (!reportFile.exists()) {
            return
        }
        Project project = task.project
        // html report
        String htmlReportPath = "${project.extensions.cpd.reportsDir}/${type}.html"
        File htmlReportFile = project.file(htmlReportPath)
        // avoid redundant re-generation
        if (!htmlReportFile.exists() || reportFile.lastModified() > htmlReportFile.lastModified()) {
            project.ant.xslt(in: reportFile,
                    style: configLoader.resolveCpdXsl(),
                    out: htmlReportPath,
            )
        }
    }
}
