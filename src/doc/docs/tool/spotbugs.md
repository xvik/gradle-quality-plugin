# SpotBugs

!!! summary ""
    Java | 
    [Home](https://spotbugs.github.io) | 
    [Release Notes](https://github.com/spotbugs/spotbugs/blob/master/CHANGELOG.md) |
    [Plugin](http://spotbugs.readthedocs.io/en/latest/gradle.html)     

!!! info
    SpotBugs is a successor project to [deprecated FindBugs](https://github.com/findbugsproject/findbugs) project.
    [Migration guide](http://spotbugs.readthedocs.io/en/latest/migration.html). If you were using custom 
    findbugs config before then rename its folder to `spotbugs`.
    
!!! warning
    In contrast to other plugins, [spotbugs plugin](http://spotbugs.readthedocs.io/en/latest/gradle.html) is not bundled with gradle
    and must be applied manually:
    
    ```groovy
    plugins {
        id 'com.github.spotbugs' version '{{ gradle.spotbugsPlugin }}' apply false
    }
    ```

    On multi-module projects it would be enough to declare spotbugs in the root project 
    (quality plugin will detect its presence in build classpath and apply in modules)

!!! warning
    **Spotbugs plugin 6.x requires java 11** (same as spotbugs 4.9.x itself)
    Spotbugs support will be disabled on lower java version, but spotbugs plugin itself might
    break your project on a lower java version ([see java compatibility notes](../guide/java.md)).

    This is the reson why its not bundled as a tansitive dependency as before: 
    keeping it as a transitive dependency would mean java 8 compatibility loose,
    even for groovy projects (not requiring spotbugs at all).

By default, plugin is applied (if found in build classpath) if java sources available (`src/main/java`).    

SpotBugs configuration differ from other tools (checkstyle, pmd): instead of exact rules configuration
it uses [efforts level](http://spotbugs.readthedocs.io/en/latest/effort.html). Deeper level could reveal more bugs, but with higher mistake possibility. 
Default settings (`max` effort and `medium` level) are perfect for most cases. Some checks were disabled in the default 
[filter file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/exclude.xml)

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
    spotbugsVersion = '{{ gradle.spotbugs }}'
    spotbugs = true // false to disable automatic plugin activation
    spotbugsEffort = 'max'  // min, less, more or max
    spotbugsLevel = 'medium' // low, medium, high
    spotbugsMaxRank = 20 // 1-4 scariest, 5-9 scary, 10-14 troubling, 15-20 of concern  
    spotbugsMaxHeapSize = '1g'
    spotbugsQuiet = false
    spotbugsAnnotations = true // false to not register spotbugs-annotations
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

### Quiet mode

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

## Suppress

To suppress violations you can use [filter file](http://spotbugs.readthedocs.io/en/latest/filter.html).
In this case you need to override [default filter file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/spotbugs/exclude.xml).

Plugin applies `com.github.spotbugs:spotbugs-annotations:{{ gradle.spotbugs }}` dependency automatically
in `compileOnly` scope in order to use special suppressing annotation:

```java
@SuppressFBWarnings("URF_UNREAD_FIELD")
```

!!! abstract
    Spotbugs can't use default `@SuppressWarnings` annotation because it's a source annotation
    and not available in bytecode. 

!!! note
    To disable automatic annotations dependency applying:
    ```java
    quality.spotbugsAnnotations = false
    ```

Note that annotations dependency will be applied even if spotbugs support is disabled (`quality.spotbugs = false`)
to not break builds on lower java versions (where spotbugs plugin is disabled due to incompatibility)


## Excludes

Spotbugs is the only quality tool which works on classes rather than on sources. By default,
spotbugs task configured with all compiles classes which may include auto-generated sources too
(more than just a source set).

Generic [exclusions mechanism](../guide/exclusion.md) configures source exclusions and,
in order to properly apply these exclusions to spotbugs, quality plugin generates extended
exclusions (xml) file. So spotbugs should (seem to) work the same as other plugins.

!!! note
    Apt-generated sources excluded automatically (if you use gradle's `annotationProcessor` configuration).
    
!!! tip
    If you need to customize default exclusions file, just put custom file in [the configs
    directory](../task/config.md) and plugin will extend it with additional excludes if required.
    
    But do not set custom excludes file directly (with `spotbugs.excludeFilter`)!

### Manual exclusion

If, for some reason, exclusions, configured in quality extension not applied (for example, due to implementation bug), 
you can always put exclusions directly into exclusions filter file (tip above) or filter compiled classes:

```groovy
afterEvaluate {
    tasks.withType(com.github.spotbugs.snom.SpotBugsTask).configureEach {
        classes = classes.filter { 
            !it.path.contains('com/mycompany/serialize/protobuf/gen/') 
        }
    }
}
```

Pay attention that this trick filters compiled files (.class), not sources! Whatever custom 
filtering logic could be used.

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

### Available plugins

[Find Security Bugs](https://github.com/find-sec-bugs/find-sec-bugs) ([site](https://find-sec-bugs.github.io/))

```groovy   
quality {
    spotbugsPlugin 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0'
}
```

[fb-contrib: A FindBugs auxiliary detector plugin](https://github.com/mebigfatguy/fb-contrib) ([site](http://fb-contrib.sourceforge.net/))

```groovy         
qualtiy {
    spotbugsPlugin 'com.mebigfatguy.fb-contrib:fb-contrib:7.6.13'
}
```

## Annotations

Use spotbugs-annotations to guide spotbugs nullability checks (`#!java @NonNull` and `#!java @Nullable`).
Add `com.github.spotbugs:spotbugs-annotations:3.1.2` dependency (with provided scope if possible).

!!! warning
    Before,  annotations from Jsr-305 [were used](http://findbugs.sourceforge.net/manual/annotations.html) 
    (`com.google.code.findbugs:jsr305`), but now it is dead.
    Remove jsr-305 jar if it were used and use [undeprecated](https://github.com/spotbugs/spotbugs/issues/130)
    `#!java @edu.umd.cs.findbugs.annotations.NonNull` and `#!java @edu.umd.cs.findbugs.annotations.Nullable`

    Pay attention becuase libraries still bring-in jsr-305 jar (e.g. guava does): do not use
    `javax.annotation.Nullable` because it may lead to split package problem on java9 and above
    ([not always](https://github.com/google/guava/issues/2960#issuecomment-546713529))  

Another alternative is [chaker framework](https://checkerframework.org/) annotations:
`org.checkerframework:checker-qual:3.0.0`. Guava [already switched](https://github.com/google/guava/issues/2960) 
to use them, so if you use it you may already have these annotations.

Using checker framework annotations should be preferable because it's on the track to community acceptance as
default jsr-305 replacement. Besides, it's the only advanced java types system extension and validation tool. 

!!! hint
    Even if you will use other annotations, people using checker framework with your library
    would still benefit from your annotations because checker framework understands [almost all of them](https://checkerframework.org/manual/#nullness-related-work). 

Summary:

* If checker framework available (`org.checkerframework:checker-qual`) use it: 
    `org.checkerframework.checker.nullness.qual.Nullable`
* Otherwise, use spotbugs-annotations (`com.github.spotbugs:spotbugs-annotations`):
    `edu.umd.cs.findbugs.annotations.Nullable`  
* **Avoid** using jsr-305 directly (`com.google.code.findbugs:jsr305`): `javax.annotation.Nullable`     
    
### Example

Here is an example, which will force you to use nullability annotations.

When you use guava functions or predicates you may receive this:

```
[NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE] input must be nonnull but is marked as nullable 
```

The reason for this is that guava use `@Nullable` annotation, which is `@Inherited`, so
even if you not set annotation on your own function or predicate it will still be visible.

The simplest workaround is to set `@NonNull` annotation on your function or predicate:

```java
public boolean apply(@NonNull final Object input) {
```

!!! hint
    `NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION` check was disabled because it does not allow this workaround to work

### Spotbugs plugin issues

Spotbugs plugin always throws an exception when violations found, so even in non strict mode
(`quality.strict = false`) you will see an exception in logs when violations found (build will not be failed).
Not critical, just confusing.

Spotbugs plugin 6.x may not work correctly on windows java 11. If you see this error:

```
java.io.IOException: No files to analyze could be opened
	at edu.umd.cs.findbugs.FindBugs2.execute(FindBugs2.java:302)
	at edu.umd.cs.findbugs.FindBugs.runMain(FindBugs.java:390)
	at edu.umd.cs.findbugs.FindBugs2.main(FindBugs2.java:1223)
```

It is because of empty "onlyAnalyze" cli parameter (jdk arguments parsing bug). 
There is nothing you can do about it, except using spotbugs plugin 5.x (not affected by this problem).
Problem [reported](https://github.com/spotbugs/spotbugs-gradle-plugin/issues/1432), waiting for a fixed version.

## Problems resolution

Most problems appear with `spotbugs` configuration. Plugin by default configures only default dependencies for it,
so if you modify this configuration you will have to specify dependencies explicitly:

```groovy
afterEvaluate {
    dependencies {
        spotbugs "com.github.spotbugs:spotbugs:${quality.spotbugsVersion}"
    }
}
```

!!! important
    Gradle will not show you dependencies tree for spotbugs configuration (because it doesn't show default 
    dependencies) so to be able to see conflicts, configure it manually (as shown above).
    After that you can investigate with:
    ```
    gradlew dependencies --configuration spotbugs
    ```
    or (for exact dependency tracking)
    ```
    gradlew dependencyInsight --configuration spotbugs --dependency asm
    ```
   

### Asm

If you have problems executing spotbugs tasks like

```
Execution failed for task ':spotbugsMain'.
> Failed to run Gradle SpotBugs Worker
   > org/objectweb/asm/RecordComponentVisitor
```

(NoClassDefFoundException in stacktrace)

Then it is possible that you have incorrect asm:

```
gradlew dependencyInsight --configuration spotbugs --dependency org.ow2.asm:asm

org.ow2.asm:asm:7.2 (selected by rule)
...
org.ow2.asm:asm:7.3.1 -> 7.2
```  

This may be caused by incorrect BOM usage. For example, [spring dependency-management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin) 
configured like this:

```groovy
dependencyManagement {
    imports {
        mavenBom "com.google.inject:guice-bom:4.2.3"
    }        
}
```

would apply to ALL configurations, including "spotbugs". In this example, guice bom will force asm 7.2
which will lead to fail.

To fix this apply BOM only to some configurations:

```groovy
dependencyManagement {
    configurations(implementation, testImplementation, provided) {
        imports {
            mavenBom "com.google.inject:guice-bom:4.2.3"
        }        
    }
}
```

!!! warning
    But, in this case, generated pom will lack "dependencyManagement" section (as it use only globally applied BOMs),
    so if resulted pom is important for you, then simply force correct asm version for spotbugs:
    ```groovy
    afterEvaluate {
        dependencies {
            spotbugs "com.github.spotbugs:spotbugs:${quality.spotbugsVersion}"
            spotbugs "org.ow2.asm:asm:9.0"
        }  
    }  
    ```               

### Build dashboard plugin

If you use [build-dashboard](https://docs.gradle.org/current/userguide/build_dashboard_plugin.html) plugin,
you may [face an error](https://github.com/xvik/gradle-quality-plugin/issues/24):

```
Execution failed for task ':buildDashboard'.
> Could not create task ':spotbugsTest'.
   > Cannot change dependencies of dependency configuration ':spotbugs' after it has been resolved.
```

This is due to a bug in build-dashboard plugin, forcing initialization of all project tasks.
Spotbugs create lazy tasks for all source sets and each task configures defaults for `spotbugs` configuration.
So when build-dashboard force initialization of not used tasks, they can't apply configurations.

To workaround this simply initialize all not used spotbugs tasks manually:

```groovy
afterEvaluate {
    tasks.findByName('spotbugsTest')
}
```

`afterEvaluate` required because spotbugs plugin applied after configuration and `findByName` 
forces task initialization (for lazy tasks). 
    