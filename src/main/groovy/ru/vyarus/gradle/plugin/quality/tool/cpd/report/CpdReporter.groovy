package ru.vyarus.gradle.plugin.quality.tool.cpd.report

import groovy.ant.AntBuilder
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlParser
import org.apache.tools.ant.BuildLogger
import org.apache.tools.ant.NoBannerLogger
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import ru.vyarus.gradle.plugin.quality.QualityPlugin
import ru.vyarus.gradle.plugin.quality.report.HtmlReportGenerator
import ru.vyarus.gradle.plugin.quality.report.ReportUtils
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.service.ConfigsService
import ru.vyarus.gradle.plugin.quality.tool.cpd.CpdTool

/**
 * Prints CPD duplicates (from xml report) into console.
 *
 * @author Vyacheslav Rusakov
 * @since 08.11.2019
 */
@CompileStatic
class CpdReporter implements Reporter<CpdTaskDesc>, HtmlReportGenerator<CpdTaskDesc> {
    private static final String CODE_INDENT = '│'

    // See AbstractTask - it' i's used inside tasks
    private final Logger logger = Logging.getLogger(Task)

    private final Provider<ConfigsService> configs

    // for tests
    CpdReporter(Project project) {
        // in prod service created per project (with custom name), for tests simple service name is ok
        this(project.gradle.sharedServices.registerIfAbsent(QualityPlugin.CONFIGS_SERVICE, ConfigsService))
    }

    CpdReporter(Provider<ConfigsService> configs) {
        this.configs = configs
    }

    // for tests
    void report(Task task, String sourceSet) {
        report(new CpdTaskDescFactory().buildDesc(task, CpdTool.NAME) as CpdTaskDesc, sourceSet)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    // org.gradle.api.tasks.SourceTask
    void report(CpdTaskDesc task, String sourceSet) {
        File reportFile = new File(task.xmlReportPath)

        if (!reportFile.exists() || reportFile.length() == 0) {
            return
        }
        Node result = new XmlParser().parse(reportFile)
        int cnt = result.duplication.size()
        if (cnt > 0) {
            logger.error "$NL$cnt ${task.language} duplicates were found by CPD$NL"
            result.duplication.each { dupl ->
                int lines = dupl.@lines as Integer
                int start = 0
                boolean first = true
                StringBuilder msg = new StringBuilder()
                dupl.file.each { file ->
                    String filePath = file.@path
                    String sourceFile = ReportUtils.extractFile(filePath)
                    String name = ReportUtils.extractJavaPackage(task.rootProjectPath, task.sources, filePath)
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

                logger.error "$msg$NL"
            }
            // html report will be generated before console reporting
            String htmlReportUrl = ReportUtils.toConsoleLink(new File("${task.reportsDir}/${sourceSet}.html"))
            logger.error "CPD HTML report: $htmlReportUrl"
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void generateHtmlReport(CpdTaskDesc task, String sourceSet) {
        File reportFile = new File(task.xmlReportPath)
        if (!reportFile.exists()) {
            return
        }
        // html report
        String htmlReportPath = "${task.reportsDir}/${sourceSet}.html"
        File htmlReportFile = new File(htmlReportPath)
        // avoid redundant re-generation
        if (!htmlReportFile.exists() || reportFile.lastModified() > htmlReportFile.lastModified()) {
            createAnt().xslt(in: reportFile,
                    style: configs.get().resolveConfigFile(CpdTool.cpd_xsl).asFile,
                    out: htmlReportFile.canonicalPath,
            )
        }
    }

    private AntBuilder createAnt() {
        // see AntBuilder default constructor
        final org.apache.tools.ant.Project project = new org.apache.tools.ant.Project()
        final BuildLogger logger = new NoBannerLogger()

        logger.messageOutputLevel = org.apache.tools.ant.Project.MSG_ERR
        logger.outputPrintStream = System.out
        logger.errorPrintStream = System.err

        project.addBuildListener(logger)
        project.init()
        project.baseDir
        return new AntBuilder(project)
    }
}
