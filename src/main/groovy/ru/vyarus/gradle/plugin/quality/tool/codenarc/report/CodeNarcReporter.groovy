package ru.vyarus.gradle.plugin.quality.tool.codenarc.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlParser
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.vyarus.gradle.plugin.quality.report.ReportUtils
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.report.model.TaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.TaskDescFactory
import ru.vyarus.gradle.plugin.quality.tool.codenarc.CodenarcTool
import ru.vyarus.gradle.plugin.quality.util.FileUtils

/**
 * Prints codenarc errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2015
 */
@CompileStatic
class CodeNarcReporter implements Reporter {
    // See AbstractTask - it' i's used inside tasks
    private final Logger logger = Logging.getLogger(Task)

    private final Properties codenarcProperties

    // for tests
    CodeNarcReporter(Project project) {
        this(loadCodenarcProperties(project))
    }

    CodeNarcReporter(Properties codenarcProperties) {
        this.codenarcProperties = codenarcProperties
    }

    /**
     * @param project project
     * @return codenarc properties with rules reference (from codenarc jar)
     */
    static Properties loadCodenarcProperties(Project project) {
        File codenarcJar = FileUtils.findConfigurationJar(project, 'codenarc', 'CodeNarc')
        return FileUtils.loadFileFromJar(codenarcJar, 'codenarc-base-rules.properties') { InputStream it ->
            Properties props = new Properties()
            props.load(it)
            return props
        }
    }

    // for tests
    void report(Task task, String sourceSet) {
        report(new TaskDescFactory().buildDesc(task, CodenarcTool.NAME), sourceSet)
    }

    @Override
    @SuppressWarnings('DuplicateStringLiteral')
    @CompileStatic(TypeCheckingMode.SKIP)
    // org.gradle.api.plugins.quality.CodeNarc
    void report(TaskDesc task, String sourceSet) {
        File reportFile = new File(task.xmlReportPath)
        if (!reportFile.exists() || reportFile.length() == 0) {
            return
        }
        Node result = new XmlParser().parse(reportFile)
        Node summary = result.PackageSummary[0]
        int fileCnt = summary.@filesWithViolations as Integer
        if (fileCnt > 0) {
            Integer p1 = summary.@priority1 as Integer
            Integer p2 = summary.@priority2 as Integer
            Integer p3 = summary.@priority3 as Integer
            Integer count = p1 + p2 + p3
            logger.error "$NL$count ($p1 / $p2 / $p3) CodeNarc violations were found in ${fileCnt} " +
                    "files$NL"

            Map<String, String> desc = [:]
            result.Rules.Rule.each {
                desc[it.@name] = ReportUtils.unescapeHtml(it.Description.text())
            }

            result.Package.each {
                String pkg = it.@path.replaceAll('/', '.')
                it.File.each {
                    String src = it.@name
                    it.Violation.each {
                        String rule = it.@ruleName
                        String[] path = ((codenarcProperties[rule]) as String).split('\\.')
                        String group = path[path.length - 2]
                        String priority = it.@priority
                        String srcLine = ReportUtils.unescapeHtml(it.SourceLine.text())
                        String message = ReportUtils.unescapeHtml(it.Message.text())
                        // part in braces recognized by intellij IDEA and shown as link
                        logger.error "[${group.capitalize()} | ${rule}] ${pkg}.($src:${it.@lineNumber})  " +
                                "[priority ${priority}]" +
                                "$NL\t>> ${srcLine}" +
                                "$NL  ${message}" +
                                "$NL  ${desc[rule]}" +
                                "$NL  https://codenarc.github.io/CodeNarc/codenarc-rules-" +
                                "${group.toLowerCase()}.html#${rule.toLowerCase()}-rule$NL"
                    }
                }
            }
        }
    }
}
