package ru.vyarus.gradle.plugin.quality.util

import groovy.xml.XmlUtil
import org.gradle.api.plugins.quality.FindBugs
import org.gradle.api.tasks.SourceSet
import org.slf4j.Logger

/**
 * Findbugs helper utils.
 *
 * @author Vyacheslav Rusakov
 * @since 17.03.2017
 */
class FindbugsUtils {

    /**
     * Replace exclusion file with extended one when exclusions are required.
     *
     * @param task findbugs task
     * @param excludes exclusion patterns
     * @param sets all source sets, covered with quality
     */
    static void replaceExcludeFilter(FindBugs task, Collection<String> excludes, Collection<SourceSet> sets,
                                     Logger logger) {
        Set<File> ignored = FileUtils.resolveIgnoredFiles(task.source, excludes)
        if (!ignored) {
            // no excluded files
            return
        }
        SourceSet set = FileUtils.findMatchingSet('findbugs', task.name, sets)
        if (!set) {
            logger.error("[Findbugs] Failed to find source set for task ${task.name}: exclusions " +
                    "(${excludes}) will not be applied")
            return
        }
        task.excludeFilter = mergeExcludes(task.excludeFilter, ignored, set.allJava.srcDirs)
    }

    /**
     * Findbugs task is a {@link org.gradle.api.tasks.SourceTask}, but does not properly support exclusions.
     * To overcome this limitation, source exclusions could be added to findbugs exclusions filter xml file.
     *
     * @param src original excludes file (default of user defined)
     * @param exclude files to exclude
     * @param roots source directories (to resolve class files)
     */
    @SuppressWarnings('FileCreateTempFile')
    static File mergeExcludes(File src, Collection<File> exclude, Collection<File> roots) {

        Node xml = new XmlParser().parse(src)
        exclude.each {
            String clazz = FileUtils.extractJavaClass(roots, it)
            if (clazz) {
                Node match = xml.appendNode('Match').appendNode('Class')
                match.attributes().put('name', clazz)
            }
        }

        File tmp = File.createTempFile('findbugs-extended-exclude', '.xml')
        tmp.deleteOnExit()
        tmp.withWriter { XmlUtil.serialize(xml, it) }
        return tmp
    }
}
