# SpotBugs

!!! summary ""
    Java | 
    [Home](https://spotbugs.github.io) | 
    [Release Notes](https://github.com/spotbugs/spotbugs/blob/master/CHANGELOG.md) |
    [Plugin](http://spotbugs.readthedocs.io/en/latest/gradle.html)     

!!! info
    SpotBugs is a successor project to [deprecated FindBugs](https://github.com/findbugsproject/findbugs) project.
    [Migration guide](http://spotbugs.readthedocs.io/en/latest/migration.html). If you were using custom 
    findbugs config before then rename it's folder to `spotbugs`.
    
!!! warning
    In contrast to other plugins, [spotbugs plugin](http://spotbugs.readthedocs.io/en/latest/gradle.html) is not bundled with gradle,
    but quality plugin will bring it as a dependency (v 2.0.1) and activate automatically.
    To use newer spotbugs plugin version simply enable plugin manually (in `plugins` section).    
    
By default, plugin activates if java sources available (`src/main/java`).    

SpotBugs configuration differ from other tools (checkstyle, pmd): instead of exact rules configuration
it uses [efforts level](http://spotbugs.readthedocs.io/en/latest/effort.html). Deeper level could reveal more bugs, but with higher mistake possibility. 
Default settings (`max` effort and `medium` level) are perfect for most cases. Some checks were disabled in the default 
[filter file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/exclude.xml)

!!! note
    Special [xsl file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/html-report-style.xsl) 
    used for manual html report generation because spotbugs plugin could generate either xml or html report and not both. 

## Output

```
2 (0 / 2 / 0) SpotBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:8) [priority 2 / rank 14]
	>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.
  
...  
```

Counts in braces show priorities (p1/p2/p3).

!!! note
    There is no link to spotbugs site (like other tools), because report already contains [everything from there](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html). 

!!! tip
    Both [priority](https://spotbugs.readthedocs.io/en/stable/filter.html#confidence) 
    and [rank](https://spotbugs.readthedocs.io/en/stable/filter.html#rank) are shown for violations: `[priority 2 / rank 14]`. 
    Priority relates to `spotbugsLevel` setting and rank to `spotbugsMaxRank`.   

## Config

Tool config options with defaults:

```groovy
quality {
    spotbugsVersion = '3.1.12'
    spotbugs = true // false to disable automatic plugin activation
    spotbugsEffort = 'max'  // min, less, more or max
    spotbugsLevel = 'medium' // low, medium, high
    spotbugsMaxRank = 20 // 1-4 scariest, 5-9 scary, 10-14 troubling, 15-20 of concern  
    spotbugsMaxHeapSize = '1g'
}
```

!!! attention 
    Gradle 5 [reduced default memory settings](https://github.com/gradle/gradle/issues/6216) and so default memory for 
    spotbugs task become `512mb` (instead of `1/4 of physical memory` as it was before). 
    To reduce the impact (as spotbugs task is memory-consuming), quality plugin sets now default
    memory to `1g`. If your project requires more memory for spotbugs, increase it with `spotbugsMaxHeapSize` option:
    `spotbugsMaxHeapSize='2g'` 
    
    Note that quality pligin setting is applied only if sotbugs task was not configured manually, for example, with
    `spotbugsMain.maxHeapSize = '2g'`.

## Suppress

To suppress violations you can use [filter file](http://spotbugs.readthedocs.io/en/latest/filter.html).
In this case you need to override [default filter file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/exclude.xml).

Or you can use annotations. SpotBugs use custom annotations and so you need to add 
`com.github.spotbugs:spotbugs-annotations:3.1.2` dependency (with provided scope if possible) and use:

```java
@SuppressFBWarnings("URF_UNREAD_FIELD")
```

!!! abstract
    Spotbugs can't use default `@SuppressWarnings` annotation because it's a source annotation
    and not available in bytecode. 

## Plugins

You may add additional spotbugs checks by declaring [spotbugs plugins](https://spotbugs.readthedocs.io/en/latest/gradle.html#introduce-spotbugs-plugin).

!!! warning
    As, by default, spotbugs plugin applied automatically after configuration read, `spotbugsPlugins` configuration can't be used directly
    
You can register plugins using quality extension shortcut:

```groovy
quality {
    spotbugsPlugin 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.0'
    spotbugsPlugin 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'        
}
```     

!!! note
    Rules from plugins would be identified in console output:
    
    ```
    [fb-contrib project | Correctness | FCBL_FIELD_COULD_BE_LOCAL] sample.(Sample.java:11)  [priority 2 / rank 7]
        >> Class sample.Sample defines fields that are used only as locals
      This class defines fields that are used in a locals only fashion,
      specifically private fields or protected fields in final classes that are accessed
      first in each method with a store vs. a load. This field could be replaced by one
      or more local variables.
    ```

Alternatively, you can use `afterEvaluate` to register directly in `spotbugsPlugins` configuration:

```groovy
afterEvaluate {
    dependencies {
        spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'
    }
}
```

Or declare spotbugs plugin manually (it will be still configured by quality plugin)
and use `spotbugsPlugins` configuration directly:

```groovy
plugins {
    id 'com.github.spotbugs' version '2.0.1'
}
dependencies {
    spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'
}
```      

!!! tip
    All these approaches could work together, but better stick to one.

### Available plugins

[Find Security Bugs](https://github.com/find-sec-bugs/find-sec-bugs) ([site](https://find-sec-bugs.github.io/))

```groovy   
quality {
    spotbugsPlugin 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.0'
}
```

[fb-contrib: A FindBugs auxiliary detector plugin](https://github.com/mebigfatguy/fb-contrib) ([site](http://fb-contrib.sourceforge.net/))

```groovy         
qualtiy {
    spotbugsPlugin 'com.mebigfatguy.fb-contrib:fb-contrib:7.4.7'
}
```

## Annotations

Use spotbugs-annotations to guide spotbugs nullability checks (`#!java @Nonnull` and `#!java @Nullable`).
Add ``com.github.spotbugs:spotbugs-annotations:3.1.2`` dependency (with provided scope if possible).

!!! warning
    Before,  annotations from Jsr-305 [were used](http://findbugs.sourceforge.net/manual/annotations.html) 
    (`com.google.code.findbugs:jsr305`), but now it is dead.
    Remove jsr-305 jar if it were used and use [undeprecated](https://github.com/spotbugs/spotbugs/issues/130)
    `#!java @Nonnull` and `#!java @Nullable`

In some cases you will have to use it.
For example, you may face issues with guava functions or predicates:

```
[NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE] input must be nonnull but is marked as nullable 
```

The reason for this is that guava use `@Nullable` annotation, which is `@Inherited`, so
even if you not set annotation on your own function or predicate it will still be visible.

The simplest workaround is to set `@Nonnull` annotation on your function or predicate:

```java
public boolean apply(@Nonnull final Object input) {
```

!!! hint
    `NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION` check was disabled because it does not allow this workaround to work

!!! abstract
    Guava is now using checker framework [instead of jsr-305](https://github.com/google/guava/issues/2960).