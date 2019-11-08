package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.ConfigLoader

/**
 * Prints CPD duplicates (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 08.11.2019
 */
@CompileStatic
class CpdReporter implements Reporter, HtmlReportGenerator {

    private static final String CODE_INDENT = '│'

    ConfigLoader configLoader

    CpdReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(Project project, String type) {
        project.with {
            File reportFile = file("${extensions.cpd.reportsDir}/${type}.xml")

            if (!reportFile.exists()) {
                return
            }
            Node result = new XmlParser().parse(reportFile)
            int cnt = result.duplication.size()
            if (cnt > 0) {
                logger.error "$NL$cnt duplicates were found by CPD$NL"
                result.duplication.each { dupl ->
                    int lines = dupl.@lines as Integer
                    int start = 0
                    boolean first = true
                    StringBuilder msg = new StringBuilder()
                    dupl.file.each { file ->
                        String filePath = file.@path
                        String sourceFile = ReportUtils.extractFile(filePath)
                        String name = ReportUtils.extractJavaPackage(project, 'main', filePath)
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

                    logger.error "${msg.toString()}$NL"
                }
                // html report will be generated before console reporting
                String htmlReportUrl = ReportUtils.toConsoleLink(project
                        .file("${project.extensions.cpd.reportsDir}/${type}.html"))
                project.logger.error "CPD HTML report: $htmlReportUrl"
            }
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void generateHtmlReport(Project project, String type) {
        File reportFile = project.file("${project.extensions.cpd.reportsDir}/${type}.xml")
        if (!reportFile.exists()) {
            return
        }
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
