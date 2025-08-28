# 6.0.0 Release notes

Summary:

* Configuration cache support
* `quality.sourceSets` configuration use source set names (instead of objects)
* "UP-TO-DATE"-related fixes for quality tasks (proper build cache support)
* New default tools side effects:
    - Checkstyle 11 requires java 17  (not enabled on java < 17)
    - Spotbugs 4.9.4 requires java 11 (not enabled on java < 11)
* Spotbugs plugin dependency is not auto-applied anymore (plugin must be declared manually now)
* New tasks:
    - `copyQualityConfigs` (used to prepare default quality configs)
    - `qualityToolVersions` (used to show active tool versions)
* New options:
    - `quality.fallbackToCompatibleToolVersion`  - auto downgrade spotbugs and checkstyle versions based on current java (for tests)
    - `quality.animalsniffer` - ability to disable animalsniffer plugin configuration
    - `quality.spotbugsQuiet` - disable spotbugs warnings (for annoying warnings)
* Gradle 7.1 as minimal requirement

[Migration guide](#migration-guide)

## Breaking changes

### Extension

`quality` extension now use gradle properties instead of fields.

For groovy builds, you'll have to use `=` to assign values: `quality.key = value`.
For kotlin builds, it _might_ be required to use `quality.key.set(value)` (in some cases).

`quality.sourceSets` option now accept strings instead of source set objects.
This was done to simplify configuration (especially for kotlin):
instead of `sourceSets = [project.sourceSets.main]` now use `sourceSets = ['main']`

Legacy configuration with object is available with method:


```groovy
quality.sourceSets(project.sourceSets.main, project.sourceSets.test)
```

or with list

```groovy
quality.sourceSets([project.sourceSets.main, project.sourceSets.test])
```

### Checkstyle

Checkstyle java 8 backport project **disappeared** (removed in maven central and from github), 
so checkstyle plugin **can't be used on java 8** anymore. 

Checkstyle 11 (configured by default) **requires java 17**.

!!! note "New behavior"
    Plugin will use checkstyle 11 on java 17 and **disable checkstyle automatically on java 8-16**.  
    If checkstyle 10 is configured manually: `quality.checkstyleVersion = '10.26.1'`, plugin would be
    enabled automatically on java 11.

### Spotbugs

The latest spotbugs plugin 6.x **requiers java 11** (also **the latest spotbugs itself**).
In order to not lose java 8 compatibility for projects not requiring spotbugs at all,
plugin **will not automatically apply spotbugs dependency** anymore.

!!! note "New behavior"
    Now you need to **apply spotbugs plugin manually**:
    ```groovy
    plugins {
        id 'java'
        id 'ru.vyarus.quality' version '6.0.0'
        id 'com.github.spotbugs' version '6.2.5' apply false
    }
    ```
    When spotbugs plugin is detected in classpath, it would be configured automatically.

Note that for multi-module builds, it would be enough to apply spotbugs plugin only in root module:
quality plugin will detect it and apply in submodules **automatically**.

Spotbugs plugin 5.x could be used **if java 8 compatibility is required**:

```groovy
plugins {
    id 'com.github.spotbugs' version '5.2.5' apply false
}
```

"apply false" is required because otherwise spotbugs plugin would be active on java 8

#### Quiet mode

Sometimes spotbugs produce nasty warnings like:

```
The following classes needed for analysis were missing:
  org.junit.Before
  org.junit.After
```

Moreover, you will see it 2 times (for each generated report).

There is nothing you could do about it, so using spotbugs [-quiet option](https://spotbugs.readthedocs.io/en/stable/running.html#output-options)
is a good choice. To automatically apply quiet for all spotbugs tasks:

```groovy
quality.spotbugsQuiet = true
```
 
### Java 11 support

If your project runs on java 11 - 16 and requires checkstyle run on it: by default, **plugin will disable checkstyle on java 11**
(it is completely ok to run quality tools only on java 17).

If you **need to run checkstyle on java 11 (16)** then specify checkstyle 10.x: `quality.checkstyleVersion = '10.26.1'` and plugin will
**enable checkstyle automatically** (recognize a compatible version).

No changes for projects not using checkstyle.

See [migration guide](#migration-guide) for more details.

### Java 8 support

If your project runs on **java 8** (for example, on CI) and you **need spotbugs**: then you can't use spotbugs plugin 6.x 
(there is _no way in gradle to disable plugin for specific java version_).
 
You'll have to use spotbugs plugin 5.x (it is compatible with java 8) and reduce spotbugs version:

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.quality' version '6.0.0'
    id 'com.github.spotbugs' version '5.2.5' apply false
}

quality {
    spotbugsVersion = '4.8.6'
}
```

See [migration guide](#migration-guide) for more details.

No changes for projects not using spotbugs.

## Gradle 7.0

Gradle 7.0 is not supported anymore. **Minimal supported version is 7.1**.

This is due to minimal requirement of spotbugs plugin 6.x (gradle 7.0 simply not tested now 
for compatibility)

## Gradle caches

Before, plugin was using doFirst/doLast hooks on quality tasks to configure them.
It was causing problems with configuration and even build cache (quality tasks UP-TO-DATE checks).

Plugin is now **fully compatible with configuration cache**

To solve cache problems, plugin now use a separate task: `copyQualityConfigs`, which
prepares all required config files (default configs) for quality tasks.

As the task prepares config files before quality tasks execution, quality tasks UP-TO-DATE
check (**build cache**) **is correct now** (you'll see fewer executions)

!!! note
    Quality plugin prints console output even for UP-TO-DATE quality tasks (so task may not execute, 
    but a console report would be shown)

## Tool versions task

There is a new `qualityToolVersions` task, showing configured versions for active tools.

For example, for java project output would look like:

```
> Task :qualityToolVersions
Java version: 17
Gradle version: 8.14.3
Checkstyle: 11.0.0
PMD: 7.16.0
```

If some tools were disabled (but applicable for current project sources), 
it would be notified:

```
> Task :qualityToolVersions
Java version: 8
Gradle version: 8.14.3
Checkstyle: disabled
PMD: 7.16.0
SpotBugs: disabled
```

## CPD

* Fixed [cpd plugin](https://github.com/aaschmid/gradle-cpd-plugin) 3.5 compatibility
* Updated default xsl file (used for html report creation)

## Animlansiffer

Added `quality.animalsniffer` option to be able to diable animalsniffer plugin configuration:

```
quality.animalsniffer = false
```

## Updated tools and default configs

Updated tool versions:

| Tool          | Old version | New version | Notes                                     |
|---------------|-------------|-------------|-------------------------------------------|
| Checkstyle    | 10.12.7     | 11.0.0      | java 21 records support, **requires java 17** |
| Spotbugs      | 4.8.3       | 4.9.4       | **requires java 11**                          |
| PMD           | 6.55        | 7.16.0      | java 25 support                           |
| CodeNarc      | 3.4.0       | 3.6.0       |                                           |

### Checkstyle

Default value for `quality.checkstyle` now depends on configured `checkstyleVersion`:

* For checkstyle 11 (default) `quality.checkstyle = false` on java < 17
* For checkstyle 10 (manually declared `quality.checkstyleVersion = '10.26.1'`) `quality.checkstyle = false` on java < 11

Default config changes:

- Add [PatternVariableAssignment](https://checkstyle.sourceforge.io/checks/coding/patternvariableassignment.html)
- Add [UnnecessaryNullCheckWithInstanceOf](https://checkstyle.sourceforge.io/checks/coding/unnecessarynullcheckwithinstanceof.html)
- Add [ConstructorsDeclarationGrouping](https://checkstyle.sourceforge.io/checks/coding/constructorsdeclarationgrouping.html)

### Spotbugs

Default value for `qulity.spotbugs` now depends on configured `spotbugsVersion`:

* For spotbugs 4.9 (default) `quality.spotbugs = false` on java < 11
* For spotbugs 4.8 (manually declared `quality.spotbugsVersion = '4.8.6'`) `quality.spotbugs = true`

Fixed support for `quality.exclude` and `quality.excludeSources` exclusions for multiple souces sets
(such excludes applied as dynamically generated rules in spotbugs exclusion xml config)

### PMD

Pmd 7 now requires two separate jars: pmd-ant and pmd-java.
Native pmd 7 support was [added in gradle 8.3](https://github.com/gradle/gradle/issues/24502)

To support running pmd on older (<8.3) gradle versions, the plugin will manually override the default pmd classpath:

```groovy
dependencies {
    pmd("net.sourceforge.pmd:pmd-ant:${quality.pmdVersion.get()}")
    pmd("net.sourceforge.pmd:pmd-java:${quality.pmdVersion.get()}")
}
```

Updated default config:

* Remove legacy rules
* Remove [AvoidLiteralsInIfCondition](https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_errorprone.html#avoidliteralsinifcondition)
* Remove [AvoidSynchronizedStatement](https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_multithreading.html#avoidsynchronizedstatement)
* Remove [AvoidSynchronizedAtMethodLevel](https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_multithreading.html?#avoidsynchronizedatmethodlevel)
* Remove [ImplicitFunctionalInterface](https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_bestpractices.html#implicitfunctionalinterface)
* Change [CouplingBetweenObjects](https://docs.pmd-code.org/pmd-doc-7.16.0/pmd_rules_java_design.html#couplingbetweenobjects) 20 -> 25

Removed stale `quality.pmdIncremental` property. It was required for gradle 5.6 - 6.3 only (currently not supported).

### CodeNarc

Update default config:

* Remove new rule [NonSerializableFieldInSerializableClass](https://codenarc.org/codenarc-rules-serialization.html#nonserializablefieldinserializableclass-rule)
  to avoid enhanced rules enabled warning (and potential problems)

## Auto fallback tools

There is a new option to automatically fallback spotbugs and checkstyle versions according
to context java version:

```groovy
quality.fallbackToCompatibleToolVersion = true
```

!!! warning
    This option was added to simplify plugin testing. It is not recommended to use it in production
    because, if you run project on different java versions (on CI), then different
    tool versions would be used, which may cause different results. Better restrict quality tasks to 
    upper-most compatible java version.

## Migration guide

For groovy build scripts it is required to use `=` for extension properties assignment:

```groovy
quality.spotbugs = true
```

For kotlin builds it might be required to use `property.set(value)` in some cases

`quality.sourceSets` now accept source set names, so instead of `[project.sourceSets.main]` use `['main']` as value.
Legacy configuration is available with method: `quality.sourceSets(project.sourceSets.main)`

The following only affects java projects, using checkstyle and/or spotbugs.

### Spotbugs

If you want to use spotbugs, apply plugin manually:

```groovy
 plugins {
    id 'java'
    id 'ru.vyarus.quality' version '6.0.0'
    id 'com.github.spotbugs' version '6.2.5' apply false
}
```

In multi-module projects, it would be enough to apply spotbugs plugin only in root module:

```groovy
plugins {
    id 'com.github.spotbugs' version '6.2.5' apply false
}
```

## Java 11 support

Checkstyle 11 (configured by default) will be disabled on java 11 automatically. 
This is completely fine: _no need to run tools on multiple java versions_.

If you **only use java 11** then checkstyle could be enabled by downgrading its version:

```groovy
quality.checkstyleVersion = '10.26.1'
```

!!! note
    There is an issue with spotbugs plugin 6.x on **windows** java 11:

    ```
    java.io.IOException: No files to analyze could be opened
        at edu.umd.cs.findbugs.FindBugs2.execute(FindBugs2.java:302)
        at edu.umd.cs.findbugs.FindBugs.runMain(FindBugs.java:390)
        at edu.umd.cs.findbugs.FindBugs2.main(FindBugs2.java:1223)
    ```
    
    It is because of empty "onlyAnalyze" cli parameter (jdk arguments parsing bug). 
    There is nothing you can do about it, except using spotbugs plugin 5.x (not affected by this problem).
    Problem [reported](https://github.com/spotbugs/spotbugs-gradle-plugin/issues/1432), waiting for a fixed version.


## Java 8 support

There is no way to enable checkstyle on java 8. But it would not be a problem: 
plugin will not try to enable checkstyle on java 8. 

Spotbugs plugin 6.x is not compatible with java 8. In case when you use multiple jvm on CI: 8, 11, 17,
there is no way to use spotbugs plugin 6.x only on java 11 and 17 (not possible to apply plugin conditionally).

You can only downgrade spotbugs plugin to 5.x (compatible with java 8):

```groovy
plugins {
    id 'com.github.spotbugs' version '5.2.5' apply false
}
```

"apply false" is required because otherwise spotbugs plugin would be active on java 8

!!! warning
    There is a side effect: as quality plugin is not configuring spotbugs plugin, it will not
    apply annotations dependency. So, if you use spotbugs annotations, you will need to add them manually:

    ```groovy
    compileOnly 'com.github.spotbugs:spotbugs-annotations:4.8.6'
    ```

!!! note "Alternative (not recommended)"
    The problem with spotbugs plugin 6.x on java 8 would be that gradle would not be able to
    find a compatible version (thanks to gradle metadata). But you can cheat gradle to ignore 
    java version checks:

    ```java
    import org.gradle.api.attributes.java.TargetJvmVersion
    
    buildscript {
        configurations.classpath.attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
    }
    
    plugins {
        id 'java'
        id 'ru.vyarus.quality' version '6.0.0'
        id 'com.github.spotbugs' version '6.2.5' apply false
    }
    ```
    This way, gradle would apply spotbugs plugin 6.x on java 8, but the quality plugin will not
    activate it, and so everything would work fine.


Also, spotbugs 4.9.x (tool) is also not compatible with java 8.

If you run **on multiple java versions**, then do nothing: plugin would not
enable spotbugs on java 8 (and spotbugs would be used on java 11 and above).

If you run **only on java 8**, then downgrade spotbugs to 4.8.x:

```groovy
quality.spotbugsVersion = '4.8.6'
```
