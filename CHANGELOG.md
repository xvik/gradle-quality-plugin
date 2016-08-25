* (breaking) Require jdk 8 (drop jdk 6,7 compatibility because of new pmd and checkstyle versions)
* Update checkstyle 6.17 -> 7.1
* Update checkstyle config:
    - Add [SingleSpaceSeparator](http://checkstyle.sourceforge.net/config_whitespace.html#SingleSpaceSeparator) check (since 6.19)
    - Disable [FileLength](http://checkstyle.sourceforge.net/config_sizes.html#FileLength) check in favour of more correct PMD ExcessiveClassLength check (which checks actual class length not file)
    - Enable [ReturnCount](http://checkstyle.sourceforge.net/config_coding.html#ReturnCount) to replace pmd OnlyOneReturn check
* Update pmd 5.4.1 -> 5.5.1
* Update pmd config:
    - Disable [UselessParentheses](https://pmd.github.io/pmd-5.5.1/pmd-java/rules/java/unnecessary.html#UselessParentheses) because of too many false positives
    - Disable [OnlyOneReturn](https://pmd.github.io/pmd-5.5.1/pmd-java/rules/java/controversial.html#OnlyOneReturn) in favour of checkstyle ReturnCount
* Update codenarc 0.25.1 -> 0.25.2
* lintOptions configuration now applies to all JavaCompile tasks (not only compileJava as before) 
* Fix multi-module projects reporting (remove duplicate reports)
* Fix generated html reports links (redundant slash on linux)
* Fix newline in reporters (use platform specific)
* Change reporting format so intelliJ IDEA can recognize class reference and show link (eclipse will probably too). Reference line ranges are not shown anymore (pmd, findbugs and column in checkstyle) - always exact line.

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
