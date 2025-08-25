# Groovy project sample

For groovy sources only CodeNarc tool is used.

## Output

```
> Task :copyQualityConfigs
> Task :compileJava NO-SOURCE
> Task :compileGroovy
> Task :processResources NO-SOURCE
> Task :classes
> Task :compileTestJava NO-SOURCE
> Task :compileTestGroovy NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE

> Task :codenarcMain
CodeNarc rule violations were found. See the report at: file:///home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/groovy/build/reports/codenarc/main.html

15 (0 / 6 / 9) CodeNarc violations were found in 1 files

[Comments | ClassJavadoc] .(GSample.groovy:3)  [priority 2]
	>> class GSample {
  Class sample.GSample missing Javadoc
  Makes sure each class and interface definition is preceded by javadoc. Enum definitions are not checked, due to strange behavior in the Groovy AST.
  https://codenarc.github.io/CodeNarc/codenarc-rules-comments.html#classjavadoc-rule

[Convention | CompileStatic] .(GSample.groovy:3)  [priority 2]
	>> class GSample {
  Class should be marked with one of @GrailsCompileStatic, @CompileStatic or @CompileDynamic
  Check that classes are explicitely annotated with either @GrailsCompileStatic, @CompileStatic or @CompileDynamic
  https://codenarc.github.io/CodeNarc/codenarc-rules-convention.html#compilestatic-rule

[Convention | MethodReturnTypeRequired] .(GSample.groovy:5)  [priority 3]
	>> def foo(String bar) {
  Method "foo" has a dynamic return type
  Checks that method return types are not dynamic, that is they are explicitly stated and different than def.
  https://codenarc.github.io/CodeNarc/codenarc-rules-convention.html#methodreturntyperequired-rule

[Convention | NoDef] .(GSample.groovy:5)  [priority 3]
	>> def foo(String bar) {
  def for method return type should not be used
  def should not be used. You should replace it with concrete type.
  https://codenarc.github.io/CodeNarc/codenarc-rules-convention.html#nodef-rule

[Convention | NoDef] .(GSample.groovy:6)  [priority 3]
	>> def res = "123" + bar + "123";
  def for declaration should not be used
  def should not be used. You should replace it with concrete type.
  https://codenarc.github.io/CodeNarc/codenarc-rules-convention.html#nodef-rule

[Convention | VariableTypeRequired] .(GSample.groovy:6)  [priority 3]
	>> def res = "123" + bar + "123";
  The type is not specified for variable "res"
  Checks that variable types are explicitly specified in declarations (and not using def)
  https://codenarc.github.io/CodeNarc/codenarc-rules-convention.html#variabletyperequired-rule

[Dry | DuplicateStringLiteral] .(GSample.groovy:6)  [priority 2]
	>> def res = "123" + bar + "123";
  Duplicate String Literal: 123
  Code containing duplicate String literals can usually be improved by declaring the String as a constant field. The ignoreStrings property () can optionally specify a comma-separated list of Strings to ignore.
  https://codenarc.github.io/CodeNarc/codenarc-rules-dry.html#duplicatestringliteral-rule

[Unnecessary | UnnecessaryGString] .(GSample.groovy:6)  [priority 3]
	>> def res = "123" + bar + "123";
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  https://codenarc.github.io/CodeNarc/codenarc-rules-unnecessary.html#unnecessarygstring-rule

[Unnecessary | UnnecessaryGString] .(GSample.groovy:6)  [priority 3]
	>> def res = "123" + bar + "123";
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  https://codenarc.github.io/CodeNarc/codenarc-rules-unnecessary.html#unnecessarygstring-rule

[Unnecessary | UnnecessarySemicolon] .(GSample.groovy:6)  [priority 3]
	>> def res = "123" + bar + "123";
  Semicolons as line endings can be removed safely
  Semicolons as line terminators are not required in Groovy: remove them. Do not use a semicolon as a replacement for empty braces on for and while loops; this is a confusing practice.
  https://codenarc.github.io/CodeNarc/codenarc-rules-unnecessary.html#unnecessarysemicolon-rule

[Unused | UnusedVariable] .(GSample.groovy:6)  [priority 2]
	>> def res = "123" + bar + "123";
  The variable [res] in class sample.GSample is not used
  Checks for variables that are never referenced. The ignoreVariableNames property (null) specifies one or more variable names that should be ignored, optionally containing wildcard characters ('*' or '?').
  https://codenarc.github.io/CodeNarc/codenarc-rules-unused.html#unusedvariable-rule

[Braces | IfStatementBraces] .(GSample.groovy:7)  [priority 2]
	>> if (bar)
  The if statement lacks braces
  Use braces for if statements, even for a single statement.
  https://codenarc.github.io/CodeNarc/codenarc-rules-braces.html#ifstatementbraces-rule

[Dry | DuplicateStringLiteral] .(GSample.groovy:8)  [priority 2]
	>> return "123"
  Duplicate String Literal: 123
  Code containing duplicate String literals can usually be improved by declaring the String as a constant field. The ignoreStrings property () can optionally specify a comma-separated list of Strings to ignore.
  https://codenarc.github.io/CodeNarc/codenarc-rules-dry.html#duplicatestringliteral-rule

[Unnecessary | UnnecessaryGString] .(GSample.groovy:8)  [priority 3]
	>> return "123"
  The String '123' can be wrapped in single quotes instead of double quotes
  String objects should be created with single quotes, and GString objects created with double quotes. Creating normal String objects with double quotes is confusing to readers.
  https://codenarc.github.io/CodeNarc/codenarc-rules-unnecessary.html#unnecessarygstring-rule

[Formatting | FileEndsWithoutNewline] .(GSample.groovy:9)  [priority 3]
	>> }
  File GSample.groovy does not end with a newline
  Makes sure the source code file ends with a newline character.
  https://codenarc.github.io/CodeNarc/codenarc-rules-formatting.html#fileendswithoutnewline-rule


> Task :check
```