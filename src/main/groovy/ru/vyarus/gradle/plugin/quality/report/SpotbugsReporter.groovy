package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlParser
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.vyarus.gradle.plugin.quality.QualityPlugin
import ru.vyarus.gradle.plugin.quality.report.model.SpotbugsTaskDesc
import ru.vyarus.gradle.plugin.quality.report.model.factory.SpotbugsModelFactory
import ru.vyarus.gradle.plugin.quality.util.FileUtils

/**
 * Prints spotbugs errors (from xml report) into console and generates html report using custom xsl.
 *
 * @author Vyacheslav Rusakov
 * @since 28.01.2018
 */
@CompileStatic
class SpotbugsReporter implements Reporter<SpotbugsTaskDesc> {
    // See AbstractTask - it' i's used inside tasks
    private final Logger logger = Logging.getLogger(Task)

    Map<String, String> pluginChecks

    // for tests
    SpotbugsReporter(Project project) {
        this(resolvePluginsChecks(project))
    }

    SpotbugsReporter(Map<String, String> pluginChecks) {
        this.pluginChecks = pluginChecks
    }

    /**
     * As each spotbugs plugins use its own error types, then custom error descriptions must be loaded for
     * proper console output.
     *
     * @param project project instance
     * @return error descriptors from plugins
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('CatchException')
    static Map<String, String> resolvePluginsChecks(Project project) {
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

    // for tests
    void report(Task task, String sourceSet) {
        report(new SpotbugsModelFactory().buildDesc(task, QualityPlugin.TOOL_SPOTBUGS) as SpotbugsTaskDesc, sourceSet)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void report(SpotbugsTaskDesc task, String sourceSet) {
        // report may not exists
        File reportFile = new File(task.xmlReportPath)
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
            logger.error "$NL$cnt ($p1 / $p2 / $p3) SpotBugs violations were found in ${fileCnt} " +
                    "files$NL"

            Map<String, String> desc = buildDescription(result)
            Map<String, String> cat = buildCategories(result)
            result.BugInstance.each { bug ->
                Node msg = bug.LongMessage[0]
                Node src = bug.SourceLine[0]
                String bugType = bug.@type
                String description = ReportUtils.unescapeHtml(desc[bugType])
                String srcPosition = src.@start
                String classname = src.@classname
                String pkg = classname[0..classname.lastIndexOf('.')]
                String cls = src.@sourcefile
                String plugin = pluginChecks[bugType] ?: ''
                // part in braces recognized by intellij IDEA and shown as link
                logger.error "[${plugin}${cat[bug.@category]} | ${bugType}] $pkg(${cls}:${srcPosition})  " +
                        "[priority ${bug.@priority} / rank ${bug.@rank}]" +
                        "$NL\t>> ${msg.text()}" +
                        "$NL  ${description}$NL"
            }
            // html report will be generated before console reporting
            String htmlReportUrl = ReportUtils.toConsoleLink(new File("${task.reportsDir}/${sourceSet}.html"))
            logger.error "SpotBugs HTML report: $htmlReportUrl"
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private Map<String, String> buildDescription(Node result) {
        Map<String, String> desc = [:]
        result.BugPattern.each { pattern ->
            desc[pattern.@type] = pattern.Details.text()
            // remove html tags
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
