package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CodeNarc
import ru.vyarus.gradle.plugin.quality.util.FileUtils

/**
 * Prints codenarc errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2015
 */
@CompileStatic
class CodeNarcReporter implements Reporter<CodeNarc> {

    @Override
    @SuppressWarnings('DuplicateStringLiteral')
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(CodeNarc task, String type) {
        File reportFile = task.reports.xml.destination
        if (!reportFile.exists()) {
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
            task.logger.error "$NL$count ($p1 / $p2 / $p3) CodeNarc violations were found in ${fileCnt} files$NL"

            Map<String, String> desc = [:]
            result.Rules.Rule.each {
                desc[it.@name] = ReportUtils.unescapeHtml(it.Description.text())
            }

            Properties props = loadCodenarcProperties(task.project)

            result.Package.each {
                String pkg = it.@path.replaceAll('/', '.')
                it.File.each {
                    String src = it.@name
                    it.Violation.each {
                        String rule = it.@ruleName
                        String[] path = props[rule].split('\\.')
                        String group = path[path.length - 2]
                        String priority = it.@priority
                        String srcLine = ReportUtils.unescapeHtml(it.SourceLine.text())
                        String message = ReportUtils.unescapeHtml(it.Message.text())
                        // part in braces recognized by intellij IDEA and shown as link
                        task.logger.error "[${group.capitalize()} | ${rule}] ${pkg}.($src:${it.@lineNumber})  " +
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

    private Properties loadCodenarcProperties(Project project) {
        File codenarcJar = FileUtils.findConfigurationJar(project, 'codenarc', 'CodeNarc')
        return FileUtils.loadFileFromJar(codenarcJar, 'codenarc-base-rules.properties') { InputStream it ->
            Properties props = new Properties()
            props.load(it)
            return props
        }
    }
}
