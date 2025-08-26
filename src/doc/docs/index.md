# Welcome to gradle quality plugin

!!! summary ""
    Plugin activates and configures quality tools for java and groovy projects using 
    [Checkstyle](tool/checkstyle.md), 
    [PMD](tool/pmd.md),
    [CPD](tool/cpd.md),
    [SpotBugs](tool/spotbugs.md),     
    [CodeNarc](tool/codenarc.md). 
    Plugin unifies console output for all quality plugins, which greatly simplifies developer workflow: 
    only console required for working with violations and makes it feel the same as java compiler errors.
    
!!! note
    Google's [error-prone](http://errorprone.info/) is not included because checkstyle and pmd cover all
    error-prone checks and quality plugin makes all tools behave the same way as error-prone, but without java compiler modifications
    and [environment specific setup](https://github.com/tbroyer/gradle-errorprone-plugin#requirements).          

**[Release Notes](about/release-notes.md)** - [History](about/history.md)  - [License](about/license.md)

## Main Features

* Zero configuration by default: provided opinionated configs applied to all quality plugins
    - Default configuration files may be customized
* Adds extra javac lint options to see more warnings
* Complete console output for all quality plugins
* Html and xml reports for all plugins (custom xsl used for findbugs html report because it can't generate both xml and html reports)
* Grouping tasks to run registered quality plugins for exact source set (e.g. checkQualityMain)

!!! note
    The plugin is **not compatible** with the gradle [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)

## How to use docs

* [**Getting started**](getting-started.md) covers installation and main usage info. Ideal for introduction.
* [**User guide**](guide/automatic.md) section contain detailed behavior description. Good to read, but if no time, read as you need it.
    * [**Configuration**](guide/config.md) - configuration reference
* [**Tools**](tool/lint.md) section describes exact quality tool configuration and usage aspects. Use it as a *hand book*.
* [**Tasks**](task/config.md) section describes custom tasks.
