package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
class CodeNarcReporterTest extends AbstractKitTest {

    def "Check codenarc report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            repositories {jcenter()}

            task testReport() << {
                new ru.vyarus.gradle.plugin.quality.report.CodeNarcReporter().report(project, 'main')
            }
        """)
        file('src/main/groovy').mkdirs()
        fileFromClasspath('build/reports/codenarc/main.xml', '/ru/vyarus/gradle/plugin/quality/report/codenarc/main.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        error.replaceAll("\r", "").contains """
24 (0 / 10 / 14) CodeNarc violations were found in 2 files

[Formatting | ClassJavadoc] sample.(GSample.groovy:3)  [priority 2]
\t>> class GSample {
  Class sample.GSample missing Javadoc
  Makes sure each class and interface definition is preceded by javadoc. Enum definitions are not checked, due to strange behavior in the Groovy AST.
  http://codenarc.sourceforge.net/codenarc-rules-formatting.html#ClassJavadoc

[Convention | NoDef] sample.(GSample.groovy:5)  [priority 3]
\t>> def foo(String bar) {
  def should not be used
  def should not be used. You should replace it with concrete type.
  http://codenarc.sourceforge.net/codenarc-rules-convention.html#NoDef

[Convention | NoDef] sample.(GSample.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  def should not be used
  def should not be used. You should replace it with concrete type.
  http://codenarc.sourceforge.net/codenarc-rules-convention.html#NoDef

[Dry | DuplicateStringLiteral] sample.(GSample.groovy:6)  [priority 2]
\t>> def res = "123" + bar + "123";
  Duplicate String Literal: 123
  Code containing duplicate String literals can usually be improved by declaring the String as a constant field. The ignoreStrings property () can optionally specify a comma-separated list of Strings to ignore.
  http://codenarc.sourceforge.net/codenarc-rules-dry.html#DuplicateStringLiteral

[Unnecessary | UnnecessaryGString] sample.(GSample.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessaryGString

[Unnecessary | UnnecessaryGString] sample.(GSample.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessaryGString

[Unnecessary | UnnecessarySemicolon] sample.(GSample.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  Semi-colons as line endings can be removed safely
  Semicolons as line terminators are not required in Groovy: remove them. Do not use a semicolon as a replacement for empty braces on for and while loops; this is a confusing practice.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessarySemicolon

[Unused | UnusedVariable] sample.(GSample.groovy:6)  [priority 2]
\t>> def res = "123" + bar + "123";
  The variable [res] in class sample.GSample is not used
  Checks for variables that are never referenced. The ignoreVariableNames property (null) specifies one or more variable names that should be ignored, optionally containing wildcard characters ('*' or '?').
  http://codenarc.sourceforge.net/codenarc-rules-unused.html#UnusedVariable

[Braces | IfStatementBraces] sample.(GSample.groovy:7)  [priority 2]
\t>> if (bar)
  The if statement lacks braces
  Use braces for if statements, even for a single statement.
  http://codenarc.sourceforge.net/codenarc-rules-braces.html#IfStatementBraces

[Dry | DuplicateStringLiteral] sample.(GSample.groovy:8)  [priority 2]
\t>> return "123"
  Duplicate String Literal: 123
  Code containing duplicate String literals can usually be improved by declaring the String as a constant field. The ignoreStrings property () can optionally specify a comma-separated list of Strings to ignore.
  http://codenarc.sourceforge.net/codenarc-rules-dry.html#DuplicateStringLiteral

[Unnecessary | UnnecessaryGString] sample.(GSample.groovy:8)  [priority 3]
\t>> return "123"
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessaryGString

[Formatting | FileEndsWithoutNewline] sample.(GSample.groovy:9)  [priority 3]
\t>> }
  File GSample.groovy does not end with a newline
  Makes sure the source code file ends with a newline character.
  http://codenarc.sourceforge.net/codenarc-rules-formatting.html#FileEndsWithoutNewline

[Formatting | ClassJavadoc] sample.(GSample2.groovy:3)  [priority 2]
\t>> class GSample2 {
  Class sample.GSample2 missing Javadoc
  Makes sure each class and interface definition is preceded by javadoc. Enum definitions are not checked, due to strange behavior in the Groovy AST.
  http://codenarc.sourceforge.net/codenarc-rules-formatting.html#ClassJavadoc

[Convention | NoDef] sample.(GSample2.groovy:5)  [priority 3]
\t>> def foo(String bar) {
  def should not be used
  def should not be used. You should replace it with concrete type.
  http://codenarc.sourceforge.net/codenarc-rules-convention.html#NoDef

[Convention | NoDef] sample.(GSample2.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  def should not be used
  def should not be used. You should replace it with concrete type.
  http://codenarc.sourceforge.net/codenarc-rules-convention.html#NoDef

[Dry | DuplicateStringLiteral] sample.(GSample2.groovy:6)  [priority 2]
\t>> def res = "123" + bar + "123";
  Duplicate String Literal: 123
  Code containing duplicate String literals can usually be improved by declaring the String as a constant field. The ignoreStrings property () can optionally specify a comma-separated list of Strings to ignore.
  http://codenarc.sourceforge.net/codenarc-rules-dry.html#DuplicateStringLiteral

[Unnecessary | UnnecessaryGString] sample.(GSample2.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessaryGString

[Unnecessary | UnnecessaryGString] sample.(GSample2.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessaryGString

[Unnecessary | UnnecessarySemicolon] sample.(GSample2.groovy:6)  [priority 3]
\t>> def res = "123" + bar + "123";
  Semi-colons as line endings can be removed safely
  Semicolons as line terminators are not required in Groovy: remove them. Do not use a semicolon as a replacement for empty braces on for and while loops; this is a confusing practice.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessarySemicolon

[Unused | UnusedVariable] sample.(GSample2.groovy:6)  [priority 2]
\t>> def res = "123" + bar + "123";
  The variable [res] in class sample.GSample2 is not used
  Checks for variables that are never referenced. The ignoreVariableNames property (null) specifies one or more variable names that should be ignored, optionally containing wildcard characters ('*' or '?').
  http://codenarc.sourceforge.net/codenarc-rules-unused.html#UnusedVariable

[Braces | IfStatementBraces] sample.(GSample2.groovy:7)  [priority 2]
\t>> if (bar)
  The if statement lacks braces
  Use braces for if statements, even for a single statement.
  http://codenarc.sourceforge.net/codenarc-rules-braces.html#IfStatementBraces

[Dry | DuplicateStringLiteral] sample.(GSample2.groovy:8)  [priority 2]
\t>> return "123"
  Duplicate String Literal: 123
  Code containing duplicate String literals can usually be improved by declaring the String as a constant field. The ignoreStrings property () can optionally specify a comma-separated list of Strings to ignore.
  http://codenarc.sourceforge.net/codenarc-rules-dry.html#DuplicateStringLiteral

[Unnecessary | UnnecessaryGString] sample.(GSample2.groovy:8)  [priority 3]
\t>> return "123"
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  http://codenarc.sourceforge.net/codenarc-rules-unnecessary.html#UnnecessaryGString

[Formatting | FileEndsWithoutNewline] sample.(GSample2.groovy:9)  [priority 3]
\t>> }
  File GSample2.groovy does not end with a newline
  Makes sure the source code file ends with a newline character.
  http://codenarc.sourceforge.net/codenarc-rules-formatting.html#FileEndsWithoutNewline
""" as String
    }
}