# FindBugs

!!! summary ""
    Java | 
    [Home](http://findbugs.sourceforge.net) | 
    [Release Notes](http://findbugs.sourceforge.net/Changes.html) |
    [Plugin](https://docs.gradle.org/current/userguide/findbugs_plugin.html)     

!!! danger
    FindBugs support is deprecated, because project is abandoned. Use SpotBugs [successor project](https://github.com/findbugsproject/findbugs) instead.
    By default, quality plugin will activate [SpotBugs](spotbugs.md) and not findbugs.
    
!!! abstract "Migration"
    If you were using custom configs then rename `findbugs` folder to `spotbugs`.
    If plugins were used then change `findbugsPlugins` to `spotbugsPlugins`.
    [Migration guide](http://spotbugs.readthedocs.io/en/latest/migration.html)
        
!!! hint
    If you want to continue using FindBugs instead of SpotBugs then simply disable spotbugs:
    `quality.spotbugs = false`. This will force automatic findbugs activation, as before. 
    If findbugs plugin is enabled manually (in `plugins` section) then spotbugs will not be activated automatically
    and findbugs will be configured.
    FindBugs support is marked as deprecated, but it *will not* be removed soon.
        
    
By default, plugin is activated if java sources available (`src/main/java`) and [spotbugs plugin](spotbugs.md) disabled 
(or findbugs plugin enabled manually).    

Findbugs configuration differ from other tools (checkstyle, pmd): instead of exact rules configuration
it uses efforts level. Deeper level could reveal more bugs, but with higher mistake possibility. 
Default settings ('max' effort and 'medium' level) are perfect for most cases.

!!! note
    Special [xsl file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/findbugs/html-report-style.xsl) 
    used for manual html report generation because findbugs plugin could generate either xml or html report and not both. 

## Output

```
2 (0 / 2 / 0) FindBugs violations were found in 2 files

[Performance | URF_UNREAD_FIELD] sample.(Sample.java:8) [priority 2]
	>> Unread field: sample.Sample.sample
  This field is never read. Consider removing it from the class.
  
...  
```

Counts in braces show priorities (p1/p2/p3).

!!! note
    There is no link to findbugs site (like other tools), because report already contains everything from there.

## Config

Tool config options with defaults:

```groovy
quality {
    findbugsVersion = '3.0.1'
    findbugs = true // false to disable automatic plugin activation
    findbugsEffort = 'max'  // min, default or max
    findbugsLevel = 'medium' // low, medium, high
}
```

## Suppress

To suppress violations you can use [filter file](http://findbugs.sourceforge.net/manual/filter.html).
In this case you need to override [default filter file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/findbugs/exclude.xml).

Or you can use annotations. FindBugs use custom annotations and so you need to add 
`com.google.code.findbugs:annotations:3.0.0` dependency (with provided scope if possible) and use:

```java
@SuppressFBWarnings("URF_UNREAD_FIELD")
```

## Plugins

You may add additional findbugs checks by declaring findbugs plugins in `findbugsPlugins` dependency configuration.

!!! warning
    As, by default, findbugs plugin is automatically applied after configuration read, `findbugsPlugins` can't be used directly

Either use afterEvaluate:

```groovy
afterEvaluate {
    dependencies {
        findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.4.1'
    }
}
```

Or declare findbugs plugin manually (it will be configured by quality plugin):

```groovy
plugins {
    id 'findbugs'
}
dependencies {
    findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.4.1'
}
```

### Available plugins

[Find Security Bugs](http://find-sec-bugs.github.io/)

```groovy
findbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.4.4'
```

[fb-contrib: A FindBugs auxiliary detector plugin](http://fb-contrib.sourceforge.net/)

```groovy
findbugsPlugins 'com.mebigfatguy.fb-contrib:fb-contrib:6.6.0'
```

## Annotations

You may use [jsr305 annotations](http://findbugs.sourceforge.net/manual/annotations.html) to guide findbugs.
Add `com.google.code.findbugs:jsr305:3.0.0` dependency (with provided scope if possible).

In some cases you will have to use it.
For example, you may face issues with guava functions or predicates:

```
[NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE] input must be nonnull but is marked as nullable 
```

The reason for this is that guava use `@Nullable` annotation, which is `@Inherited`, so
even if you not set annotation on your own function or predicate it will still be visible.

The simplest workaround is to set `@Nonnull` annotation (jsr305) on your function or predicate:

```java
public boolean apply(@Nonnull final Object input) {
```
