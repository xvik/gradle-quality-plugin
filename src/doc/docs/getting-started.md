# Getting started

## Installation

Plugin is available from maven central, [bintray jcenter](https://bintray.com/bintray/jcenter)
and [gradle plugins portal](https://plugins.gradle.org).

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-quality-plugin:2.4.0'
    }
}
apply plugin: 'ru.vyarus.quality'
```

OR

```groovy
plugins {
    id 'ru.vyarus.quality' version '2.4.0'
}
```

!!! warning
    Plugin must be applied after `java` or `groovy` plugins. Otherwise it will do nothing.

!!! note
    Plugin is compiled for java 6, but pmd 5.5 [requires java 7](https://pmd.github.io/pmd-5.5.1/overview/changelog-old.html) 
    and checkstyle 7 [requires java 8](http://checkstyle.sourceforge.net/releasenotes.html#Release_7.0). So, by default, you will need java 8.

If you are using lower java versions either use previous plugin release ([1.3.0](https://github.com/xvik/gradle-quality-plugin/tree/1.3.0)) or manually specify lower checkstyle and pmd (if required java 6)
versions using `checkstyleVersion = '6.19'` and `pmdVersion = '5.4.2'` properties (note that you may need to customize provided default rules configurations to disable rules not yet available in your tool versions).

## Usage

Plugin will auto detect java and groovy sources and activate required quality plugins.
All tools will be configured with the default opinionated configs. 

```bash
$ gradlew check
```

Will execute all quality plugins. Alternatively, you can use [grouping task](task/group.md) to run checks without tests.

If any violations were found then build will fail with all violations printed to console. For example like this:

```
23 PMD rule violations were found in 2 files

[Comments | CommentRequired] sample.(Sample.java:3) 
  headerCommentRequirement Required
  https://pmd.github.io/pmd-5.4.0/pmd-java/rules/java/comments.html#CommentRequired
  
...  
```

Or you can use build task (which also calls check): 

```bash
$ gradlew build
```

!!! tip
    It's better to fix all quality issues before commit.

## Non strict mode

You can switch off strict mode to avoid build failure when quality violations are found:

```groovy
quality {
    strict = false
}
```

You will still see all violations in the output.

## Suppress

Sometimes (quite rare) tool could be wrong or your situation
could require violation break. In this case violation could be suppressed: see exact tool page for suppression hints.