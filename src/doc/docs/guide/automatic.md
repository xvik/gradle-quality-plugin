# Automatic mode

By default, plugin works in automatic mode an does not require configuration to run.

## Plugins

Java quality tool plugins will be activated if java sources are present (`src/main/java`):
[Checkstyle](../tool/checkstyle.md), 
[PMD](../tool/pmd.md) and 
[SpotBugs](../tool/spotbugs.md) (or [FindBugs](../tool/findbugs.md)).

Groovy quality plugins will be activated if groovy sources are present (`src/main/groovy`):
[CodeNarc](../tool/codenarc.md)

If you have both java and groovy sources then all plugins will be enabled (and they will not conflict).

If [animalsniffer](../tool/animalsniffer.md) plugin was manually registered then it would be 
configured the same way as other quality plugins (quality configuration unification).

## Scope

All quality plugins register a check task per source set. For example, `checkstyleMain`, `checkstyleTest`.
But `check` task depend only on quality tasks from configured scopes. 

By default, all activated plugins will check only main source set: only *Main quality tasks will be executed
during `check` (or `build`). You can call quality task for not configured source set manually (e.g. `checkstyleTest`).

!!! hint
    To enable test sources check: `quality.sourceSets = [sourceSets.main, sourceSets.test]`

!!! hint
    To run all checks for source set use [grouping task](../task/group.md)
    
## Configs

Plugin provides default configs for all tools.
These configs are opinionated: not all possible checks are enabled, just the sane majority of them. Also, some defaults were changed.
Anyway, all disabled checks are commented in config files, so it would be clear what was disabled.

You can modify one or more configs with [initConfigsTask](../task/config.md).  


## Manually registered plugins configuration 

If you register any quality plugin manually then it will be configured even if it's not supposed to be registered by project sources.

For example, project contains only java sources (`/src/main/java`) and codenarc plugin registered manually:

```groovy
plugins {
    id 'groovy'
    id 'codenarc'
    id 'ru.vyarus.quality'
}
```

Then quality plugin will register checkstyle, pmd and spotbugs (findbugs) plugins and configure codenarc plugin (which is not supposed to be used according to current sources). 

To prevent manually registered plugin configuration use referenced quality option. For example, to prevent codenarc plugin configuration in example above:

 ```groovy
 quality {
     codenarc = false
 }
 ```