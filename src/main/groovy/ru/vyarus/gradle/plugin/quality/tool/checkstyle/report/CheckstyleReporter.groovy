package ru.vyarus.gradle.plugin.quality.tool.checkstyle.report

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
import ru.vyarus.gradle.plugin.quality.tool.checkstyle.CheckstyleTool

/**
 * Prints checkstyle errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 */
@SuppressWarnings('DuplicateStringLiteral')
@CompileStatic
class CheckstyleReporter implements Reporter {

    // See AbstractTask - it' i's used inside tasks
    private final Logger logger = Logging.getLogger(Task)

    // for tests
    void report(Task task, String sourceSet) {
        report(new TaskDescFactory().buildDesc(task, CheckstyleTool.NAME), sourceSet)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(TaskDesc task, String sourceSet) {
        File reportFile = new File(task.xmlReportPath)
        if (!reportFile.exists() || reportFile.length() == 0) {
            return
        }

        Node result = new XmlParser().parse(reportFile)
        int cnt = result.file.error.size()
        if (cnt > 0) {
            int filesCnt = result.file.findAll { it.error.size() > 0 }.size()
            logger.error "$NL$cnt Checkstyle rule violations were found in $filesCnt files$NL"

            result.file.each { file ->
                String filePath = file.@name
                String sourceFile = ReportUtils.extractFile(filePath)
                String name = ReportUtils.extractJavaPackage(task.projectPath, task.sourceDirs, filePath)

                file.error.each {
                    String check = extractCheckName(it.@source)
                    String group = extractGroupName(it.@source)
                    String srcPointer = it.@line
                    // part in braces recognized by intellij IDEA and shown as link
                    logger.error "[${group.capitalize()} | $check] $name.($sourceFile:$srcPointer)" +
                            "$NL  ${it.@message}" +
                            "$NL  https://checkstyle.sourceforge.io/checks/${group}/${check.toLowerCase()}.html$NL"
                }
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
        if (group == 'checks' || group == 'indentation') {
            group = 'misc'
        }
        return group
    }
}
