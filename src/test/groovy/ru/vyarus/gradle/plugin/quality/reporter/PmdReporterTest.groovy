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

            task testReport() {
                doLast {
                    new ru.vyarus.gradle.plugin.quality.tool.pmd.report.PmdReporter().report(pmdMain, 'main')
                }
            }
        """)
        file('src/main/java').mkdirs()
        String report = getClass().getResourceAsStream('/ru/vyarus/gradle/plugin/quality/report/pmd/main.xml').text
                .replaceAll('\\$\\{srcRoot\\}', file('src/main/java').canonicalPath.replaceAll('\\\\', '\\\\\\\\'))
        File target = file('build/reports/pmd/main.xml')
        target.parentFile.mkdirs()
        target << report

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        unifyString(error).contains """
13 PMD rule violations were found in 2 files

[Best Practices | UnusedPrivateField] sample.(Sample.java:8)
  Avoid unused private fields such as 'sample'.
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_bestpractices.html#unusedprivatefield

[Error Prone | AvoidFieldNameMatchingTypeName] sample.(Sample.java:8)
  It is somewhat confusing to have a field name matching the declaring class name
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_errorprone.html#avoidfieldnamematchingtypename

[Design | SingularField] sample.(Sample.java:8)
  Perhaps 'sample' could be replaced by a local variable.
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_design.html#singularfield

[Design | ImmutableField] sample.(Sample.java:8)
  Private field 'sample' could be made final; it is only initialized in the declaration or constructor.
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_design.html#immutablefield

[Code Style | MethodArgumentCouldBeFinal] sample.(Sample.java:10)
  Parameter 'sample' is not assigned and could be declared final
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_codestyle.html#methodargumentcouldbefinal

[Code Style | MethodArgumentCouldBeFinal] sample.(Sample.java:14)
  Parameter 'args' is not assigned and could be declared final
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_codestyle.html#methodargumentcouldbefinal

[Best Practices | SystemPrintln] sample.(Sample.java:16)
  System.out.println is used
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_bestpractices.html#systemprintln

[Best Practices | UnusedPrivateField] sample.(Sample2.java:8)
  Avoid unused private fields such as 'sample'.
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_bestpractices.html#unusedprivatefield

[Design | SingularField] sample.(Sample2.java:8)
  Perhaps 'sample' could be replaced by a local variable.
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_design.html#singularfield

[Design | ImmutableField] sample.(Sample2.java:8)
  Private field 'sample' could be made final; it is only initialized in the declaration or constructor.
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_design.html#immutablefield

[Code Style | MethodArgumentCouldBeFinal] sample.(Sample2.java:10)
  Parameter 'sample' is not assigned and could be declared final
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_codestyle.html#methodargumentcouldbefinal

[Code Style | MethodArgumentCouldBeFinal] sample.(Sample2.java:14)
  Parameter 'args' is not assigned and could be declared final
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_codestyle.html#methodargumentcouldbefinal

[Best Practices | SystemPrintln] sample.(Sample2.java:16)
  System.out.println is used
  https://pmd.github.io/pmd-6.0.1/pmd_rules_java_bestpractices.html#systemprintln
""" as String
    }
}