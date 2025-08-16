package ru.vyarus.gradle.plugin.quality.tool.pmd.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlParser
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.vyarus.gradle.plugin.quality.report.ReportUtils
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.tool.pmd.PmdTool

/**
 * Prints pmd errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 */
@CompileStatic
class PmdReporter implements Reporter {
    // See AbstractTask - it' i's used inside tasks
    private final Logger logger = Logging.getLogger(Task)

    // for tests
    void report(Task task, String sourceSet) {
        report(new TaskDescFactory().buildDesc(task, PmdTool.NAME), sourceSet)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(TaskDesc task, String sourceSet) {
        File reportFile = new File(task.xmlReportPath)
        if (!reportFile.exists() || reportFile.length() == 0) {
            return
        }
        Node result = new XmlParser().parse(reportFile)
        int cnt = result.file.violation.size()
        if (cnt > 0) {
            logger.error "$NL$cnt PMD rule violations were found in ${result.file.size()} files$NL"

            result.file.each { file ->
                String filePath = file.@name
                String sourceFile = ReportUtils.extractFile(filePath)
                String name = ReportUtils.extractJavaPackage(task.projectPath, task.sourceDirs, filePath)
                file.violation.each { violation ->
                    String srcPos = violation.@beginline
                    // part in braces recognized by intellij IDEA and shown as link
                    logger.error "[${violation.@ruleset} | ${violation.@rule}] $name.($sourceFile:${srcPos})" +
                            "$NL  ${violation.text().trim()}" +
                            "$NL  ${violation.@externalInfoUrl}$NL"
                }
            }
        }
    }
}
