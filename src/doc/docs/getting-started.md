# Getting started

## Installation

Plugin is available from [maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-quality-plugin),
and [gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.quality).

```groovy
plugins {
    id 'ru.vyarus.quality' version '{{ gradle.version }}'
}
```

OR

```groovy
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-quality-plugin:{{ gradle.version }}'
    }
}
apply plugin: 'ru.vyarus.quality'
```

!!! warning
    Plugin must be applied after `java`, `java-library` or `groovy` plugins. Otherwise, it will do nothing.

!!! note
    Requires java 8 and gradle >= 7.1

If you are using lower java versions use previous plugin releases.

## Usage

Plugin will auto detect java and groovy sources and activate required quality plugins.
All tools will be configured with the default opinionated configs.

!!! tip ""
    See [init configs task](task/config.md) to modify default configs 

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

### SpotBugs

Spotbugs plugin (java code checks) is not bundled with gradle and so you need to 
manually apply it:

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.quality' version '6.0.0'
    id 'com.github.spotbugs' version '6.2.5'
}
```

For multi-module projects, add it in the root module:

```groovy
plugins {
    id 'com.github.spotbugs' version '6.2.5' apply false
}
```

Quality plugin will apply it automatically in submodules (it must be present in the build classpath so plugin could apply it).

!!! note
    Before, spotbugs plugin was bundled as a transitive dependency, and so direct plugin
    declaration was not required. But the actual spotbugs plugin requires java 11, and
    keeping it as a transitive dependency would mean java 8 compatibility loose,
    even for groovy projects (not requiring spotbugs at all).
    

## Non strict mode

You can switch off strict mode to avoid build failure when quality violations are found:

```groovy
quality {
    strict = false
}
```

You will still see all violations in the output.

## Suppress

Sometimes a tool could be "wrong" for your specific situation. 
In this case, a violation could be suppressed: see the exact tool page for suppression hints)
(e.g. [checstyle suppress](tool/checkstyle.md#suppress)).

!!! note ""
    It is **completely normal to suppress some warnings**! But don't do it too much often.
    When you put a suppression annotation, you mean: "I know about this violation, but it is ok for this case"
    
Note that check name is always printed in square braces:

```
[Comments | CommentRequired] sample.(Sample.java:3)
```
Use it for suppression (e.g. `@SuppressWarnings("PMD.CommentRequired")` in case of PMD.

## Java projects

!!! note
    Spotbugs tool requires java 11 and checkstyle requires java 17.

You may develop locally on java 17, but run project on java 8 on CI (keep it in mind).

See [java support notes](guide/java.md) for details. 

## Examples

See [example projects](index.md#samples)
