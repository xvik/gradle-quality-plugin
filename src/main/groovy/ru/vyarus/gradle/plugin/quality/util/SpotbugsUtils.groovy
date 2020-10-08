package ru.vyarus.gradle.plugin.quality.util

import com.github.spotbugs.snom.SpotBugsTask
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlUtil
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
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
     * Spotbugs task properties may be configured only once. At that time it is too early to compute exact file,
     * but we can assume that if excludes configured then temp file would be required. So preparing temp file
     * ahead of time. Later, it would be filled with actual exclusions (if anything matches).
     *
     * @param taskName spotbugs task name
     * @param extension extension
     * @param configured configured exclusions file (most likely default one)
     * @return excludes file for task configuration
     */
    @SuppressWarnings('FileCreateTempFile')
    static File excludesFile(String taskName, QualityExtension extension, File configured) {
        // spotbugs does not support exclude of SourceTask, so appending excluded classes to
        // xml exclude filter
        // for custom rank appending extra rank exclusion rule
        if (extension.exclude || extension.excludeSources || extension.spotbugsMaxRank < MAX_RANK) {
            File tmp = File.createTempFile(taskName + '-extended-exclude', '.xml')
            tmp.deleteOnExit()
            tmp << configured.text
            return tmp
        }
        return configured
    }

    /**
     * Extend exclusions filter file with when exclusions are required. Note: it is assumed that tmp file was already
     * created (because it is impossible to configure different file on this stage).
     * <p>
     * Apt sources are also counted (to be able to ignore apt sources).
     *
     * @param task spotbugs task
     * @param extension extension instance
     * @param logger project logger for error messages
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static void replaceExcludeFilter(SpotBugsTask task, QualityExtension extension, Logger logger) {
        // setting means max allowed rank, but filter evicts all ranks >= specified (so +1)
        Integer rank = extension.spotbugsMaxRank < MAX_RANK ? extension.spotbugsMaxRank + 1 : null

        SourceSet set = FileUtils.findMatchingSet('spotbugs', task.name, extension.sourceSets)
        if (!set) {
            logger.error("[SpotBugs] Failed to find source set for task ${task.name}: exclusions " +
                    ' will not be applied')
            return
        }
        // apt is a special dir, not mentioned in sources!
        File aptGenerated = (task.project.tasks.findByName(set.compileJavaTaskName) as JavaCompile)
                .options.annotationProcessorGeneratedSourcesDirectory

        Set<File> ignored = FileUtils.resolveIgnoredFiles(task.sourceDirs.asFileTree, extension.exclude)
        ignored.addAll(FileUtils.resolveIgnoredFiles(task.project.fileTree(aptGenerated), extension.exclude))
        if (extension.excludeSources) {
            // add directly excluded files
            ignored.addAll(extension.excludeSources.asFileTree.matching { include '**/*.java' }.files)
        }
        if (!ignored && !rank) {
            // no custom excludes required
            return
        }

        Collection<File> sources = []
        sources.addAll(set.allJava.srcDirs)
        sources.add(aptGenerated)

        mergeExcludes(task.excludeFilter.get().asFile, ignored, sources, rank)
    }

    /**
     * Spotbugs task is a {@link org.gradle.api.tasks.SourceTask}, but does not properly support exclusions.
     * To overcome this limitation, source exclusions could be added to spotbugs exclusions filter xml file.
     * <p>
     * Also, rank-based filtering is only possible through exclusions file.
     *
     * @param exclusions file to be extended (already tmp file)
     * @param exclude files to exclude (may be empty)
     * @param roots source directories (to resolve class files)
     * @param rank custom rank value (optional)
     */
    @SuppressWarnings('FileCreateTempFile')
    static void mergeExcludes(File src, Collection<File> exclude, Collection<File> roots, Integer rank = null) {
        Node xml = new XmlParser().parse(src)

        exclude.each {
            String clazz = FileUtils.extractJavaClass(roots, it)
            if (clazz) {
                xml.appendNode(MATCH).appendNode('Class', ['name': clazz])
            }
        }

        if (rank) {
            xml.appendNode(MATCH).appendNode('Rank', ['value': rank])
        }

        Writer writer = src.newWriter()
        XmlUtil.serialize(xml, writer)
        writer.flush()
    }

    /**
     * @param project gradle project
     * @return true if spotbugs plugin enabled, false otherwise
     */
    static boolean isPluginEnabled(Project project) {
        return project.plugins.hasPlugin('com.github.spotbugs')
    }
}
