* Update checkstyle 6.14.1 -> 6.17
* Update codenarc 0.24.1 -> 0.25.1 
* Disable default checkstyle html report (enabled in gradle >=2.10) to avoid duplicate report generation
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
