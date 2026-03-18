package ru.vyarus.gradle.plugin.quality.util

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSet

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.BiConsumer
import java.util.stream.Stream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Path utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 21.03.2017
 */
@CompileStatic
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
    @CompileStatic(TypeCheckingMode.SKIP)
    static Set<File> resolveIgnoredFiles(FileTree files, Collection<String> excludes) {
        excludes ? new HashSet(files.matching { include excludes }.files) : []
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

    /**
     * Finds exact jar in gradle configuration.
     *
     * @param project project to look configurations in
     * @param configurationName configuration name
     * @param jarIdentity part of jar name to identify it
     * @return found jar file
     * @throws IllegalArgumentException if jar not found
     */
    static File findConfigurationJar(Project project, String configurationName, String jarIdentity) {
        File jar = project.configurations.getByName(configurationName).files.find {
            it.name.contains(jarIdentity)
        }
        if (jar == null) {
            throw new IllegalArgumentException("$jarIdentity jar not found in configuration $configurationName")
        }
        return jar
    }

    /**
     * Loads file from provided jar file (found with
     * {@link #findConfigurationJar(org.gradle.api.Project, java.lang.String, java.lang.String)}).
     * <p>
     * Action closure receives input stream (and so could load file contents and handle it).
     * Opened input stream is always closed after closure execution.
     *
     * @param jar jar file to search in
     * @param filePath file path inside jar
     * @param action closure to handle file contents
     * @return value returned by action closure
     * @throws IllegalArgumentException if file not found in jar
     */
    static <T> T loadFileFromJar(File jar, String filePath, Closure<T> action) {
        ZipFile file = new ZipFile(jar)
        ZipEntry entry = file.getEntry(filePath)
        if (entry == null) {
            throw new IllegalArgumentException("File not found in $jar.name: $filePath")
        }
        try {
            return action.call(file.getInputStream(entry))
        } finally {
            // close all related zip streams
            file.close()
        }
    }

    /**
     * Process all xml files in directory inside jar.
     *
     * @param jar jar file
     * @param path directory path inside jar
     * @param action file action
     */
    @SuppressWarnings(['CyclomaticComplexity', 'NestedBlockDepth'])
    static void loadFilesFromJar(File jar, String path, BiConsumer<Path, InputStream> action) {
        try (FileSystem zipFs = FileSystems.newFileSystem(jar.toPath())) {
            Path root = zipFs.getPath(path)

            try (Stream<Path> pathStream = Files.walk(root, 1)) {
                pathStream.forEach(file -> {
                    if (!Files.isDirectory(file) && file.fileName.toString().endsWith('.xml')) {
                        try (InputStream stream = Files.newInputStream(file)) {
                            action.accept(file, stream)
                        }
                    }
                })
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read files inside jar file path $jar.name: $path", e)
        }
    }
}
