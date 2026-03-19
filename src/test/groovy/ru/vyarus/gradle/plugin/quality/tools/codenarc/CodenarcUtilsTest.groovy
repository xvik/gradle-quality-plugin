package ru.vyarus.gradle.plugin.quality.tools.codenarc

import ru.vyarus.gradle.plugin.quality.tool.codenarc.CodenarcUtils
import spock.lang.Specification

import java.nio.file.Files

/**
 * @author Vyacheslav Rusakov
 * @since 19.03.2026
 */
class CodenarcUtilsTest extends Specification {
    def "Check excludes in existing category"() {

        expect: "conversion"
        merge(['ExcludedRule'], ['ExcludedRule': 'rulesets/basic.xml']) ==
                """<ruleset xmlns="http://codenarc.org/ruleset/1.0" xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://codenarc.org/ruleset-schema.xsd">
  <ruleset-ref path="rulesets/basic.xml">
    <exclude name="CommentRequired"/>
    <exclude name="ExcludedRule"/>
  </ruleset-ref>
  <ruleset-ref path="rulesets/formatting.xml">
    <rule-config name="SpaceAroundMapEntryColon">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
    <rule-config name="SpaceAroundMapEntryColon2">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
  </ruleset-ref>
  <ruleset-ref path="rulesets/comments.xml"/>
</ruleset>
""" as String
    }

    def "Check exclude of configured rule"() {

        expect: "conversion"
        merge(['SpaceAroundMapEntryColon'], ['SpaceAroundMapEntryColon': 'rulesets/formatting.xml']) ==
                """<ruleset xmlns="http://codenarc.org/ruleset/1.0" xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://codenarc.org/ruleset-schema.xsd">
  <ruleset-ref path="rulesets/basic.xml">
    <exclude name="CommentRequired"/>
  </ruleset-ref>
  <ruleset-ref path="rulesets/formatting.xml">
    <rule-config name="SpaceAroundMapEntryColon2">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
    <exclude name="SpaceAroundMapEntryColon"/>
  </ruleset-ref>
  <ruleset-ref path="rulesets/comments.xml"/>
</ruleset>
""" as String
    }

    def "Check excludes in existing empty category"() {

        expect: "conversion"
        merge(['ExcludedRule'], ['ExcludedRule': 'rulesets/comments.xml']) ==
                """<ruleset xmlns="http://codenarc.org/ruleset/1.0" xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://codenarc.org/ruleset-schema.xsd">
  <ruleset-ref path="rulesets/basic.xml">
    <exclude name="CommentRequired"/>
  </ruleset-ref>
  <ruleset-ref path="rulesets/formatting.xml">
    <rule-config name="SpaceAroundMapEntryColon">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
    <rule-config name="SpaceAroundMapEntryColon2">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
  </ruleset-ref>
  <ruleset-ref path="rulesets/comments.xml">
    <exclude name="ExcludedRule"/>
  </ruleset-ref>
</ruleset>
""" as String
    }

    def "Check excludes in non-existing category"() {

        expect: "conversion"
        merge(['ExcludedRule'], ['ExcludedRule': 'rulesets/unknown.xml']) ==
                """<ruleset xmlns="http://codenarc.org/ruleset/1.0" xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://codenarc.org/ruleset-schema.xsd">
  <ruleset-ref path="rulesets/basic.xml">
    <exclude name="CommentRequired"/>
  </ruleset-ref>
  <ruleset-ref path="rulesets/formatting.xml">
    <rule-config name="SpaceAroundMapEntryColon">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
    <rule-config name="SpaceAroundMapEntryColon2">
      <property name="characterAfterColonRegex" value="\\s"/>
    </rule-config>
  </ruleset-ref>
  <ruleset-ref path="rulesets/comments.xml"/>
  <ruleset-ref path="rulesets/unknown.xml">
    <exclude name="ExcludedRule"/>
  </ruleset-ref>
</ruleset>
""" as String
    }


    private String merge(List<String> excludes, Map<String, String> rules) {
        File tmp = Files.createTempFile("test", "codenarc").toFile()
        tmp.text = new File(getClass().getResource('/ru/vyarus/gradle/plugin/quality/config/codenarc/codenarc.xml').toURI()).text
        CodenarcUtils.mergeExcludes(tmp, rules, excludes)

        def res = tmp.text.replace('\r', '')
        // on java 11 groovy inserts blank lines between tags
                .replaceAll('\n {1,}\n', '\n')
        tmp.delete()
        return res
    }
}
