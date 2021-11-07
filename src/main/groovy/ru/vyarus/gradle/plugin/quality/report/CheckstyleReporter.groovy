package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.plugins.quality.Checkstyle
import ru.vyarus.gradle.plugin.quality.ConfigLoader

/**
 * Prints checkstyle errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 */
@SuppressWarnings('DuplicateStringLiteral')
@CompileStatic
class CheckstyleReporter implements Reporter<Checkstyle> {

    ConfigLoader configLoader

    CheckstyleReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(Checkstyle task, String type) {
        File reportFile = ReportUtils.getReportFile(task.reports.xml)
        if (!reportFile.exists() || reportFile.length() == 0) {
            return
        }

        Node result = new XmlParser().parse(reportFile)
        int cnt = result.file.error.size()
        if (cnt > 0) {
            int filesCnt = result.file.findAll { it.error.size() > 0 }.size()
            task.logger.error "$NL$cnt Checkstyle rule violations were found in $filesCnt files$NL"

            result.file.each { file ->
                String filePath = file.@name
                String sourceFile = ReportUtils.extractFile(filePath)
                String name = ReportUtils.extractJavaPackage(task.project, type, filePath)

                file.error.each {
                    String check = extractCheckName(it.@source)
                    String group = extractGroupName(it.@source)
                    String srcPointer = it.@line
                    // part in braces recognized by intellij IDEA and shown as link
                    task.logger.error "[${group.capitalize()} | $check] $name.($sourceFile:$srcPointer)" +
                            "$NL  ${it.@message}" +
                            "$NL  http://checkstyle.sourceforge.io/config_${group}.html#$check$NL"
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
