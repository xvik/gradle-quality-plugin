# Java project sample

For java projects Checkstyle, PMD and SpotBugs tools are applied.

Spotbugs plugin must be manually added to build classpath! (checkstyle and PMD plugins are bundled with gradle)

## Output

```
> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :copyQualityConfigs

> Task :checkstyleMain
Checkstyle rule violations were found. See the report at: file:///home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/java/build/reports/checkstyle/main.html
Checkstyle files with violations: 1
Checkstyle violations by severity: [error:2]


> Task :pmdMain
6 PMD rule violations were found. See the report at: file:///home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/java/build/reports/pmd/main.html

6 PMD rule violations were found in 1 files

[Best Practices | UnusedPrivateField] sample.(Sample.java:8)
  Avoid unused private fields such as 'sample'.
  https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_bestpractices.html#unusedprivatefield

[Design | ImmutableField] sample.(Sample.java:8)
  Field 'sample' may be declared final
  https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_design.html#immutablefield

[Error Prone | AvoidFieldNameMatchingTypeName] sample.(Sample.java:8)
  It is somewhat confusing to have a field name matching the declaring class name
  https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_errorprone.html#avoidfieldnamematchingtypename

[Code Style | MethodArgumentCouldBeFinal] sample.(Sample.java:10)
  Parameter 'sample' is not assigned and could be declared final
  https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_codestyle.html#methodargumentcouldbefinal

[Code Style | UseDiamondOperator] sample.(Sample.java:15)
  Raw type use may be avoided by using a diamond: `new ArrayDeque<>()`
  https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_codestyle.html#usediamondoperator

[Best Practices | SystemPrintln] sample.(Sample.java:16)
  Usage of System.out/err
  https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_bestpractices.html#systemprintln


> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE

> Task :checkstyleMain

2 Checkstyle rule violations were found in 1 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:1)
  File does not end with a newline.
  https://checkstyle.sourceforge.io/checks/misc/newlineatendoffile.html

[Javadoc | MissingJavadocType] sample.(Sample.java:6)
  Missing a Javadoc comment.
  https://checkstyle.sourceforge.io/checks/javadoc/missingjavadoctype.html


> Task :spotbugsMain
SpotBugs ended with exit code 1

1 (0 / 1 / 0) SpotBugs violations were found in 1 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:11)  [priority 2 / rank 18]
	>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.
  See CWE-563: Assignment to Variable without Use.

SpotBugs HTML report: file:///home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/java/build/reports/spotbugs/main.html

> Task :check
```