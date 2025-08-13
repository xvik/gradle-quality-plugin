package ru.vyarus.gradle.plugin.quality.report

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlSlurper

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
    static String extractJavaPackage(String projectDir, List<String> sourceFiles, String file) {
        String name = new File(file).canonicalPath
        // try looking in java and related (groovy) sources (mixed mode)
        String root = matchRoot(sourceFiles, name)
        // if found, extract package, otherwise format file path, relative to project
        return root
                ? resolvePackage(root, name)
                : resolveFilePath(projectDir, name)
    }

    /**
     * Special version of package matching mechanism for muti-module resolution: looks for all source sets and all
     * child modules.
     *
     * @param rootProjectPath root project directory
     * @param sources source set dirs from project and sub projects (key is a module path, "" for root project)
     * @param file absolute path to source file
     * @return package for provided java class path
     */
    static String extractJavaPackage(String rootProjectPath, Map<String, List<String>> sources, String file) {
        String name = new File(file).canonicalPath

        String root = findMultiModuleRoot(sources, name)
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
        return resolveFilePath(rootProjectPath, name)
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

    private static String matchRoot(List<String> sources, String filePath) {
        // try looking in java and related (groovy) sources (mixed mode)
        return sources.find { filePath.startsWith(it) }
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
    private static String findMultiModuleRoot(Map<String, List<String>> sources, String file) {
        // check each project and sub projects source sets
        for (String root : sources.keySet()) {
            String match = matchRoot(sources.get(root), file)
            if (match) {
                // prefix module name for readability
                return (root.empty ? '' : "$root>") + match
            }
        }
        return null
    }
}
