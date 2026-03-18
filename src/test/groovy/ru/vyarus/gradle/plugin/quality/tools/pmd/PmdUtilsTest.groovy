package ru.vyarus.gradle.plugin.quality.tools.pmd


import ru.vyarus.gradle.plugin.quality.tool.pmd.PmdUtils
import spock.lang.Specification

import java.nio.file.Files

/**
 * @author Vyacheslav Rusakov
 * @since 18.03.2026
 */
class PmdUtilsTest extends Specification {

    def "Check excludes in existing category"() {

        expect: "conversion"
        merge(['ExcludedRule'], ['ExcludedRule': 'category/java/documentation.xml']) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://pmd.sourceforge.net/ruleset/2.0.0" name="Base ruleset" xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <description>
        General Java quality rules.
  </description>
  <rule ref="category/java/documentation.xml">
    <exclude name="CommentRequired"/>
    <exclude name="ExcludedRule"/>
  </rule>
  <rule ref="category/java/errorprone.xml">
    <exclude name="AssignmentInOperand"/>
  </rule>
  <rule ref="category/java/errorprone.xml/CloseResource">
    <properties>
      <property name="types" value="java.sql.Connection,java.sql.Statement,java.sql.ResultSet"/>
    </properties>
  </rule>
  <rule ref="category/java/security.xml"/>
</ruleset>
""" as String
    }

    def "Check exclude of configured rule"() {

        expect: "conversion"
        merge(['CloseResource'], ['CloseResource': 'category/java/errorprone.xml']) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://pmd.sourceforge.net/ruleset/2.0.0" name="Base ruleset" xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <description>
        General Java quality rules.
  </description>
  <rule ref="category/java/documentation.xml">
    <exclude name="CommentRequired"/>
  </rule>
  <rule ref="category/java/errorprone.xml">
    <exclude name="AssignmentInOperand"/>
    <exclude name="CloseResource"/>
  </rule>
  <rule ref="category/java/security.xml"/>
</ruleset>
""" as String
    }

    def "Check excludes in existing empty category"() {

        expect: "conversion"
        merge(['ExcludedRule'], ['ExcludedRule': 'category/java/security.xml']) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://pmd.sourceforge.net/ruleset/2.0.0" name="Base ruleset" xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <description>
        General Java quality rules.
  </description>
  <rule ref="category/java/documentation.xml">
    <exclude name="CommentRequired"/>
  </rule>
  <rule ref="category/java/errorprone.xml">
    <exclude name="AssignmentInOperand"/>
  </rule>
  <rule ref="category/java/errorprone.xml/CloseResource">
    <properties>
      <property name="types" value="java.sql.Connection,java.sql.Statement,java.sql.ResultSet"/>
    </properties>
  </rule>
  <rule ref="category/java/security.xml">
    <exclude name="ExcludedRule"/>
  </rule>
</ruleset>
""" as String
    }

    def "Check excludes in non-existing category"() {

        expect: "conversion"
        merge(['ExcludedRule'], ['ExcludedRule': 'category/java/none.xml']) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://pmd.sourceforge.net/ruleset/2.0.0" name="Base ruleset" xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <description>
        General Java quality rules.
  </description>
  <rule ref="category/java/documentation.xml">
    <exclude name="CommentRequired"/>
  </rule>
  <rule ref="category/java/errorprone.xml">
    <exclude name="AssignmentInOperand"/>
  </rule>
  <rule ref="category/java/errorprone.xml/CloseResource">
    <properties>
      <property name="types" value="java.sql.Connection,java.sql.Statement,java.sql.ResultSet"/>
    </properties>
  </rule>
  <rule ref="category/java/security.xml"/>
  <rule ref="category/java/none.xml">
    <exclude name="ExcludedRule"/>
  </rule>
</ruleset>
""" as String
    }


    private String merge(List<String> excludes, Map<String, String> rules) {
        File tmp = Files.createTempFile("test", "pmd").toFile()
        tmp.text = new File(getClass().getResource('/ru/vyarus/gradle/plugin/quality/config/pmd/pmd.xml').toURI()).text
        PmdUtils.mergeExcludes(tmp, rules, excludes)

        def res = tmp.text.replace('\r', '')
        // on java 11 groovy inserts blank lines between tags
                .replaceAll('\n {1,}\n', '\n')
        tmp.delete()
        return res
    }
}
