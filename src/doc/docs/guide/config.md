# Configuration

Use `quality` closure to configure plugin.
Defaults:

```groovy
quality {
    
    // Tools versions
    
    checkstyleVersion = '8.12'
    pmdVersion = '6.7.0'    
    spotbugsVersion = '3.1.6'
    codenarcVersion = '1.2.1'
    animalsnifferVersion

    /**
     * When disabled, quality plugins will not be registered automatically (according to sources). 
     * Only manually registered quality plugins will be configured. 
     */
    autoRegistration = true

    // Enable/disable tools (when auto registration disabled control configuration appliance)
     
    checkstyle = true
    pmd = true
    // when spotbugs disabled, findbugs plugin is used by default
    spotbugs = true
    codenarc = true
    
    /**
     * The analysis effort level. The value specified should be one of min, default, or max.
     * Higher levels increase precision and find more bugs at the expense of running time and
     * memory consumption. Default is 'max'.
     */
    spotbugsEffort = 'max'
    
    /**
     * The priority threshold for reporting bugs. If set to low, all bugs are reported.
     * If set to medium, medium and high priority bugs are reported.
     * If set to high, only high priority bugs are reported. Default is 'medium'.
     */
    spotbugsLevel = 'medium'

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
    enabled = true
    
    /**
     * When false, disables reporting quality issues to console. Only gradle general error messages will
     * remain in logs. This may be useful in cases when project contains too many warnings.
     * Also, console reporting require xml reports parsing, which could be time consuming in case of too
     * many errors (large xml reports).
     * True by default.
     */
    consoleReporting = true
    
    /**
     * When false, no html reports will be built. True by default.
     */
    htmlReports = true

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
     * Spotbugs (Findbugs) does not support exclusion directly, but plugin will resolve excluded classes and apply
     * them to xml exclude file (default one or provided by user).
     * 
     * By default nothing is excluded.
     * 
     * IMPORTANT: Patterns are checked relatively to source set dirs (not including them). So you can only
     * match source files and packages, but not absolute file path (this is gradle specific, not plugin).
     *
     * @see org.gradle.api.tasks.SourceTask#exclude(java.lang.Iterable) (base class for all quality tasks)
     */
    exclude = []
        
     /**
      * Direct sources to exclude from checks (except animalsniffer).
      * This is useful as last resort, when extension or package is not enough for filtering.
      * Use {@link Project#files(java.lang.Object)} or {@link Project#fileTree(java.lang.Object)}
      * to create initial collections and apply filter on it (using
      * {@link org.gradle.api.file.FileTree#matching(groovy.lang.Closure)}).
      * 
      * Plugin will include files into spotbugs (findbugs) exclusion filter xml (default one or provided by user).
      * 
      * Note: this must be used when excluded classes can't be extracted to different source set and
      * filter by package and filename is not sufficient.
      */
     FileCollection excludeSources   

    /**
     * User configuration files directory. Files in this directory will be used instead of default (bundled) configs.
     */
    configDir = 'gradle/config/'
    
    
    // Deprecated due to switch to SpotBugs by default
    findbugsVersion = '3.0.1'
    findbugs = true
    findbugsEffort = 'max'
    findbugsLevel = 'medium'
}
```

## Manual mode

You can disable [automatic quality plugins registration](automatic.md) (guided by source detection) 
and register required plugins manually:

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

## Disable console output

In some cases it may not be desired to see errors in console. For example, when quality control applied on existing project
and you have thousands of warnings.

```groovy
quality {
    consoleReporting = false
}
```

!!! warning 
    Don't turn off console warnings in other cases: people tend to ignore problems they didn't see 
    (practice shows that normally almost no one looks html reports of quality tools). You must see warnings for
    each build to finally fix them all someday (or fix them as they appear).

Console reporting use xml reports, produced by quality plugins. In case of too many errors, xml parsing could slow down build.
You may use reporting disabling to speed up build a bit. In most cases (when you don't have thousands of errors) console reporting [will be fast](profile.md). 

## Html reports

By default, all plugins are configured to generate both xml and html reports. Xml report is
required for console output and html report preserved for consultation.
If you don't need html reports (e.g. on ci server) they could be disabled:

```groovy
quality {
    htmlReports = false
}
```

## Disable quality plugins

If you want to disable all quality checks:

```groovy
quality {
    enabled = false
}
```

This will disable all quality tasks (by setting `task.enabled = false` for each quality task). Quality tasks will still be visible, but marked as SKIPPED on execution.

!!! note
    `enable` state will not affect tasks called directly. For example, you set `quality.enabled = false` and call `checkstyleMain` - it will be executed.
    Motivation is simple - if you call task directly then you want it to work. 

Also, enabled state not affects quality tasks when quality grouping tasks called. For example, if you call `checkQualityMain` - all quality plugins will be executed,
even if disabled in configuration. Motivation is the same as with direct call - you obviously want to perform quality checks.

!!! note 
    if quality grouping task called as dependency of other task, quality plugins will be skipped. Exceptions applies only to direct cases when expected behaviour is obvious.

## Configuration override

It is still possible to configure quality plugins, but direct configuration closures will not work:

!!! fail
    ```groovy
    checkstyle {  // will not work, because plugin will override it
        ...
    }
    ```

But will work like this:

!!! success
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
* [SpotBugsExtension](http://spotbugs.readthedocs.io/en/latest/gradle.html#configure-gradle-plugin)
* [FindBugsExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.FindBugsExtension.html)
* [CodeNarcExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.CodeNarcExtension.html)

