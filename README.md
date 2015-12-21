#Gradle quality plugin
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/gradle-quality-plugin.svg)](https://travis-ci.org/xvik/gradle-quality-plugin)

### About

Static code analysis for Java and Groovy projects using Checkstyle, PMD, FindBugs and CodeNarc.
Plugin implements unified console output for all quality plugins which greatly simplifies developer workflow: 
only console is required for working with violations and makes it feel the same as java compiler errors.

Features:
* Adds extra javac lint options to see more warnings
* Complete console output for all quality plugins
* Html report for all plugins (xsl used for checkstyle and findbugs)
* Zero configuration by default: provided opinionated configs will make it work out of the box
* Task to copy default configs for customization

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
        classpath 'ru.vyarus:gradle-quality-plugin:1.1.0'
    }
}
apply plugin: 'ru.vyarus.quality'
```

OR

```groovy
plugins {
    id 'ru.vyarus.quality' version '1.1.0'
}
```

Plugin must be applied after java or groovy plugins. Otherwise it will do nothing.

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
It's intentionally impossible to disable console output even in non strict mode to never let you forget about violations.

In order to execute all quality checks do:

```bash
$ gradlew check
```

It's better to call it before commit.

### Console reporting

Here are the samples of console error messages. Note that sometimes (really rare) tool could be wrong or your situation
could require violation break. In this case violation could be suppressed.

#### Checkstyle

```
8 Checkstyle rule violations were found in 2 files

[Misc | NewlineAtEndOfFile] sample.Sample:0
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

[Comments | CommentRequired] sample.Sample:3-14 
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

[Performance | URF_UNREAD_FIELD] sample.Sample:8 (priority 2)
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
findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.4.1'
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

[Formatting | ClassJavadoc] sample.GSample:3  (priority 2)
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

### Configuration

Use `quality` closure to configure plugin.
Defaults:

```groovy
quality {
    
    // Tools versions
    
    checkstyleVersion = '6.13'
    pmdVersion = '5.4.1'
    findbugsVersion = '3.0.1'
    codenarcVersion = '0.24.1'
    animalsnifferVersion

    // Enable/disable tools
     
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
     * Source sets to apply checks on.
     * Default is [sourceSets.main] to apply only for project sources, excluding tests.
     */
    sourceSets = [sourceSets.main]

    /**
     * User configuration files directory. Files in this directory will be used instead of default (bundled) configs.
     */
    configDir = 'gradle/config/'
}
```

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
            html-report-style.xsl	
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
