package ru.vyarus.gradle.plugin.quality.util

import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSet

/**
 * Path utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 21.03.2017
 */
class FileUtils {

    /**
     * Searches for source set current task belongs to using task name build convention.
     *
     * @param baseName base name for task group
     * @param taskName full task name
     * @param sets source sets to search in
     * @return found source set or null when not found
     */
    static SourceSet findMatchingSet(String baseName, String taskName, Collection<SourceSet> sets) {
        return sets.find { it.getTaskName(baseName, null) == taskName }
    }

    /**
     * Resolves excluded files of file set.
     *
     * @param files files collection
     * @param excludes exclude patterns
     * @return set of excluded files
     */
    static Set<File> resolveIgnoredFiles(FileTree files, Collection<String> excludes) {
        excludes ? files.matching { include excludes }.files : Collections.emptyList()
    }

    /**
     * Resolves class name from source file.
     *
     * @param roots source directories
     * @param file java source file
     * @return class name
     */
    @SuppressWarnings('DuplicateStringLiteral')
    static String extractJavaClass(Collection<File> roots, File file) {
        String name = file.canonicalPath
        File root = roots.find { name.startsWith(it.canonicalPath) }
        if (!root) {
            return null
        }
        name = name[root.canonicalPath.length() + 1..-1] // remove sources dir prefix
        name = name[0..name.lastIndexOf('.') - 1] // remove extension
        name.replaceAll('\\\\|/', '.')
    }
}
