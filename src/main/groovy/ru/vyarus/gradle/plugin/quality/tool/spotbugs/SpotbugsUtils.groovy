package ru.vyarus.gradle.plugin.quality.tool.spotbugs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskCollection
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.util.FileUtils
import ru.vyarus.gradle.plugin.quality.util.SourceSetUtils

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
     * Searches for applied spotbugs plugin (plugin must be present in buildscript classpath and may not be applied).
     *
     * @param project project
     * @return spotbugs plugin class or null if plugin is not available
     */
    static Class<? extends Plugin> findPluginClass(Project project) {
        try {
            // plugin registered, but not applied (apply false)
            return project.buildscript.classLoader
                    .loadClass('com.github.spotbugs.snom.SpotBugsPlugin') as Class<? extends Plugin>
        } catch (ClassNotFoundException ignored) {
        }
        return null
    }

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
     * Spotbugs use enums for configuration since plugin version 6.x., but 5.x use pure strings (through method).
     * Enum conversion would work for both plugin versions.
     *
     * @param plugin spotbugs plugin instance
     * @param name enum name (without package)
     * @param value configured value
     * @return enum or string value
     */
    static Object enumValue(Class plugin, String name, String value) {
        Class type = plugin.classLoader.loadClass("com.github.spotbugs.snom.$name")
        type.getMethod('valueOf', String).invoke(null, value.toUpperCase())
    }

    /**
     * SpotBugsPlugin applies all spotbugs tasks as check task dependencies. This method looks modifies check task
     * dependsOn collection (override it) in order to remove some spotbugs tasks.
     *
     * @param check check task
     * @param project project
     * @param extension extension
     * @param spotbugsTaskType spotbugs task class
     */
    @SuppressWarnings('Instanceof')
    static void fixCheckDependencies(Project project, QualityExtension extension, Class spotbugsTaskType) {
        project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME)
                .configure { check ->
                    // all tasks that should be assigned to check
                    List<String> requiredTasks = SourceSetUtils
                            .getSourceSets(project, extension.sourceSets.get())*.getTaskName(SpotbugsTool.NAME, null)
                    // tasks already assigned to check, but not required
                    List<String> toRemove = project.tasks.withType(spotbugsTaskType).matching { Task t ->
                        !requiredTasks.contains(t.name)
                    }*.name
                    if (toRemove.empty) {
                        return
                    }

                    List depends = check.dependsOn.asList()
                    // SpotBugsPlugin applies tasks like thins:
                    // it.dependsOn(project.tasks.withType(SpotBugsTask::class.java))
                    // so we search and remove this container
                    depends.removeIf {
                        it instanceof TaskCollection && (it as TaskCollection<Task>)
                                .find { it.name.startsWith(SpotbugsTool.NAME) }
                    }
                    // no remove verification because in the latest plugin there is an option to disable tasks addition
                    // which means the absence of configured tasks might be expected behaviour

                    // replace tasks applied by SpotBugsPlugin with actually required tasks
                    depends.add(requiredTasks)
                    // override depends on (important to not use method here which will append new collection!)
                    check.dependsOn = depends
                }
    }

    /**
     * Extend exclusions filter file when exclusions are required. Note: it is assumed that tmp file was already
     * created (because it is impossible to configure different file on this stage).
     * <p>
     * Apt sources are also counted (to be able to ignore apt sources).
     *
     * @param task spotbugs task
     * @param extension extension instance
     * @param logger project logger for error messages
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('ParameterCount')
    static void replaceExcludeFilter(File file, Set<File> aptGenerated, Set<File> sourceDirs,
                                     Integer configuredRank, List<String> excludes, FileCollection excludeSources,
                                     ObjectFactory factory) {
        // setting means max allowed rank, but filter evicts all ranks >= specified (so +1)
        Integer rank = configuredRank < MAX_RANK ? configuredRank + 1 : null

        Set<File> ignored = []
        if (excludes) {
            sourceDirs.each {
                ConfigurableFileTree tree = factory.fileTree().from(it)
                ignored.addAll(FileUtils.resolveIgnoredFiles(tree, excludes))
            }
        }

        // exclude all apt-generated files
        aptGenerated.each {
            ConfigurableFileTree tree = factory.fileTree().from(it)
            ignored.addAll(tree.filter { it.path.endsWith('.java') }.files)
        }
        if (excludeSources) {
            // add directly excluded files
            ignored.addAll(excludeSources.asFileTree.matching { include '**/*.java' }.files)
        }
        if (!ignored && !rank) {
            // no custom excludes required
            return
        }

        Collection<File> sources = []
        sources.addAll(sourceDirs)
        sources.addAll(aptGenerated)

        mergeExcludes(file, ignored, sources, rank)
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
    static void mergeExcludes(File src, Collection<File> exclude, Collection<File> roots, Integer rank = null) {
        Node xml = new XmlParser().parse(src)
        exclude.each {
            String clazz = FileUtils.extractJavaClass(roots, it)
            if (clazz) {
                // use regexp to also exclude inner classes
                xml.appendNode(MATCH).appendNode('Class', ['name': "~${clazz.replace('.', '\\.')}(\\\$.*)?"])
            }
        }

        if (rank) {
            xml.appendNode(MATCH).appendNode('Rank', ['value': rank])
        }

        Writer writer = src.newWriter(false)
        writer.writeLine('<?xml version="1.0" encoding="UTF-8"?>')

        Writer sw = new StringWriter()
        XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(sw))
        printer.print(xml)
        writer.write(sw.toString())

        // not direct serialization to avoid blank lines
//        XmlUtil.serialize(xml, writer)
        writer.flush()
    }
}
