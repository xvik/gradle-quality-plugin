package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.reporting.Report
import org.gradle.api.tasks.SourceSet

/**
 * Reporting utils.
 *
 * @author Vyacheslav Rusakov
 * @since 16.11.2015
 */
@SuppressWarnings('DuplicateStringLiteral')
@CompileStatic
class ReportUtils {

    /**
     * Resolve java package from provided absolute source path.
     * Use configured java source roots to properly detect class package.
     *
     * @param project project instance
     * @param type execution type (main or test)
     * @param file absolute path to source file
     * @return package for provided java class path
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static String extractJavaPackage(Project project, String type, String file) {
        String name = new File(file).canonicalPath
        // try looking in java and related (groovy) sources (mixed mode)
        String root = matchRoot(project.sourceSets[type] as SourceSet, name)
        // if found, extract package, otherwise format file path, relative to project
        return root
                ? resolvePackage(root, name)
                : resolveFilePath(project.projectDir.canonicalPath, name)
    }

    /**
     * Special version of package matching mechanism for muti-module resolution: looks for all source sets and all
     * child modules.
     *
     * @param project project instance
     * @param file absolute path to source file
     * @return package for provided java class path
     */
    static String extractJavaPackage(Project project, String file) {
        String name = new File(file).canonicalPath

        String root = findMultiModuleRoot(project, name)
        // if found, extract package, otherwise format file path, relative to root project
        if (root) {
            // cut off possible module prefix
            String module = root.indexOf('>') > 0 ? root[0..root.lastIndexOf('>')] : ''
            if (module) {
                root = root[module.length()..-1]
            }
            // apply module prefix
            return module.replace('>', '/') + resolvePackage(root, file)
        }
        return resolveFilePath(project.rootDir.canonicalPath, name)
    }

    /**
     * @param path absolute path to source file
     * @return file name without path
     */
    static String extractFile(String path) {
        int idx = path.replaceAll('\\\\', '/').lastIndexOf('/')
        path[idx + 1..-1]
    }

    /**
     * Unescapes html string.
     * Uses XmlSlurper as the simplest way.
     *
     * @param html html
     * @return raw string without html specific words
     */
    static String unescapeHtml(String html) {
        new XmlSlurper().parseText("<t>${html.trim().replaceAll('\\&nbsp;', '')}</t>")
    }

    /**
     * @param file file to resolve path
     * @return canonical file path with '/' as separator and without leading slash for linux
     */
    static String noRootFilePath(File file) {
        String path = file.canonicalPath.replaceAll('\\\\', '/')
        path.startsWith('/') ? path[1..-1] : path
    }

    /**
     * @param file file
     * @return file link to use in console output
     */
    static String toConsoleLink(File file) {
        return "file:///${noRootFilePath(file)}"
    }

    /**
     * Required because destination property was deprecated since gradle 7, but it still must be used for
     * older gradle versions.
     *
     * @param report report instance
     * @return report destination file
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static File getReportFile(Report report) {
        if (report == null) {
            return null
        }
        // Provider for gradle 7 and Property for gradle 8
        // static compilation must be disabled for method!
        Object output = report.outputLocation
        return output.get().asFile
    }

    private static String matchRoot(SourceSet set, String filePath) {
        Closure search = { Iterable<File> files ->
            files*.canonicalPath.find { String s -> filePath.startsWith(s) }
        }
        // try looking in java and related (groovy) sources (mixed mode)
        return search(set.allJava.srcDirs)
    }

    private static String resolvePackage(String rootPath, String filePath) {
        String name = filePath
        if (rootPath) {
            name = name[rootPath.length() + 1..-1] // remove sources dir prefix
        }
        name = name[0..name.lastIndexOf('.') - 1] // remove extension
        name = name.replaceAll('\\\\|/', '.')
        name = name[0..name.lastIndexOf('.') - 1] // remove class name
        return name
    }

    private static String resolveFilePath(String rootPath, String filePath) {
        String name = filePath
        name = name[rootPath.length() + 1..-1] // relative to project root
        name = name[0..name.lastIndexOf('.') - 1] // remove extension
        name = name.replaceAll('\\\\|/', '/')
        name = name[0..name.lastIndexOf('/') - 1] // remove class name
        return name
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static String findMultiModuleRoot(Project project, String file) {
        // check source sets
        if (project.plugins.findPlugin(JavaBasePlugin) != null) {
            for (SourceSet set : project.sourceSets) {
                String root = matchRoot(set, file)
                if (root) {
                    return root
                }
            }
        }
        // check subprojects
        for (Project sub : project.subprojects) {
            String res = findMultiModuleRoot(sub, file)
            if (res != null) {
                // apply module name to easily identify class location
                return "$sub.name>$res"
            }
        }
        return null
    }
}
