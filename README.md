# Gradle quality plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-quality-plugin.svg)](https://travis-ci.org/xvik/gradle-quality-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-quality-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-quality-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-quality-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-quality-plugin)

**DOCUMENTATION** http://xvik.github.io/gradle-quality-plugin

### About

Static code analysis for Java and Groovy projects using [Checkstyle](https://checkstyle.sourceforge.io/), 
[PMD](https://pmd.github.io/), [CPD](https://pmd.github.io/), [SpotBugs](https://spotbugs.github.io/) 
and [CodeNarc](http://codenarc.sourceforge.net/).
Plugin implements unified console output for all quality plugins, which greatly simplifies developer workflow: 
only console required for working with violations and makes it feel the same as java compiler errors.

Features:
* Zero configuration by default: provided opinionated configs applied to all quality plugins
    - Default configuration files may be customized
* Adds extra javac lint options to see more warnings
* Complete console output for all quality plugins
* Html and xml reports for all plugins (custom xsl used for findbugs html report because it can't generate both xml and html reports)
* Grouping tasks to run registered quality plugins for exact source set (e.g. checkQualityMain)

##### Summary

* Configuration: `quality`
* Tasks:
    - `initQualityConfig` - copy default configs for customization 
    - `checkQuality[Main]` - run quality tasks for main (or any other) source set       
* Enable plugins: [Checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html),
[PMD](https://docs.gradle.org/current/userguide/pmd_plugin.html),
[SpotBugs](http://spotbugs.readthedocs.io/en/latest/gradle.html),
[CodeNarc](https://docs.gradle.org/current/userguide/codenarc_plugin.html)


### Setup

NOTE: when updating plugin version in your project don't forget to call `clean` task to remove cached configs from previous plugin version

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-quality-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-quality-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-quality-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-quality-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/quality/ru.vyarus.quality.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.quality)

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

Minimal requirements: java 8, gradle 5.1 

#### Compatibility

Plugin compiled for java 8, compatible with java 11

Gradle | Version
--------|-------
5.1     | 4.0.0
4.1     | [3.4.0](http://xvik.github.io/gradle-quality-plugin/3.4.0)
older   | [2.4.0](http://xvik.github.io/gradle-quality-plugin/2.4.0)

Java tools require `sourceCompatibility=1.8` (or above).
 
Version [3.3.0](http://xvik.github.io/gradle-quality-plugin/3.3.0) is the latest supporting `sourceCompatibility=1.6`  

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/gradle-quality-plugin)
* Select `Commits` section and click `Get it` on commit you want to use (you may need to wait while version builds if no one requested it before)
    or use `master-SNAPSHOT` to use the most recent snapshot

For gradle before 6.0 use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'ru.vyarus:gradle-quality-plugin:b9474cab84'
    }
}
apply plugin: 'ru.vyarus.quality'
```

For gradle 6.0 and above:

* Add to `settings.gradle` (top most!) with required commit hash as version:

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.namespace == 'ru.vyarus.quality') {
                  useModule('ru.vyarus:gradle-quality-plugin:b9474cab84')
              }
          }
      }
      repositories {
          maven { url 'https://jitpack.io' }
          gradlePluginPortal()          
      }
  }    
  ``` 
* Use plugin without declaring version: 

  ```groovy
  plugins {
      id 'ru.vyarus.quality'
  }
  ```  

</details>  

### Usage

Read [documentation](http://xvik.github.io/gradle-quality-plugin)

### Might also like

* [mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin) - beautiful project documentation generation
* [python-plugin](https://github.com/xvik/gradle-use-python-plugin) - use python modules in build
* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [java-library generator](https://github.com/xvik/generator-lib-java) - java library project generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
