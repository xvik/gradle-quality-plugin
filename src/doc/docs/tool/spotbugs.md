# SpotBugs

!!! summary ""
    Java | [Home](https://spotbugs.github.io) | [Plugin](http://spotbugs.readthedocs.io/en/latest/gradle.html)

!!! info
    SpotBugs is a successor project to [deprecated FindBugs](https://github.com/findbugsproject/findbugs) project.
    [Migration guide](http://spotbugs.readthedocs.io/en/latest/migration.html). If you were using custom 
    findbugs config before then rename it's folder to `spotbugs`.
    
!!! warning
    In contrast to other plugins, [spotbugs plugin](http://spotbugs.readthedocs.io/en/latest/gradle.html) is not bundled with gradle,
    but quality plugin will bring it as a dependency (v 1.6.1) and activate automatically.
    To use newer spotbugs plugin version simply enable plugin manually (in `plugins` section).    
    
By default, plugin is activated if java sources available (`src/main/java`).    

SpotBugs configuration differ from other tools (checkstyle, pmd): instead of exact rules configuration
it uses [efforts level](http://spotbugs.readthedocs.io/en/latest/effort.html). Deeper level could reveal more bugs, but with higher mistake possibility. 
Default settings ('max' effort and 'medium' level) are perfect for most cases. Some checks were disabled in the default 
[filter file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/exclude.xml)

!!! note
    Special [xsl file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/html-report-style.xsl) 
    used for manual html report generation because spotbugs plugin could generate either xml or html report and not both. 

## Output

```
2 (0 / 2 / 0) SpotBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:8) [priority 2]
	>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.
  
...  
```

Counts in braces show priorities (p1/p2/p3).

!!! note
    There is no link to spotbugs site (like other tools), because report already contains [everything from there](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html).

## Config

Tool config options with defaults:

```groovy
quality {
    spotbugsVersion = '3.1.2'
    spotbugs = true // false to disable automatic plugin activation
    spotbugsEffort = 'max'  // min, less, more or max
    spotbugsLevel = 'medium' // low, medium, high
}
```

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

You may add additional spotbugs checks by declaring spotbugs plugins in `spotbugsPlugins` dependency configuration.

!!! note
    Spotbugs is compatible with findbugs plugins.

!!! warning
    As, by default, spotbugs plugin is automatically applied after configuration read, `spotbugsPlugins` can't be used directly

Either use afterEvaluate:

```groovy
afterEvaluate {
    dependencies {
        spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.2.0'
    }
}
```

Or declare spotbugs plugin manually (it will be configured by quality plugin):

```groovy
plugins {
    id 'com.github.spotbugs' version '1.6.1'
}
dependencies {
    spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.2.0'
}
```

### Available plugins

[Find Security Bugs](http://find-sec-bugs.github.io/)

```groovy
spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.7.1'
```

[fb-contrib: A FindBugs auxiliary detector plugin](http://fb-contrib.sourceforge.net/)

```groovy
spotbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:7.2.0'
```

## Annotations

Use spotbugs-annotations to guide spotbugs nullability checks (`#!java @Nonnull` and `#!java @Nullable`).
Add ``com.github.spotbugs:spotbugs-annotations:3.1.2`` dependency (with provided scope if possible).

!!! warning
    Before,  annotations from Jsr-305 [were used](http://findbugs.sourceforge.net/manual/annotations.html) 
    (`com.google.code.findbugs:jsr305`), but now it is dead.
    Remove jsr-305 jar if it were used and use [undeprecated](https://github.com/spotbugs/spotbugs/issues/130))
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

!!! note
    `NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION` check is disabled because it does not allow this workaround to work

!!! abstract
    Guava is now using checker framework [instead of jsr-305](https://github.com/google/guava/issues/2960).