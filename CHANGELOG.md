* Update codenarc 1.0 -> 1.1
* Update checkstyle 8.2 -> 8.8
* Update pmd 5.8.1 -> 6.0.1
* Update pmd config:
    - (breaking) All rules reordered according to [new groups](https://pmd.github.io/pmd-6.0.0/pmd_release_notes.html#rule-categories) 
* (breaking) Spotbugs is used instead of Findbugs by default (as [successor](https://github.com/findbugsproject/findbugs))
    - [com.github.spotbugs](http://spotbugs.readthedocs.io/en/latest/gradle.html) external plugin applied (quality plugin brings it as a dependency)
    - Spotbugs plugin will use configs from different folder (spotbugs/), so if custom findbugs configs were used move them to spotbugs/ folder    
    - Findbugs support is deprecated and will be removed someday (but not soon). 
    - To use findbugs (as before): disable spotbugs support (quality.spotbugs = false) 
       or enable findbugs plugin manually (in this case spotbugs plugin will not be registered)  

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
