package ru.vyarus.gradle.plugin.quality.util

import com.github.spotbugs.SpotBugsTask
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlUtil
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.slf4j.Logger
import ru.vyarus.gradle.plugin.quality.QualityExtension

/**
 * Spotbugs helper utils.
 *
 * @author Vyacheslav Rusakov
 * @since 28.01.2018
 */
@CompileStatic
class SpotbugsUtils {

    private static final int MAX_RANK = 20
    private static final String MATCH = 'Match'

    /**
     * Validate declared rank value correctness.
     * @param rank rank value
     */
    static void validateRankSetting(int rank) {
        if (rank < 1 || rank > MAX_RANK) {
            throw new IllegalArgumentException(
                    "spotbugsMaxRank may be only between 1 and 20, but it is set to $rank ")
        }
    }

    /**
     * Replace exclusion file with extended one when exclusions are required.
     *
     * @param task spotbugs task
     * @param extension extension instance
     * @param logger project logger for error messages
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static void replaceExcludeFilter(SpotBugsTask task, QualityExtension extension, Logger logger) {
        // setting means max allowed rank, but filter evicts all ranks >= specified (so +1)
        Integer rank = extension.spotbugsMaxRank < MAX_RANK ? extension.spotbugsMaxRank + 1 : null
        Set<File> ignored = FileUtils.resolveIgnoredFiles(task.source, extension.exclude)
        if (extension.excludeSources) {
            // add directly excluded files
            ignored.addAll(extension.excludeSources.asFileTree.matching { include '**/*.java' }.files)
        }
        if (!ignored && !rank) {
            // no custom excludes required
            return
        }
        SourceSet set = FileUtils.findMatchingSet('spotbugs', task.name, extension.sourceSets)
        if (!set) {
            logger.error("[SpotBugs] Failed to find source set for task ${task.name}: exclusions " +
                    ' will not be applied')
            return
        }

        task.excludeFilter = mergeExcludes(task.excludeFilter, ignored, set.allJava.srcDirs, rank)
    }

    /**
     * Spotbugs task is a {@link org.gradle.api.tasks.SourceTask}, but does not properly support exclusions.
     * To overcome this limitation, source exclusions could be added to spotbugs exclusions filter xml file.
     * <p>
     * Also, rank-based filtering is only possible through exclusions file.
     *
     * @param src original excludes file (default of user defined)
     * @param exclude files to exclude (may be empty)
     * @param roots source directories (to resolve class files)
     * @param rank custom rank value (optional)
     */
    @SuppressWarnings('FileCreateTempFile')
    static File mergeExcludes(File src, Collection<File> exclude, Collection<File> roots, Integer rank = null) {
        Node xml = new XmlParser().parse(src)

        exclude.each {
            String clazz = FileUtils.extractJavaClass(roots, it)
            if (clazz) {
                xml.appendNode(MATCH).appendNode('Class')
                        .attributes().put('name', clazz)
            }
        }

        if (rank) {
            xml.appendNode(MATCH).appendNode('Rank')
                    .attributes().put('value', rank)
        }

        File tmp = File.createTempFile('spotbugs-extended-exclude', '.xml')
        tmp.deleteOnExit()
        tmp.withWriter { XmlUtil.serialize(xml, it) }
        return tmp
    }

    /**
     * @param project gradle project
     * @return true if spotbugs plugin enabled, false otherwise
     */
    static boolean isPluginEnabled(Project project) {
        return project.plugins.hasPlugin('com.github.spotbugs')
    }
}
