package ru.vyarus.gradle.plugin.quality.report

import com.github.spotbugs.snom.SpotBugsTask
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.ConfigLoader
import ru.vyarus.gradle.plugin.quality.util.FileUtils

/**
 * Prints spotbugs errors (from xml report) into console and generates html report using custom xsl.
 * Gradle spotbugs plugin support html report generation too, but it can't generate both xml and html at the same
 * time (so we have to generate html separately, because xml report is required for console reporting).
 *
 * @author Vyacheslav Rusakov
 * @since 28.01.2018
 */
@CompileStatic
class SpotbugsReporter implements Reporter<SpotBugsTask>, HtmlReportGenerator<SpotBugsTask> {
    private static final String XML = 'XML'

    ConfigLoader configLoader

    SpotbugsReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(SpotBugsTask task, String type) {
        // report may not exists
        File reportFile = ReportUtils.getReportFile(task.reports.findByName(XML))
        if (reportFile == null || !reportFile.exists() || reportFile.length() == 0) {
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
            task.logger.error "$NL$cnt ($p1 / $p2 / $p3) SpotBugs violations were found in ${fileCnt} " +
                    "files$NL"

            Map<String, String> desc = buildDescription(result)
            Map<String, String> cat = buildCategories(result)
            Map<String, String> plugins = resolvePluginsChecks(task.project)
            result.BugInstance.each { bug ->
                Node msg = bug.LongMessage[0]
                Node src = bug.SourceLine[0]
                String bugType = bug.@type
                String description = ReportUtils.unescapeHtml(desc[bugType])
                String srcPosition = src.@start
                String classname = src.@classname
                String pkg = classname[0..classname.lastIndexOf('.')]
                String cls = src.@sourcefile
                String plugin = plugins[bugType] ?: ''
                // part in braces recognized by intellij IDEA and shown as link
                task.logger.error "[${plugin}${cat[bug.@category]} | ${bugType}] $pkg(${cls}:${srcPosition})  " +
                        "[priority ${bug.@priority} / rank ${bug.@rank}]" +
                        "$NL\t>> ${msg.text()}" +
                        "$NL  ${description}$NL"
            }
            // html report will be generated before console reporting
            String htmlReportUrl = ReportUtils.toConsoleLink(task.project
                    .file("${task.project.extensions.spotbugs.reportsDir.get()}/${type}.html"))
            task.logger.error "SpotBugs HTML report: $htmlReportUrl"
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void generateHtmlReport(SpotBugsTask task, String type) {
        File reportFile = ReportUtils.getReportFile(task.reports.findByName(XML))
        if (reportFile == null || !reportFile.exists()) {
            return
        }
        Project project = task.project
        // html report
        String htmlReportPath = "${project.extensions.spotbugs.reportsDir.get()}/${type}.html"
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

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('CatchException')
    private Map<String, String> resolvePluginsChecks(Project project) {
        Map<String, String> res = [:]
        project.configurations.getByName('spotbugsPlugins').resolve().each { jar ->
            try {
                FileUtils.loadFileFromJar(jar, 'findbugs.xml') { InputStream desc ->
                    Node result = new XmlParser().parse(desc)
                    // to simplify reporting
                    String provider = result.@provider + ' | '
                    result.Detector.each {
                        it.@reports.split(',').each { String name ->
                            res.put(name, provider)
                        }
                    }
                }
            } catch (Exception ignore) {
                // it may be dependencies jars or format could suddenly change
            }
        }
        res
    }
}
