# Gradle quality plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-quality-plugin.svg)](https://travis-ci.org/xvik/gradle-quality-plugin)

### About

Static code analysis for Java and Groovy projects using Checkstyle, PMD, FindBugs and CodeNarc.
Plugin implements unified console output for all quality plugins which greatly simplifies developer workflow: 
only console is required for working with violations and makes it feel the same as java compiler errors.

Features:
* Adds extra javac lint options to see more warnings
* Complete console output for all quality plugins
* Html and xml reports for all plugins (custom xsl used for findbugs html report because it can't generate both xml and html reports)
* Zero configuration by default: provided opinionated configs will make it work out of the box
* Task to copy default configs for customization
* Grouping tasks to run registered quality plugins for exact source set (e.g. checkQualityMain)

### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-quality-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-quality-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.quality).

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-quality-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-quality-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-quality-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-quality-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-quality-plugin:2.3.0'
    }
}
apply plugin: 'ru.vyarus.quality'
```

OR

```groovy
plugins {
    id 'ru.vyarus.quality' version '2.3.0'
}
```

Plugin must be applied after `java` or `groovy` plugins. Otherwise it will do nothing.

**IMPORTANT** Plugin is compiled for java 6, but pmd 5.5 [requires java 7](https://pmd.github.io/pmd-5.5.1/overview/changelog-old.html) 
and checkstyle 7 [requires java 8](http://checkstyle.sourceforge.net/releasenotes.html#Release_7.0). So, by default, you will need java 8.

If you are using lower java versions either use previous plugin release ([1.3.0](https://github.com/xvik/gradle-quality-plugin/tree/1.3.0)) or manually specify lower checkstyle and pmd (if required java 6)
versions using `checkstyleVersion = '6.19'` and `pmdVersion = '5.4.2'` properties (note that you may need to customize provided default rules configurations to disable rules not yet available in your tool versions).

### Default behaviour

Plugin will activate [Checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html), 
[PMD](https://docs.gradle.org/current/userguide/pmd_plugin.html) and 
[FindBugs](https://docs.gradle.org/current/userguide/findbugs_plugin.html) plugins for java project. Java is detected by enabled java plugin
and physical presence of java sources. This is required because groovy plugin is also java plugin and these tools are not required for groovy.
If groovy plugin available and groovy sources exists, [CodeNarc](https://docs.gradle.org/current/userguide/codenarc_plugin.html) will be enabled.

For example, if groovy plugin active and there is no directory 'src/main/groovy' - CodeNarc will not be registered (suppose default plugins sources config to main only). 
This is common when groovy used only for tests.

If you have both java and groovy sources, then FindBugs, Checkstyle and PMD will check java sources and CodeNarc will be used for groovy.

By default, checks applied only to main sources and avoid test sources checking. 

Default configs are opinionated: not all possible checks are enabled, just the sane majority of them. Also, some defaults were changed.
Anyway, all disabled checks are commented in config files, so it would be clear what was disabled.

Any violation leads to build failure (strict mode) - this is the only way to keep quality and I strongly suggest not to disable it.
Otherwise you'll either spent days fixing violations (someday after) or never fix them. 

Of course, [default behaviour could be changed](#configuration)

In order to execute all quality checks do:

```bash
$ gradlew check
```

Or use special tasks (added by plugin) to run quality checks by source:

```bash
$ gradlew checkQualityMain
```

In contrast to `check`, this task run only quality tasks registered for Main source set without running tests.  

It's better to fix all quality issues before commit.

### Console reporting

Here are the samples of console error messages. Note that sometimes (really rare) tool could be wrong or your situation
could require violation break. In this case violation could be suppressed.

Note that class line usually looks like `sample.(Sample.java:0)`. This syntax allows idea (probably eclipse too)
to put direct link to source from console.

#### Checkstyle

```
8 Checkstyle rule violations were found in 2 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:0)
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile
```

To [suppress violation](http://checkstyle.sourceforge.net/config_filters.html#SuppressWarningsFilter):

```java
@SuppressWarnings("NewlineAtEndOfFile")
```

Or [with prefix](http://checkstyle.sourceforge.net/config_annotation.html#SuppressWarningsHolder) (but require lower cased name):

```java
@SuppressWarnings("checkstyle:newlineatendoffile")
```

To suppress all violations:

```java
@SuppressWarnings("all")
```

Or using [comments](http://checkstyle.sourceforge.net/config_filters.html#SuppressionCommentFilter):

```java
// CHECKSTYLE:OFF
..anything..
// CHECKSTYLE:ON
```

#### PMD

```
23 PMD rule violations were found in 2 files

[Comments | CommentRequired] sample.(Sample.java:3) 
  headerCommentRequirement Required
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/comments.html#CommentRequired

```

To [suppress violation](https://pmd.github.io/pmd-5.4.0/usage/suppressing.html):

```java
@SuppressWarnings("PMD.CommentRequired")
```

To suppress all violations:

```java
@SuppressWarnings("PMD")
```

#### FindBugs

```
2 (0 / 2 / 0) FindBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:8) [priority 2]
	>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.
```

Counts in braces show priorities (p1/p2/p3).

Note: there is no link to findbugs site, because report already contains everything from there.

To suppress violations you can use [filter file](http://findbugs.sourceforge.net/manual/filter.html).
In this case you need to override default filter file (see below).

Or you can use annotations. FindBugs use custom annotations and so you need to add 
`com.google.code.findbugs:annotations:3.0.0` dependency (with provided scope if possible) and use:

```java
@SuppressFBWarnings("URF_UNREAD_FIELD")
```

###### Plugins

You may add additional findbugs checks by declaring findbugs plugins in `findbugsPlugins` dependency configuration.
But, as findbugs plugin applied after configuration read, there are two options:

Either use afterEvaluate:

```groovy
afterEvaluate {
    dependencies {
        findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.4.1'
    }
}
```

Or declare findbugs plugin manually (it will be configured by quality plugin):

```groovy
plugins {
    id 'findbugs'
}
dependencies {
    findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.4.1'
}
```

[Find Security Bugs](http://find-sec-bugs.github.io/)

```groovy
findbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.4.4'
```

[fb-contrib: A FindBugs auxiliary detector plugin](http://fb-contrib.sourceforge.net/)

```groovy
findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.6.0'
```

###### Annotations

You may use [jsr305 annotations](http://findbugs.sourceforge.net/manual/annotations.html) to guide findbugs.
Add `com.google.code.findbugs:jsr305:3.0.0` dependency (with provided scope if possible).

In some cases you will have to use it.
For example, you may face issues with guava functions or predicates:

```
[NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE] input must be nonnull but is marked as nullable 
```

The reason for this is that guava use `@Nullable` annotation, which is `@Inherited`, so
even if you not set annotation on your own function or predicate it will still be visible.

The simplest workaround is to set `@Nonnull` annotaion (jsr305) on your function or predicate:

```java
public boolean apply(@Nonnull final Object input) {
```

#### CodeNarc

```
24 (0 / 10 / 14) CodeNarc violations were found in 2 files

[Formatting | ClassJavadoc] sample.(GSample.groovy:3)  [priority 2]
	>> class GSample {
  Class sample.GSample missing Javadoc
  Makes sure each class and interface definition is preceded by javadoc. Enum definitions are not checked, due to strange behavior in the Groovy AST.
  http://codenarc.sourceforge.net/codenarc-rules-formatting.html#ClassJavadoc
```

Counts in braces show priorities (p1/p2/p3).

To [suppress violation](http://codenarc.sourceforge.net/codenarc-configuring-rules.html#Suppressing_A_Rule_From_Within_Source_Code):

```java
@SuppressWarnings("ClassJavadoc")
```

#### AnimalSniffer

If [ru.vyarus.animalsniffer](https://github.com/xvik/gradle-animalsniffer-plugin) applied then 
it will be configured the same way as other quality plugins:

```groovy
animalsniffer {
    toolVersion = extension.animalsnifferVersion
    ignoreFailures = !extension.strict
    sourceSets = extension.sourceSets    
}
```

### Exclude files from check 

All quality tasks are based on [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
which allows excluding sources using ant patterns.

To apply exclusion to all plugins at once use:

```groovy
quality {
    exclude '**/sample/**'
}
```

This will not affect animalsniffer plugin, because it checks different thing (binary compatibility) and 
use it's own configuration [to configure exclusions](https://github.com/xvik/gradle-animalsniffer-plugin#extend-signature).

Findbugs task does not support exclusions on task level, so plugin manually resolve all excluded
classes and add them to findbugs excludes filter xml file (default or custom user file).
As a result, exclusion works the same way for all plugins.
 
Please note that exclusion patterns are resolved on relative paths (relative to source dir),
so absolute file path matching will not work. Your pattern must match just "package" and file name parts.
See example of how to overcome this in direct files exclusion chapter below.

#### Alternative configurations

All of the following configurations are allowed:

```groovy
quality {
    exclude '**/Sample.java', 'com/foo/**'
}
```

```groovy
quality {
    exclude '**/Sample.java' 
    exclude 'com/foo/**'
}
```

```groovy
quality {
    exclude = ['**/Sample.java', 'com/foo/**']
}
```

#### Direct source exclusions

When you need to exclude sources from check, you should consider:

* Extract such sources into it's own source set and exclude this set from check. (generated classes case)
* Use pattern excludes (above) to exclude sources based on package and (or) file name

If non of the above works for you, then you did sources configuration not according to best practices. 
Anyway, there is last resort option for such cases (when it could not be done the right way).

Suppose we have generated sources, added to main source set:

```groovy
sourceSets.main {
    java {
        srcDir 'build/generated/java'
    }
}
```

Here we have two source dirs for java sources: `src/main/java` and `build/generated/java`.

We want to exclude them from quality check, so we try:

```groovy
quality {
    exclude '**/generated/**'
}
```

which WILL NOT WORK because gradle applies patterns relatively to `build/generated/java` directory
and so our patter will never match.

Instead, specify ignored files directly, using rich 
[gradle files api](https://docs.gradle.org/current/userguide/working_with_files.html):

```groovy
quality {
    excludeSources = fileTree('build/generated')
}
```

This will exclude all files in 'generated' directory from quality tools checks.

As with patterns exclude, this will not affect animalsniffer. For findbugs, plugin will
add excluded classes to exclude filter.

Another example, just to show how flexible it could be configured:

```groovy
quality {
    excludeSources = fileTree('build/generated').matching {
        include '**/sample/**/*.java'
    }
}
```

Exclude all java sources in sample package (in generated directory).
Include pattern here will work relatively to `build/generated` directory.

You can use even single files:

```groovy
quality {
    excludeSources = files('build/generated/java/com/mypkg/Bad.java')
}
```

Exclude options could be used together (exclude files and patterns).

### Grouping tasks

Each quality plugin (checkstyle, pmd, findbugs etc) registers separate quality task for each source set. 
For example, `checkstyleMain` and `checkstyleTest`.
Then plugins looks to affected source sets (quality.sourceSets) and applies tasks only for affected source sets to `check` task.
For example, by default, only main source set is configured, so only `checkstyleMain` assigned to `check`.
Anyway, `checkstyleTest` task is registered and may be called directly (even if it's not used for project validation).

By analogy, quality plugin register grouping task for each available source set: `checkQualityMain`, `checkQualityTest` etc.
These tasks simply calls all quality tasks relative to source set. 
For example, if we have java quality plugins registered (for example, automatically) then calling `checkQualityMain` will call
`checkstyleMain`, `pmdMain` and `findbugsMain`.

This is just a handy shortcut to run quality check tasks for exact source set without running tests (like main `check`).
Generally usable to periodically check code violations. 

### Configuration

Use `quality` closure to configure plugin.
Defaults:

```groovy
quality {
    
    // Tools versions
    
    checkstyleVersion = '8.0'
    pmdVersion = '5.8.1'
    findbugsVersion = '3.0.1'
    codenarcVersion = '0.27.0'
    animalsnifferVersion

    /**
     * When disabled, quality plugins will not be registered automatically (according to sources). 
     * Only manualy registered quality plugins will be configured. 
     */
    boolean autoRegistration = true

    // Enable/disable tools (when auto registration disabled control configuration appliance)
     
    checkstyle = true
    pmd = true
    findbugs = true
    codenarc = true
    
    /**
     * The analysis effort level. The value specified should be one of min, default, or max.
     * Higher levels increase precision and find more bugs at the expense of running time and
     * memory consumption. Default is 'max'.
     */
    findbugsEffort = 'max'
    
    /**
     * The priority threshold for reporting bugs. If set to low, all bugs are reported.
     * If set to medium, medium and high priority bugs are reported.
     * If set to high, only high priority bugs are reported. Default is 'medium'.
     */
    findbugsLevel = 'medium'

    /**
     * Javac lint options to show compiler warnings, not visible by default.
     * Applies to all CompileJava tasks.
     * Options will be added as -Xlint:option
     * Full list of options: http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html#BHCJCABJ
     */
    lintOptions = ['deprecation', 'unchecked']

    /**
     * Strict quality leads to build fail on any violation found. If disabled, all violation
     * are just printed to console.
     */
    strict = true
    
    /**
     * When false, disables quality tasks execution. Allows disabling tasks without removing plugins.
     * Quality tasks are still registered, but skip execution, except when task called directly or through
     * checkQualityMain (or other source set) grouping task.
     */
    boolean enabled = true
    
    /**
     * When false, disables reporting quality issues to console. Only gradle general error messages will
     * remain in logs. This may be useful in cases when project contains too many warnings.
     * Also, console reporting require xml reports parsing, which could be time consuming in case of too
     * many errors (large xml reports).
     * True by default.
     */
    boolean consoleReporting = true

    /**
     * Source sets to apply checks on.
     * Default is [sourceSets.main] to apply only for project sources, excluding tests.
     */
    sourceSets = [project.sourceSets.main]
    
    /**
     * Source patterns (relative to source dir) to exclude from checks. Simply sets exclusions to quality tasks.
     * 
     * Animalsniffer is not affected because
     * it's a different kind of check (and, also, it operates on classes so source patterns may not comply).
     * 
     * Findbugs does not support exclusion directly, but plugin will resolve excluded classes and apply
     * them to xml exclude file (default one or provided by user).
     * 
     * By default nothing is excluded.
     * 
     * IMPORTANT: Patterns are checked relatively to source set dirs (not including them). So you can only
     * match source files and packages, but not absolute file path (this is gradle specific, not plugin).
     *
     * @see org.gradle.api.tasks.SourceTask#exclude(java.lang.Iterable) (base class for all quality tasks)
     */
    Collection<String> exclude = []
        
     /**
      * Direct sources to exclude from checks (except animalsniffer).
      * This is useful as last resort, when extension or package is not enough for filtering.
      * Use {@link Project#files(java.lang.Object)} or {@link Project#fileTree(java.lang.Object)}
      * to create initial collections and apply filter on it (using
      * {@link org.gradle.api.file.FileTree#matching(groovy.lang.Closure)}).
      * 
      * Plugin will include files into findbugs exclusion filter xml (default one or provided by user).
      * 
      * Note: this must be used when excluded classes can't be extracted to different source set and
      * filter by package and filename is not sufficient.
      */
     FileCollection excludeSources   

    /**
     * User configuration files directory. Files in this directory will be used instead of default (bundled) configs.
     */
    configDir = 'gradle/config/'
}
```


#### Manually registered plugins configuration

If you register any quality plugin manually it will be configured even if it's not supposed to be registered by project sources.

For example, project contains only java sources (`/src/main/java`) and codenarc plugin registered manually:

```groovy
plugins {
    id 'groovy'
    id 'codenarc'
    id 'ru.vyarus.quality'
}
```

Then quality plugin will register checkstyle, pmd and findbugs plugins and configure codenarc plugin (which is not supposed to be used according to current sources). 

To prevent manually registered plugin configuration use referenced quality option. For example, to prevent codenarc plugin configuration in example above:

```groovy
quality {
    codenarc = false
}
```

#### Manual mode

You can disable automatic quality plugins registration (guided by source detection) and register required plugins manually:

```groovy
plugins {
    id 'groovy'
    id 'checkstyle'
    id 'pmd'
}

quality {
    autoRegistration = false
}
```

Here checkstyle and pmd plugins will be configured and no other plugins will be registered.

#### Disable console output

In some cases it may not be desired to see errors in console. For example, when quality control applied on existing project
and you have thousands of warnings.

```groovy
quality {
    consoleReporting = false
}
```

But don't turn off console warnings in other cases: people tend to ignore problems they didn't see 
(practice shows that normally almost no one looks html reports of quality tools). You must see warnings for
each build to finally fix them all someday (or fix them as they appear).

Console reporting use xml reports, produced by quality plugins. In case of too many errors, xml parsing could slow down build.
You may use reporting disabling to speed up build a bit. In most cases (when you don't have thousands of errors) console reporting will be fast. 

#### Disable quality plugins

If you want to disable quality checks in some cases or opposite - enable quality checks for some build types,
you can use:

```groovy
quality {
    enabled = false
}
```

This will disable all quality tasks (by setting task.enabled = false for each). Quality tasks will still be visible, but marked as SKIPPED on execution.

Note that enable state will not affect tasks called directly. For example, you set `quality.enabled = false` and call `checkstyleMain` - it will be executed.
Motivation is simple - if you call task directly then you want it to work. 

Also, enabled state not affects quality tasks when quality grouping tasks called. For example, if you call `checkQualityMain` - all quality plugins will be executed,
even if disabled in configuration. Motivation is the same as with direct call - you obviously want to perform quality checks.

NOTE: if quality grouping task called as dependency of other task, quality plugins will be skipped. Exceptions applies only to direct cases when expected behaviour is obvious.

#### Configuration override

It is still possible to configure plugins, but direct configuration closures will not work:

```groovy
checkstyle {  // will not work, because plugin will override it
    ...
}
```

But will work like this:

```groovy
afterEvaluation {
    checkstyle { // will be applied after plugin and override configuration
        ...
    }
}
```

For plugins configuration options look:
* [CheckstyleExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.CheckstyleExtension.html)
* [PmdExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.PmdExtension.html)
* [FindBugsExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.FindBugsExtension.html)
* [CodeNarcExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.CodeNarcExtension.html)

#### Configuration files

Plugin contains predefined configurations for all plugins.
During execution default files are copied into `$buildDir/quality-configs` (if no custom user configs provided).
  
If you want to customize them there is a task to copy everything into project:

```bash
$ gradlew initQualityConfig
```  

It will copy all configs into configured (`quality.configDir`) folder (will not override existing configs).

```
gradle\
    config\
        checkstyle\
            checkstyle.xml		
        codenarc\
            codenarc.xml		
        findbugs\
            exclude.xml			
            html-report-style.xsl	
        pmd\
            pmd.xml			
```

Task copies all configs, but you may remove all files you don't want to customize (plugin will use default versions for them).
File names are important: if you rename files plugin will not find them and use defaults.

Configuration files contain all possible rules. Not used rules are commented.

### Profiling

Gradle profile report (`--profile` [option](https://docs.gradle.org/current/userguide/gradle_command_line.html)) 
shows quality tools tasks time (checkstyleMain, pmdMain etc), 
which includes both tool execution time and console reporting (performed by quality plugin). 

If you need to know exact console reporting time use `--info` option. Plugin writes reporting execution time as info log 
(see log messages starting with `[plugin:quality]` just after quality tools logs).

Alternatively, you can disable console reporting and run quality tasks with `--profile` again to see "pure" quality plugins time. 

### HTML reports

Checkstyle, pmd, codenarc plugins will produce both xml and html reports.
Findbugs plugin could produce only xml or html report, but xml report is required for console reporting. 
So quality plugin have to manually generate findbugs html report using custom xsl.

You may be sure that all html reports are generated, even when console reporting is disabled. 
(of course, if you don't disable findbugs plugin configuration with quality.findbugs = false). 

All plugins will put link to html report in general error message, except findbugs which will have to put link to xml report.
Link to findbugs html report is printed to console manually after errors reporting. If console reporting disabled,
findbugs html report path is not printed, because it should be shown only when errors found, but quality plugin
can't know it without parsing xml report (which is useless without console reporting).

### Might also like

* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [java-library generator](https://github.com/xvik/generator-lib-java) - java library project generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
