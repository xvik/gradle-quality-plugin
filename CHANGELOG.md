* Fix report separating lines disappear in intellij IDEA output (appears when gradle output recognized as junit test output).
  Extra zero-width space symbol used to prevent trims.
* Update checkstyle 8.36.2 -> 8.40
* Update checkstyle config:
    - Add [RecordComponentName](https://checkstyle.sourceforge.io/config_naming.html#RecordComponentName)
    - Add [JavadocMissingLeadingAsterisk](https://checkstyle.sourceforge.io/config_javadoc.html#JavadocMissingLeadingAsterisk)
    - Add [LambdaBodyLength](https://checkstyle.sourceforge.io/config_sizes.html#LambdaBodyLength)
    - Add optional [external suppressions file support](https://checkstyle.sourceforge.io/config_filters.html#SuppressionFilter): 
        just create gradle/config/checkstyle/suppressions.xml and it will be used automatically with the default config
* Update pmd 6.28 -> 6.31    
* Update pmd config:
    - Remove [AvoidInstantiatingObjectsInLoops](https://pmd.github.io/pmd-6.28.0/pmd_rules_java_performance.html#avoidinstantiatingobjectsinloops)
        because its useless most of the time
    - Remove [AssignmentInOperand](https://pmd.github.io/pmd-6.28.0/pmd_rules_java_errorprone.html#assignmentinoperand)
        because its quite common technique 
    - Remove [AvoidUsingVolatile](https://pmd.github.io/pmd-6.28.0/pmd_rules_java_multithreading.html#avoidusingvolatile)
        because it's not an issue but attention pointer
    - Update [NcssCount](https://pmd.github.io/pmd-6.28.0/pmd_rules_java_design.html#ncsscount)
        methodReportLevel from 30 to 40, classReportLevel from 300 to 500
* Update spotbugs 4.1.3 -> 4.2.0  
* Update spotbugs plugin 4.5.1 -> 4.6.0
* Set checkstyle.configDirectory property: required for ${config_loc} variable (#29)

### 4.4.0 (2020-10-11)
* Update codenarc 1.5 -> 2.0.0 (groovy 3 and java 14 support, requires java 7)
* Update codenarc config:
    - Disable new rule [ImplicitReturnStatement](https://codenarc.org/codenarc-rules-convention.html#implicitreturnstatement-rule)
        because it will cause too many warnings on typical projects and sometimes don't see existing return
* Update pmd 6.23 -> 6.28 (java 15 support, text block syntax)
* Update checkstyle 8.32 -> 8.36.2 (java 14 records support, text block syntax)
* Update checkstyle config
    - Add [NoCodeInFile](https://checkstyle.sourceforge.io/config_misc.html#NoCodeInFile)
    - Add [IllegalIdentifierName](https://checkstyle.sourceforge.io/config_naming.html#IllegalIdentifierName)
    - Add [RecordComponentNumber](https://checkstyle.sourceforge.io/config_sizes.html#RecordComponentNumber)
    - Add [RecordTypeParameterName](https://checkstyle.sourceforge.io/config_naming.html#RecordTypeParameterName)
    - Add [PatternVariableName](https://checkstyle.sourceforge.io/config_naming.html#PatternVariableName)
* Update spotbugs 4.0.3 -> 4.1.3
* Update spotbugs plugin 4.1.0 -> 4.5.1   
* Fix exclusions apply for spotbugs (#27)
* Automatically exclude apt-generated sources for spotbugs 
  (apt processor(s) assumed to be specified with annotationProcessor configuration)   

### 4.3.0 (2020-05-13)
* Update spotbugs plugin to 4.1.0 (#26)
    - Fixes gradle 6.4 compatibility.
    - The new plugin is a re-write of the original spotbugs plugin. But quality plugin 
      makes it work as before, so you shouldn't see any difference.
    - Minimal supported gradle is now 5.6 (due to spotbugs plugin)  
    - New spotbugs plugin can generate html report itself, but this option is not used
* Update spotbugs 4.0.2 -> 4.0.3
* Update checkstyle 8.31 -> 8.32
* Update checkstyle config:
    - New rule [JavadocMissingWhitespaceAfterAsterisk](https://checkstyle.sourceforge.io/config_javadoc.html#JavadocMissingWhitespaceAfterAsterisk)
* Update pmd 6.22 -> 6.23        

NOTE:
    * New spotbugs plugin does not support build cache (https://github.com/spotbugs/spotbugs-gradle-plugin/issues/244)
    * Spotbugs task always show an exception when violations found (not a problem, just confusing)

### 4.2.2 (2020-04-23)
* Remove spotbugs configuration "tuning" (added in the last version) because it eventually works incorrectly (often causing warnings).
  Spotbugs 4.0.2 depends on slf4j 1.8 which should avoid compatibility problems (at least for some time).

### 4.2.1 (2020-04-16)
* Update spotbugs 4.0.1 -> 4.0.2
* Revert (and change) spotbugs configuration customizations:
    - Remove explicit asm dependency for "spotbugs" configuration
      It was added by mistake: dependency-management plugin applied for all configurations 
      was actually guilty of incorrect asm version (case description added to documentation)
    - Force correct version of sl4j-simple instead of removing dependency
      (nasty warnings introduced in previous release will disappear now)
      
Warnings from the previous version are no more actual (except new plugin version (4)).           

### 4.2.0 (2020-04-15)
* Fix gradle configuration fail: "Cannot access last() element from an empty List"
  (project with explicit quality configuration fails to open in the new IDEA)
* Fix concurrent default configs initialization clash (#22) 
* Update spotbugs 3.1.12 -> 4.0.1   
* Spotbugs classpath changes ("spotbugs" configuration):
    - Removed `sl4j-simple` dependency: you will see default slf4j warnings
       but everything will work in all cases and will no more fail due to sl4j version class with gradle's own sl4j (#20)
    - `asm` 7.3.1 is directly specified to force 7.3.1 because otherwise gradle (5.6) downgrades it to 7.2 (which leads to execution fails "no class def found").         
* Update codenarc 1.4 -> 1.5
* Update codenarc config:
    - Disable new rule [ImplicitClosureParameter](https://codenarc.github.io/CodeNarc/codenarc-rules-convention.html#implicitclosureparameter-rule)
* Fix codenarc link in console report (docs moved from sourceforge to github)
* Update checkstyle 8.29 -> 8.31
* Update checkstyle rules:
    * New rule [UnnecessarySemicolonAfterOuterTypeDeclaration](https://checkstyle.sourceforge.io/config_coding.html#UnnecessarySemicolonAfterOuterTypeDeclaration)
    * Disable new rule [AvoidDoubleBraceInitialization](https://checkstyle.sourceforge.io/config_coding.html#AvoidDoubleBraceInitialization)
    * Update [NewlineAtEndOfFile](https://checkstyle.sourceforge.io/config_misc.html#NewlineAtEndOfFile) 
        lineSeparator configuration to default (lf_cr_crlf) because rule fixed in 8.30 and now may cause too many violations 
* Fix checkstyle link in console report (sourceforge.net changed to sourceforge.io to avoid redirect)
* Update pmd 6.21 -> 6.22 

WARNING (spotbugs related): 
1. If you want to downgrade spotbugs version (with `quality.spotbugsVersion = ..`) then you'll have
   to also force correct asm version on spotbugs configuration.
2. If you need to see spotbugs logs, then manually add `slf4j-simple` dependency to spotbugs configuration.
   (normally, spotbugs logs are not important and removing dependency fixes some environments)
3. If you don't want to see sl4j default warnings then simply put `slf4j-nop` dependency into spotbugs
    configuration. I can't do it automatically because I may introduce new sl4j compatibility problems due to incorrect version.       
4. I know, there is a [new spotbugs plugin](https://github.com/spotbugs/spotbugs-gradle-plugin) 4.0.5,
   but it is conceptually different, so old version would be used for some time. (I tried to upgrade, but it requires time to resolve all issues)
5. BUT with all this, spotbugs 4 should work without problems for everyone!        

### 4.1.0 (2020-02-15)
* Fix disabled plugin execution with no-tasks gradle run (pure initialization) (#21)
* Update checkstyle 8.26 -> 8.29
* Update checkstyle config:
    - Add new checks:
        * [AvoidNoArgumentSuperConstructorCall](https://checkstyle.sourceforge.io/config_coding.html#AvoidNoArgumentSuperConstructorCall)
        * [NoEnumTrailingComma](https://checkstyle.sourceforge.io/config_coding.html#NoEnumTrailingComma)
        * [NoArrayTrailingComma](https://checkstyle.sourceforge.io/config_coding.html#NoArrayTrailingComma)
        * [JavadocContentLocation](https://checkstyle.sourceforge.io/config_javadoc.html#JavadocContentLocation)
* Update pmd 6.17.0 -> 6.21.0 
* Update pmd config:
    - Disabled rule `InvalidSlf4jMessageFormat` name changed to `InvalidLogMessageFormat`
    
### 4.0.0 (2019-11-16)
* Gradle 6.0 compatibility
    - (breaking) Removed findbugs plugin support because it was removed in gradle 6
* (breaking) Minimal required gradle is now 5.1     
* Update spotbugs 3.1.11 -> 3.1.12
* Update com.github.spotbugs plugin: 1.6.5 -> 2.0.1
    - Note that plugin group changed: gradle.plugin.com.github.spotbugs -> com.github.spotbugs
* Add spotbugs-related options:
    - spotbugsMaxHeapSize setting may be used to increase default spotbugs memory ([reduced to 512mb in gradle 5](https://github.com/gradle/gradle/issues/6216)).
        Option does not override maxHeapSize manually set on spotbugs task (to not break working builds) (#12)
    - spotbugsMaxRank setting allows to filter low-ranked rules (ranks are different from priorities!) (#15)
        Option modifies excludes.xml file (the only way to apply rank threshold)
    - spotbugsPlugin shortcut method to simplify plugins declaration (without afterEvaluate block or manual spotbugs plugin declaration)    
* Spotbugs console report changes:
    - Show both rule rank and priority : `[priority 2 / rank 14]`
    - Identify rules from plugins: `[fb-contrib project | Correctness | FCBL_FIELD_COULD_BE_LOCAL]`            
* Update codenarc 1.3 -> 1.4
* Update checkstyle 8.17 -> 8.26
* Update checkstyle config:
    - Disable [TrailingComment](https://checkstyle.sourceforge.io/config_misc.html#TrailingComment)
        as not useful and contradicting with PMD suppression syntax (`// NOPMD`)
    - Add new checks:
        * [MissingJavadocPackage](https://checkstyle.sourceforge.io/config_javadoc.html#MissingJavadocPackage)
        * [MissingJavadocType](https://checkstyle.sourceforge.io/config_javadoc.html#MissingJavadocType)
        * [UnnecessarySemicolonInTryWithResources](https://checkstyle.sourceforge.io/config_coding.html#UnnecessarySemicolonInTryWithResources)
        * [UnnecessarySemicolonInEnumeration](https://checkstyle.sourceforge.io/config_coding.html#UnnecessarySemicolonInEnumeration)
        * [UnnecessarySemicolonAfterTypeMemberDeclaration](https://checkstyle.sourceforge.io/config_coding.html#UnnecessarySemicolonAfterTypeMemberDeclaration)
        * [InvalidJavadocPosition](https://checkstyle.sourceforge.io/config_javadoc.html#InvalidJavadocPosition)        
        * [JavadocBlockTagLocation](https://checkstyle.sourceforge.io/config_javadoc.html#JavadocBlockTagLocation)
    - Disable new checks:
        * [OrderedProperties](https://checkstyle.sourceforge.io/config_misc.html#OrderedProperties)
        * [MissingJavadocMethod](https://checkstyle.sourceforge.io/config_javadoc.html#MissingJavadocMethod)             
* Update pmd 6.11.0 -> 6.17.0  
    NOTE: 6.18 or 6.19 can't be used due to [regression](https://github.com/pmd/pmd/issues/2098) (should be fixed in 6.20) 
* Update pmd config:
    - Add 1 as allowed "magic number" for [AvoidLiteralsInIfCondition](https://pmd.github.io/pmd-6.11.0/pmd_rules_java_errorprone.html#avoidliteralsinifcondition)
    - Disable new rule [AvoidUncheckedExceptionsInSignatures](https://pmd.github.io/pmd-6.17.0/pmd_rules_java_design.html#avoiduncheckedexceptionsinsignatures)
        because it produces false positives for implemented interfaces (and generally not useful)
    - Remove `java.lang.AutoCloseable` in [CloseResource](https://pmd.github.io/pmd-6.17.0/pmd_rules_java_errorprone.html#closeresource) rule
        because it produces too many false positives    
* Add `pmdIncremental` option - shortcut for gradle's `pmd.incrementalAnalysis` option. Disabled by default.    
* Add PMD's CPD tool support through [de.aaschmid.cpd](https://github.com/aaschmid/gradle-cpd-plugin) plugin. (#4) 
    - CPD gradle plugin must be applied manually: no automatic plugin enabling  
    - Sets pmd version and silent mode. 
    - Sources configured according to quality configuration. Exclusions (source and pattern) are supported too 
    - Html report generated (using style recommended by pmd; style file added as overridable config)
    - Full console report (like for other quality plugins)  
    - Support for multi-module projects (where cpd plugin applied in root project and quality in subprojects)
* Use gradle configuration avoidance to prevent not used quality tasks creation
* Use compilerArgumentProviders instead of direct options modification (JavaCompile.options.compilerArgs) to workaround 
    possible immutable list usage in options (#19)

### 3.4.0 (2019-02-16)
* Fix source files root detection (#13)
* Set spotbugs tasks default heap size to 1g to mimic old behaviour in [gradle 5](https://docs.gradle.org/5.0/userguide/upgrading_version_4.html#rel5.0:default_memory_settings) (#12)
* Update pmd 6.9.0 -> 6.11.0   
* Update checkstyle 8.14 -> 8.17
* Update spotbugs 3.1.9 -> 3.1.11
* Update codenarc 1.2.1 -> 1.3
* Update codenarc config:
    - Add new ruleset [Comments](http://codenarc.sourceforge.net/codenarc-rules-comments.html)
    - Disable new rule [ClassStartsWithBlankLine](http://codenarc.sourceforge.net/codenarc-rules-formatting.html#ClassStartsWithBlankLine)
    - Disable new rule [ClassEndsWithBlankLine](http://codenarc.sourceforge.net/codenarc-rules-formatting.html#ClassEndsWithBlankLine)

NOTE com.github.spotbugs plugin not updated to more recent version (1.6.9) because 
it breaks gradle 4.0-4.6 compatibility (versions from 1.6.6) and build cache support  

### 3.3.0 (2018-11-23)
* Update pmd config:
    - Allow camel cased enum constants for [FieldNamingConventions](https://pmd.github.io/pmd-6.7.0/pmd_rules_java_codestyle.html#fieldnamingconventions)
* Update spotbugs 3.1.6 -> 3.1.9
* Update com.github.spotbugs plugin 1.6.3 -> 1.6.5 
* Update checkstyle 8.12 -> 8.14
* Update pmd 6.7.0 -> 6.9.0            

### 3.2.0 (2018-09-11)
* Gradle 4.10 compatibility:
    - updated com.github.spotbugs plugin 1.6.2 -> 1.6.3  
* Update checkstyle 8.11 -> 8.12    
* Update pmd 6.5.0 -> 6.7.0     
* Update codenarc 1.2 -> 1.2.1

### 3.1.1 (2018-07-22)
* Fix maven central and jcenter artifacts (missed dependency) (#9)
* Update spotbugs 3.1.5 -> 3.1.6 

### 3.1.0 (2018-07-13)
* Gradle 4.8 compatibility:
    - updated com.github.spotbugs plugin 1.6.1 -> 1.6.2  
    - fix exclusions support 
* Update checkstyle 8.8 -> 8.11
* Update checkstyle config:
    - New check [LambdaParameterName](http://checkstyle.sourceforge.net/config_naming.html#LambdaParameterName)    
* Update pmd 6.1.0 -> 6.5.0 
* Update pmd config:
    - Disable [ClassNamingConventions](https://pmd.github.io/pmd-6.5.0/pmd_rules_java_codestyle.html#classnamingconventions)
* Update spotbugs 3.1.2 -> 3.1.5 
* Update codenarc 1.1 -> 1.2
* Update codenarc config:
    - Disable new check [NoJavaUtilDate](http://codenarc.sourceforge.net/codenarc-rules-convention.html#NoJavaUtilDate) 
* Option to disable html reports: quality.htmlReports=false (#5)

### 3.0.0 (2018-03-25)
* Update codenarc 1.0 -> 1.1
* Update checkstyle 8.2 -> 8.8
* Update pmd 5.8.1 -> 6.1.0
* Update pmd config:
    - (breaking) All rules reordered according to [new groups](https://pmd.github.io/pmd-6.0.0/pmd_release_notes.html#rule-categories)
    - Disable [CommentDefaultAccessModifier](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_codestyle.html#commentdefaultaccessmodifier) as not useful
    - Disable [ExcessiveClassLength](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#excessiveclasslength)
        and [ExcessiveMethodLength](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#excessivemethodlength)
        in favor of new rule [NcssCount](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#ncsscount) (which counts lengths without empty lines and comments)
    - Defaults for [NcssCount](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#ncsscount) (which counts length without empty lines and comments) changed:
        30 lines for method and 300 for class (with previous Excessive* rules it was 50 and 500 accordingly)
    - Disable new rule [DataClass](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#dataclass) as too strict for general cases
    - Switch from [ModifiedCyclomaticComplexity](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#modifiedcyclomaticcomplexity) (deprecated) to
        [CyclomaticComplexity](https://pmd.github.io/pmd-6.1.0/pmd_rules_java_design.html#cyclomaticcomplexity) with ignoreBooleanPaths option                 
* (breaking) use Spotbugs (3.1.2) instead of Findbugs by default (as [successor](https://github.com/findbugsproject/findbugs))
    - [com.github.spotbugs](http://spotbugs.readthedocs.io/en/latest/gradle.html) external plugin applied (quality plugin brings it as a dependency)
    - Spotbugs plugin will use configs from different folder (spotbugs/), so if custom findbugs configs were used move them to spotbugs/ folder    
    - Findbugs support is deprecated and will be removed someday (but not soon). 
    - To use findbugs (as before): disable spotbugs support (quality.spotbugs = false) 
       or enable findbugs plugin manually (in this case spotbugs plugin will not be registered)  
* Update spotbugs exclusions:
    - Exclude [NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#np-method-tightens-nullness-annotation-on-parameter-np-method-parameter-tightens-annotation) 
        check as it prevents @Nullable override, which may be required for guava functions
    - Exclude [NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#np-possible-null-pointer-dereference-due-to-return-value-of-called-method-np-null-on-some-path-from-return-value) 
        check as misleading and not useful 

### 2.4.0 (2017-09-21)
* Support gradle 4.2 (#3)    
* Update checkstyle 8.0 -> 8.2
* Update codenarc 0.27.0 -> 1.0
* Update checkstyle config:
    - Move SuppressionCommentFilter inside TreeWalker (8.1 breaking change)
    - Remove FileContentsHolder (8.2 breaking change) 
    - New check [AnnotationOnSameLine](http://checkstyle.sourceforge.net/config_annotation.html#AnnotationOnSameLine) (added in 8.2) added to config, but disabled

### 2.3.0 (2017-07-24)
* Update checkstyle 7.6 -> 8.0
* Update pmd 5.5.4 -> 5.8.1
* Update pmd config:
    - Add description to avoid [warning](https://github.com/pmd/pmd/issues/470)
    - Disable [AccessorMethodGeneration](https://pmd.github.io/latest/pmd-java/rules/java/design.html#AccessorMethodGeneration)
    check as it makes sense for android projects and not so important for java. 
    Moreover, possible fixes will contradict with checkstyle's [VisibilityModifier](http://checkstyle.sourceforge.net/config_design.html#VisibilityModifier)   

### 2.2.0 (2017-03-21)
* Update checkstyle 7.4 -> 7.6
* Update pmd 5.5.2 -> 5.5.4
* Update codenarc 0.26.0 -> 0.27.0
* Update pmd config:
    - Disable [NullAssignment](https://pmd.github.io/latest/pmd-java/rules/java/controversial.html#NullAssignment)
    check due to false positives
* Add unified exclusion patterns (glob) configuration: exclude (#2)
    - Does not affect animalsniffer because it's a different type of check
    - Findbugs did not support direct exclusions, so plugin resolves excluded classes and adds them to exclusion xml (default or user defined)
* Add configuration to directly exclude source files from check (for exceptional cases when pattern exclusion cant help): excludeSources
    - Does not affect animalsniffer
    - For findbugs excluded classes will be added to exclusions xml filter   

### 2.1.0 (2017-01-24)
* Update checkstyle 7.1 -> 7.4
* Update pmd 5.5.1 -> 5.5.2
* Update codenarc 0.25.2 -> 0.26.0
* Update pmd config:
    - Disable junit4 migration rules which cause false positives for non test code:
    [JUnit4TestShouldUseBeforeAnnotation](https://pmd.github.io/latest/pmd-java/rules/java/migrating.html#JUnit4TestShouldUseBeforeAnnotation),
    [JUnit4TestShouldUseAfterAnnotation](https://pmd.github.io/latest/pmd-java/rules/java/migrating.html#JUnit4TestShouldUseAfterAnnotation),
    [JUnit4TestShouldUseTestAnnotation](https://pmd.github.io/latest/pmd-java/rules/java/migrating.html#JUnit4TestShouldUseTestAnnotation)    

### 2.0.0 (2016-09-03)
* Update checkstyle 6.17 -> 7.1 (requires min jdk 8)
* Update checkstyle config:
    - Add [SingleSpaceSeparator](http://checkstyle.sourceforge.net/config_whitespace.html#SingleSpaceSeparator) check (since 6.19)
    - Disable [FileLength](http://checkstyle.sourceforge.net/config_sizes.html#FileLength) check in favour of more correct PMD ExcessiveClassLength check (which checks actual class length not file)
    - Enable [ReturnCount](http://checkstyle.sourceforge.net/config_coding.html#ReturnCount) to replace pmd OnlyOneReturn check
* Update pmd 5.4.1 -> 5.5.1 (requires min jdk 7)
* Update pmd config:
    - Disable [UselessParentheses](https://pmd.github.io/pmd-5.5.1/pmd-java/rules/java/unnecessary.html#UselessParentheses) because of too many false positives
    - Disable [OnlyOneReturn](https://pmd.github.io/pmd-5.5.1/pmd-java/rules/java/controversial.html#OnlyOneReturn) in favour of checkstyle ReturnCount
    - Disable [InvalidSlf4jMessageFormat](https://pmd.github.io/pmd-5.5.1/pmd-java/rules/java/logging-java.html#InvalidSlf4jMessageFormat) due to [known bug](https://sourceforge.net/p/pmd/bugs/1509)
    - Disable [CyclomaticComplexity](https://pmd.github.io/pmd-5.5.1/pmd-java/rules/java/codesize.html#CyclomaticComplexity) and [StdCyclomaticComplexity](https://pmd.github.io/pmd-5.5.0/pmd-java/rules/java/codesize.html#StdCyclomaticComplexity) as duplicate rules for ModifiedCyclomaticComplexity
* Update codenarc 0.25.1 -> 0.25.2
* (breaking) quality.lintOptions configuration now applies to all JavaCompile tasks (not only to compileJava as before) 
* Fix multi-module projects reporting (remove duplicate reports)
* Fix generated html reports links (redundant slash on linux)
* Fix newline in reporters (use platform specific)
* Change reporting format so intelliJ IDEA can recognize class reference and show link (eclipse will probably too). Reference line ranges are not shown anymore (pmd, findbugs and column in checkstyle) - always exact line.
* (breaking) configure manually registered plugins, even if plugin not supposed to be used due to sources auto detection. Configuration may be disabled using quality configuration flags.
* Add ability to disable automatic plugins registration: quality.autoRegistration = false. Only manually registered plugins will be configured.
* Add ability to disable quality tasks with configuration property: quality.enabled = false. Quality tasks will still work if called directly or through grouping task (e.g. checkQualityMain).
* (breaking) Remove checkstyle html report generation: gradle can generate html report since 2.10 and when gradle generates html report, it puts link to it into main error message
* Findbugs html report is always generated (not only when errors found like before)
* Add ability to disable console reporting with configuration property: quality.consoleReporting = false
* Add grouping tasks for registered quality plugins: checkQualityMain, checkQualityTest (per source set). Allows running quality tasks for exact source set or run quality tasks not enabled for 'check' task.

### 1.3.0 (2016-03-30)
* Update checkstyle 6.14.1 -> 6.17
* Update codenarc 0.24.1 -> 0.25.1 
* Disable default checkstyle html report (enabled in gradle >=2.10) to avoid duplicate report generation
* Add reporting execution time logging (visible with --info option)
* Update default checkstyle config:
    - Disable [Misc/UncommentedMain](http://checkstyle.sourceforge.net/config_misc.html#UncommentedMain)

### 1.2.0 (2016-01-01)
* Update default configs: 
    - Disable [Pmd/Controversial/AvoidFinalLocalVariable](https://pmd.github.io/pmd-5.4.1/pmd-java/rules/java/controversial.html#AvoidFinalLocalVariable)
    - [Pmd/Design/AvoidDeeplyNestedIfStmts](https://pmd.github.io/pmd-5.4.1/pmd-java/rules/java/design.html#AvoidDeeplyNestedIfStmts) default set to 4
    - [Checkstyle/Coding/NestedIfDepth](http://checkstyle.sourceforge.net/config_coding.html#NestedIfDepth) default set to 3
    - Add disabled check in config [Checkstyle/Naming/CatchParameterName](http://checkstyle.sourceforge.net/config_naming.html#CatchParameterName)  
* Update checkstyle 6.13 -> 6.14.1     

### 1.1.1 (2015-12-24)
* Fix AnimalSniffer version configuration

### 1.1.0 (2015-12-21)
* Add [ru.vyarus.animalsniffer](https://github.com/xvik/gradle-animalsniffer-plugin) plugin configuration support
* Improve FindBugs console output

### 1.0.3 (2015-12-11)
* Delay default configs copying to actual task execution to avoid problems with clean task

### 1.0.2 (2015-12-06)
* Unescape CodeNarc messages
* Update checkstyle 6.12.1 -> 6.13
* Update pmd 5.4.0 -> 5.4.1

### 1.0.1 (2015-12-04)
* Don't activate CodeNarc if groovy plugin enabled but no groovy sources folder exists

### 1.0.0 (2015-11-19)
* Initial release
