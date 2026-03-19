package ru.vyarus.gradle.plugin.quality.tool.codenarc

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.util.FileUtils

import java.nio.file.Path

/**
 * Codenarc related utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 19.03.2026
 */
@CompileStatic(TypeCheckingMode.SKIP)
class CodenarcUtils {

    /**
     * Codenarc rules description files are stored in jar (multiple xml files under rulesets/).
     * To disable rule by name we need to know its full path. This method resolves all declared rules with their
     * path.
     * <p>
     * Also, codenarc use rule class in declaration files, so we need an additional file with user rule names mappings
     * into rule class names (same file used for reporting).
     *
     * @param project project instance
     * @param ruleClasses codenarc rule names mapping to rule class names
     * @return rule name -- category path (e.g. rulesets/security.xml) map
     */
    static Map<String, String> readExistingRules(Project project, Properties ruleClasses) {
        // rule class -- rule name
        Map<String, String> revIndex = [:]
        ruleClasses.each { revIndex.put(it.value as String, it.key as String) }

        Map<String, String> res = [:]
        File jar = project.configurations.getByName('codenarc').resolve()
                .find { it.name.startsWith('CodeNarc') }
        FileUtils.loadFilesFromJar(jar, '/rulesets') { Path file, InputStream desc ->
            new XmlParser().parse(desc).rule.each {
                String rule = revIndex[it.@class]
                res[rule] = file.toString().substring(1)
//                println "${file.toString()}/${rule}"
            }
        }
        res
    }

    /**
     * Modify rules file by applying required exclusions. To exclude rule in codenarc we need to know full rule path
     * (rules declared in different xml files).
     * <p>
     * Having all possible rules declarations, we could validate required exclusions correctness.
     * Rule exclude must be applied under the correct group (declaration file): this group might exists or not.
     * Also, it's possible that removed rule is explicitly configured and this custom configuration must be removed.
     *
     * @param src configuration file
     * @param rules all known codenarc rules (for current codenarc version)
     * @param excluded rule names to exclude
     */
    @SuppressWarnings(['MethodSize', 'Println', 'SpaceAfterOpeningBrace', 'SpaceBeforeClosingBrace'])
    static void mergeExcludes(File src, Map<String, String> rules, List<String> excluded) {
        List<String> toExclude = []
        // rules to exclude grouped by group (to cleanup rule customizations)
        Map<String, List<String>> groupRules = [:]
        // group name - xml node, related to this group
        Map<String, Node> targetGroups = [:]
        excluded.each {
            String group = rules[it]
            if (group) {
                toExclude.add(it)
                groupRules.putIfAbsent(group, [])
                groupRules[group].add(it)
                targetGroups.putIfAbsent(group, null)
            } else {
                println "[quality] Can't suppress codenarc rule $it because it does not exists"
            }
        }

        if (toExclude.empty) {
            // nothing to do
            return
        }

        Node xml = new XmlParser().parse(src)

        // There might be 3 cases:
        // - <ruleset-ref path='rulesets/formatting.xml'> already declared and need only to put exclusion inside of it
        // - <rule-config name="SpaceAroundMapEntryColon"> rule explicitly configured inside group and need to
        // remove it
        // - no declaration for category - need to create both root node and exclusion

        // So first, parse the entire file to find all occurrences of required groups and remove individual nodes
        // Next, add excludes to groups (if required, create new groups)

        //  <ruleset-ref path='rulesets/formatting.xml'>
        xml.'ruleset-ref'.each {
            String ref = it.@path
            if (targetGroups.containsKey(ref)) {
                // category node
                targetGroups[ref] = it

                // cleanup <rule-config name="SpaceAroundMapEntryColon">
                it.'rule-config'.each { rule ->
                    String name = rule.@name
                    if (groupRules[ref].contains(name)) {
                        rule.replaceNode {}
                    }
                }
            }
        }

        // create missed nodes, if required
        targetGroups.keySet().each {
            targetGroups[it] = targetGroups[it] ?: xml.appendNode('ruleset-ref', ['path': it])
        }

        // apply excludes for required rules
        toExclude.each {
            String group = rules[it]
            targetGroups[group].appendNode('exclude', ['name': it])
        }

        println "[quality] suppressed codenarc rules: ${toExclude.join(', ')}"

        Writer writer = src.newWriter(false)
        Writer sw = new StringWriter()
        XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(sw))
        printer.print(xml)
        writer.write(sw.toString())

        // not direct serialization to avoid blank lines
//        XmlUtil.serialize(xml, writer)
        writer.flush()
    }
}
