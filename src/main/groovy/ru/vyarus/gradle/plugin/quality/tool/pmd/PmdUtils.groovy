package ru.vyarus.gradle.plugin.quality.tool.pmd

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.util.FileUtils

import java.nio.file.Path

/**
 * Pmd helper utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 18.03.2026
 */
@CompileStatic(TypeCheckingMode.SKIP)
class PmdUtils {

    /**
     * Pmd java rules description files are stored in pmd-java jar (multiple xml files under category/java).
     * To disable rule by name we need to know its full path. This method resolves all declared rules with their
     * path.
     *
     * @param project project instance
     * @return rule name -- category path (e.g. category/errorprone.xml) map
     */
    static Map<String, String> readExistingRules(Project project) {
        Map<String, String> res = [:]
        File jar = project.configurations.getByName('pmd').resolve().find { it.name.contains('pmd-java') }
        FileUtils.loadFilesFromJar(jar, '/category/java') { Path file, InputStream desc ->
            new XmlParser().parse(desc).rule.each {
                res[it.@name] = file.toString().substring(1)
//                println "${file.toString()}/${it.@name}"
            }
        }
        res
    }

    /**
     * Modify rules file by applying required exclusions. To exclude rule in pmd we need to know full rule path
     * (rules declared in different xml files).
     * <p>
     * Having all possible rules declarations, we could validate required exclusions correctness.
     * Rule exclude must be applied under the correct group (declaration file): this group might exists or not.
     * Also, it's possible that removed rule is explicitly configured and this custom configuration must be removed.
     *
     * @param src configuration file
     * @param rules all known pmd rules (for current pmd version)
     * @param excluded rule names to exclude
     */
    @SuppressWarnings(['MethodSize', 'Println', 'SpaceAfterOpeningBrace', 'SpaceBeforeClosingBrace'])
    static void mergeExcludes(File src, Map<String, String> rules, List<String> excluded) {
        List<String> toExclude = []
        List<String> fullNames = []
        // group name - xml node, related to this group
        Map<String, Node> targetGroups = [:]
        excluded.each {
            String group = rules[it]
            if (group) {
                toExclude.add(it)
                fullNames.add("$group/$it".toString())
                targetGroups.putIfAbsent(group, null)
            } else {
                println "[quality] Can't suppress pmd rule $it because it does not exists"
            }
        }

        if (toExclude.empty) {
            // nothing to do
            return
        }

        Node xml = new XmlParser().parse(src)

        // There might be 3 cases:
        // - <rule ref="category/java/codestyle.xml"> already declared and need only to put exclusion inside of it
        // - <rule ref="category/java/codestyle.xml/FieldNamingConventions"> rule explicitly configured and need to
        // remove it
        // - no declaration for category - need to create both root node and exclusion

        // So first, parse the entire file to find all occurrences of required groups and remove individual nodes
        // Next, add excludes to groups (if required, create new groups)

        //  <rule ref="category/java/design.xml">
        xml.rule.each {
            String ref = it.@ref
            if (fullNames.contains(ref)) {
                // custom rule configuration - remove
                it.replaceNode {}
            } else if (targetGroups.containsKey(ref)) {
                // category node
                targetGroups[ref] = it
            }
        }

        // create missed nodes, if required
        targetGroups.keySet().each {
            targetGroups[it] = targetGroups[it] ?: xml.appendNode('rule', ['ref': it])
        }

        // apply excludes for required rules
        toExclude.each {
            String group = rules[it]
            targetGroups[group].appendNode('exclude', ['name': it])
            println "[quality] suppressed pmd rule: ($group) $it"
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
