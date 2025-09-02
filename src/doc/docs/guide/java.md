# Java version

!!! note
    Spotbugs tool requires java 11 and checkstyle requires java 17.

This is only actual for java projects (groovy projects use only CodeNarc).

Plugin will automatically disable tools on incompatible java versions. 
But spotbugs case is tricky (see below).

!!! important
    It is completely normal to run quality tools on higher java version only (e.g. develop with jdk 17):
    no need to run it on each supported java on CI.

## Java 11 support

Checkstyle 11 (configured by default) will be disabled on java 11 automatically.

If you **only use java 11** then checkstyle could be enabled by downgrading its version:

```groovy
quality.checkstyleVersion = '10.26.1'
```

(plugin enables checkstyle based on a configured version, so there is no need to manually specify `quality.checkstyle=true`)

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
there is no way to use spotbugs plugin 6.x only on java 11 and 17 (not possible to apply plugin conditionally in gradle).

!!! note
    Spotbugs plugin 6.x is a problem for java 8 because its gradle metadata set java_version=11 attribute.
    If your project configured for java 8 source compatibility (`sourceCompatibility= 1.8`), then gradle will
    gradle would **fail to start** because there is compatible plugin version.

You can only downgrade spotbugs plugin to 5.x (compatible with java 8):

```groovy
plugins {
    id 'com.github.spotbugs' version '5.2.5' apply false
}
```

In this case gradle would be able to resolve the build classpath on java 8, but the quality plugin would not 
enable spotbugs on java 8 (because recent spotbugs itself require java 11).

"apply false" is important because otherwise gradle would apply spotbugs plugin 6.x on java 8.

!!! warning
    There is a side effect: as quality plugin is not configuring spotbugs plugin, it will not
    apply annotations dependency. So, if you use spotbugs annotations, you will need to add them manually:

    ```groovy
    compileOnly 'com.github.spotbugs:spotbugs-annotations:4.8.6'
    ```

If it is required to use spotbugs plugin 6.x on java >= 11, then you'll have to apply it like this
(the only way for conditional application):

```groovy
buildscript {
    repositories { mavenCentral() }
    dependencies {
        if (JavaVersion.current() >= JavaVersion.VERSION_11) {
            classpath 'com.github.spotbugs.snom:spotbugs-gradle-plugin:{{ gradle.spotbugsPlugin }}'
        }
    }
}
```

!!! note "Alternative (not recommended)"
    You can cheat gradle to ignore java version checks for resolved plugin dependencies:

    ```java
    import org.gradle.api.attributes.java.TargetJvmVersion
    
    buildscript {
        configurations.classpath.attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
    }
    
    plugins {
        id 'java'
        id 'ru.vyarus.quality' version '6.0.0'
        id 'com.github.spotbugs' version '{{ gradle.spotbugsPlugin }}' apply false
    }
    ```
    This way, gradle would apply spotbugs plugin 6.x on java 8, but the quality plugin will not
    activate it, and so everything would work fine.


Spotbugs 4.9.x (used by default) is not compatible with java 8.

If you run **on multiple java versions**, then do nothing: plugin would not
enable spotbugs on java 8 (and spotbugs would be used on java 11 and above).

If you run **only on java 8**, then downgrade spotbugs to 4.8.x:

```groovy
quality.spotbugsVersion = '4.8.6'
```

## Auto tools downgrade

There is a special option to automatically downgrade tools to the lowest supported version:

```groovy
quality.fallbackToCompatibleToolVersion = true
```

When enabled, it would change checkstyle to 10.x on java 11 and spotbugs to 4.8.x on java 8.

!!! note
    This option is was added for plugin testing and **not recommended** because it may cause unexpected 
    behavior (especially on CI): different tool versions may (and almost certainly would) produce different results.