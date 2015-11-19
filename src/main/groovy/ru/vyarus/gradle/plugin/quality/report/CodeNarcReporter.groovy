package ru.vyarus.gradle.plugin.quality.report

import org.gradle.api.Project

import java.util.zip.ZipFile

/**
 * Prints codenarc errors (from xml report) into console.
 *
 * @author Vyacheslav Rusakov 
 * @since 13.11.2015
 */
class CodeNarcReporter implements Reporter {

    @Override
    @SuppressWarnings('DuplicateStringLiteral')
    void report(Project project, String type) {
        project.with {
            File reportFile = file("${extensions.codenarc.reportsDir}/${type}.xml")
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
                logger.error "\n$count ($p1 / $p2 / $p3) CodeNarc violations were found in ${fileCnt} files"

                Map<String, String> desc = [:]
                result.Rules.Rule.each {
                    desc[it.@name] = it.Description.text()
                }

                Properties props = loadCodenarcProperties(project)

                result.Package.each {
                    String pkg = it.@path.replaceAll('/', '.')
                    it.File.each {
                        String src = it.@name.split('\\.')[0]
                        it.Violation.each {
                            String rule = it.@ruleName

                            String[] path = props[rule].split('\\.')
                            String group = path[path.length - 2]

                            String priority = it.@priority
                            String srcLine = ReportUtils.unescapeHtml(it.SourceLine.text())
                            logger.error "\n[${group.capitalize()} | ${rule}] ${pkg}.$src:${it.@lineNumber}  " +
                                    "(priority ${priority})" +
                                    "\n\t>> ${srcLine}" +
                                    "\n  ${it.Message.text()}" +
                                    "\n  ${desc[rule]}" +
                                    "\n  http://codenarc.sourceforge.net/codenarc-rules-${group}.html#$rule"
                        }
                    }
                }
            }
        }
    }

    private Properties loadCodenarcProperties(Project project) {
        Properties props = new Properties()
        File codenarcJar = project.configurations.getByName('codenarc').files.find {
            it.name.contains('CodeNarc')
        }
        ZipFile file = new ZipFile(codenarcJar)
        props.load(file.getInputStream(file.getEntry('codenarc-base-rules.properties')))
        return props
    }
}
