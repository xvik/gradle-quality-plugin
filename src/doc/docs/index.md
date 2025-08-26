# Welcome to gradle quality plugin

!!! summary ""
    Plugin configures quality tools for java and groovy projects.

    * Auto enabled plugins: [Checkstyle](tool/checkstyle.md), 
    [PMD](tool/pmd.md),
    [CodeNarc](tool/codenarc.md).
    * External plugins (configured when manually applied):
    [SpotBugs](tool/spotbugs.md),
    [CPD](tool/cpd.md), 
    [AnimalSniffer](tool/animalsniffer.md),

Plugin unifies console output for all quality plugins, which greatly simplifies developer workflow: 
only console required for working with violations and makes it feel the same as java compiler errors.

Also, plugin provides default (opinionated) configurations for quality tools, so you 
can use them with zero configuration.

**[Release Notes](about/release-notes.md)** - [History](about/history.md)  - [License](about/license.md)

## Main Features

* Zero configuration by default: provided opinionated configs applied to all quality plugins
    - Default configuration files may be customized
* Adds extra javac lint options to see more warnings
* Complete console output for all quality plugins
* Html and xml reports for all plugins (custom xsl used for findbugs html report because it can't generate both xml and html reports)
* Grouping tasks to run registered quality plugins for exact source set (e.g. checkQualityMain)

!!! note
    The plugin is **compatible** with the gradle [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)

## How to use docs

* [**Getting started**](getting-started.md) covers installation and main usage info. Ideal for introduction.
* [**User guide**](guide/automatic.md) section contain detailed behavior description. Good to read, but if no time, read as you need it.
    * [**Configuration**](guide/config.md) - configuration reference
* [**Tools**](tool/lint.md) section describes exact quality tool configuration and usage aspects. Use it as a *hand book*.
* [**Tasks**](task/config.md) section describes custom tasks.

## Samples

* [Java project](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/java)
* [Groovy project](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/groovy)
* [Java + generated code + exclusion by source dir](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/java-codegen)
* [Java + generated code + exclusion by source path](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/java-codegen2)
* [Java + apt](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/java-apt)
* [CPD](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/cpd)
* [AnimalSniffer](https://github.com/xvik/gradle-quality-plugin/tree/master/src/gradleTest/animalsniffer)