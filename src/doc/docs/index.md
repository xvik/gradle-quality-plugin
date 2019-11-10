# Welcome to gradle quality plugin

!!! summary ""
    Plugin activates and configures quality tools for java and groovy projects using 
    [Checkstyle](tool/checkstyle.md), 
    [PMD](tool/pmd.md),
    [CPD](tool/cpd.md),
    [SpotBugs](tool/spotbugs.md),     
    [CodeNarc](tool/codenarc.md). 
    Plugin unifies console output for all quality plugins which greatly simplifies developer workflow: 
    only console is required for working with violations and makes it feel the same as java compiler errors.
    
!!! note
    Google's [error-prone](http://errorprone.info/) is not included because checkstyle and pmd covers all
    error-prone checks and quality plugin makes all tools behave the same way as error-prone, but without java compiler modifications
    and [environment specific setup](https://github.com/tbroyer/gradle-errorprone-plugin#requirements).          

[Release notes](about/history.md) - [License](about/license.md)

## Main Features

* Adds extra javac lint options to see more warnings
* Complete and unified console output for all quality plugins
* Html and xml reports configured for all plugins
* Zero configuration by default: provided opinionated configs will make it work out of the box
* Easy configs customization 
* Grouping tasks to run registered quality plugins for exact source set

## How to use docs

* [**Getting started**](getting-started.md) covers installation and main usage info. Ideal for introduction.
* [**User guide**](guide/automatic.md) section contain detailed behavior description. Good to read, but if no time, read as you need it.
    * [**Configuration**](guide/config.md) - configuration reference
* [**Tools**](tool/lint.md) section describes exact quality tool configuration and usage aspects. Use it as a *hand book*.
* [**Tasks**](task/config.md) section describes custom tasks.
