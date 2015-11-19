package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
class PmdReporterTest extends AbstractKitTest {

    def "Check pmd report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
            }

            task testReport() << {
                new ru.vyarus.gradle.plugin.quality.report.PmdReporter().report(project, 'main')
            }
        """)
        file('src/main/java').mkdirs()
        fileFromClasspath('build/reports/pmd/main.xml', '/ru/vyarus/gradle/plugin/quality/report/pmd/main.xml')

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.standardError

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        error.replaceAll("\r", "") == """
15 PMD rule violations were found in 2 files

[Unused Code | UnusedPrivateField] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:5
  Avoid unused private fields such as 'sample'.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/unusedcode.html#UnusedPrivateField

[Naming | AvoidFieldNameMatchingTypeName] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:5
  It is somewhat confusing to have a field name matching the declaring class name
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/naming.html#AvoidFieldNameMatchingTypeName

[Design | SingularField] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:5
  Perhaps 'sample' could be replaced by a local variable.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#SingularField

[Design | ImmutableField] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:5
  Private field 'sample' could be made final; it is only initialized in the declaration or constructor.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#ImmutableField

[Optimization | MethodArgumentCouldBeFinal] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:7
  Parameter 'sample' is not assigned and could be declared final
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/optimizations.html#MethodArgumentCouldBeFinal

[Design | UseVarargs] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:11
  Consider using varargs for methods or constructors which take an array the last parameter.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#UseVarargs

[Design | UncommentedEmptyMethodBody] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:11-13
  Document empty method body
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#UncommentedEmptyMethodBody

[Optimization | MethodArgumentCouldBeFinal] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample:11
  Parameter 'args' is not assigned and could be declared final
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/optimizations.html#MethodArgumentCouldBeFinal

[Unused Code | UnusedPrivateField] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:5
  Avoid unused private fields such as 'sample'.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/unusedcode.html#UnusedPrivateField

[Design | SingularField] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:5
  Perhaps 'sample' could be replaced by a local variable.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#SingularField

[Design | ImmutableField] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:5
  Private field 'sample' could be made final; it is only initialized in the declaration or constructor.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#ImmutableField

[Optimization | MethodArgumentCouldBeFinal] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:7
  Parameter 'sample' is not assigned and could be declared final
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/optimizations.html#MethodArgumentCouldBeFinal

[Design | UseVarargs] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:11
  Consider using varargs for methods or constructors which take an array the last parameter.
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#UseVarargs

[Design | UncommentedEmptyMethodBody] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:11-13
  Document empty method body
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/design.html#UncommentedEmptyMethodBody

[Optimization | MethodArgumentCouldBeFinal] C:.Users.xvik.AppData.Local.Temp.junit1006517273776660000.src.main.java.sample.Sample2:11
  Parameter 'args' is not assigned and could be declared final
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/optimizations.html#MethodArgumentCouldBeFinal
""" as String
    }
}