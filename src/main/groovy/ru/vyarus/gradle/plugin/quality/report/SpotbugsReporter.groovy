package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.ConfigLoader

/**
 * Prints spotbugs errors (from xml report) into console and generates html report using custom xsl.
 * Gradle spotbugs plugin support html report generation too, but it can't generate both xml and html at the same
 * time (so we have to generate html separately, because xml report is required for console reporting).
 *
 * @author Vyacheslav Rusakov
 * @since 28.01.2018
 */
@CompileStatic
class SpotbugsReporter implements Reporter, HtmlReportGenerator {
    ConfigLoader configLoader

    SpotbugsReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(Project project, String type) {
        project.with {
            File reportFile = file("${extensions.spotbugs.reportsDir}/${type}.xml")
            if (!reportFile.exists()) {
                return
            }
            Node result = new XmlParser().parse(reportFile)
            int cnt = result.BugInstance.size()
            if (cnt > 0) {
                Node summary = result.FindBugsSummary[0]
                int fileCnt = summary.FileStats.findAll { (it.@bugCount as Integer) > 0 }.size()
                int p1 = summary.@priority_1 == null ? 0 : summary.@priority_1 as Integer
                int p2 = summary.@priority_2 == null ? 0 : summary.@priority_2 as Integer
                int p3 = summary.@priority_3 == null ? 0 : summary.@priority_3 as Integer
                logger.error "$NL$cnt ($p1 / $p2 / $p3) SpotBugs violations were found in ${fileCnt} files$NL"

                Map<String, String> desc = buildDescription(result)
                Map<String, String> cat = buildCategories(result)
                result.BugInstance.each { bug ->
                    Node msg = bug.LongMessage[0]
                    Node src = bug.SourceLine[0]
                    String description = ReportUtils.unescapeHtml(desc[bug.@type])
                    String srcPosition = src.@start
                    String classname = src.@classname
                    String pkg = classname[0..classname.lastIndexOf('.')]
                    String cls = src.@sourcefile
                    // part in braces recognized by intellij IDEA and shown as link
                    logger.error "[${cat[bug.@category]} | ${bug.@type}] $pkg(${cls}:${srcPosition})  " +
                            "[priority ${bug.@priority}]" +
                            "$NL\t>> ${msg.text()}" +
                            "$NL  ${description}$NL"
                }
                // html report will be generated before console reporting
                String htmlReportUrl = ReportUtils.toConsoleLink(project
                        .file("${project.extensions.spotbugs.reportsDir}/${type}.html"))
                project.logger.error "SpotBugs HTML report: $htmlReportUrl"
            }
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void generateHtmlReport(Project project, String type) {
        File reportFile = project.file("${project.extensions.spotbugs.reportsDir}/${type}.xml")
        if (!reportFile.exists()) {
            return
        }
        // html report
        String htmlReportPath = "${project.extensions.spotbugs.reportsDir}/${type}.html"
        File htmlReportFile = project.file(htmlReportPath)
        // avoid redundant re-generation
        if (!htmlReportFile.exists() || reportFile.lastModified() > htmlReportFile.lastModified()) {
            project.ant.xslt(in: reportFile,
                    style: configLoader.resolveSpotbugsXsl(),
                    out: htmlReportPath,
            )
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private Map<String, String> buildDescription(Node result) {
        Map<String, String> desc = [:]
        result.BugPattern.each { pattern ->
            desc[pattern.@type] = pattern.Details.text()
            //remove html tags
                    .replaceAll('<(.|\n)*?>', '')
            // remove empty lines after tags remove (only one separator line remain)
                    .replaceAll('([ \t]*\n){3,}', "$NL$NL")
            // reduce left indents
                    .replaceAll('\n\t+', "$NL  ").replaceAll(' {2,}', '  ')
            // indent all not indented lines
                    .replaceAll('\n([^\\s])', "$NL  \$1").trim()
        }
        return desc
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private Map<String, String> buildCategories(Node result) {
        Map<String, String> cat = [:]
        result.BugCategory.each { category ->
            cat[category.@category] = category.Description.text()
        }
        return cat
    }
}
