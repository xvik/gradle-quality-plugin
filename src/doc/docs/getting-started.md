# Getting started

## Installation

!!! note
    When updating plugin version in your project don't forget to call `clean` task to remove cached configs from previous plugin version

Plugin is available from [maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-quality-plugin),
[bintray jcenter](https://bintray.com/vyarus/xvik/gradle-quality-plugin/_latestVersion)
and [gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.quality).

```groovy
plugins {
    id 'ru.vyarus.quality' version '4.0.0'
}
```

OR

```groovy
buildscript {
    repositories {
        jcenter()
        gradlePluginPortal()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-quality-plugin:4.0.0'
    }
}
apply plugin: 'ru.vyarus.quality'
```

!!! warning
    Plugin must be applied after `java`, `java-library` or `groovy` plugins. Otherwise it will do nothing.

!!! note
    Requires java 8 and gradle >= 5.1

If you are using lower java versions use previous plugin releases.

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