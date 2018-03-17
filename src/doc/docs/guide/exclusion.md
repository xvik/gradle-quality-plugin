# Exclusions

## Exclude files from check 

All quality tasks are based on [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
which allows excluding sources using ant patterns.

To apply exclusion to all plugins at once use:

```groovy
quality {
    exclude '**/sample/**'
}
```

!!! note
    This will not affect animalsniffer plugin, because it checks different thing (binary compatibility) and 
    use it's own configuration [to configure exclusions](https://github.com/xvik/gradle-animalsniffer-plugin#extend-signature).

!!! info 
    Findbugs task does not support exclusions on task level, so plugin manually resolve all excluded
    classes and add them to findbugs excludes filter xml file (default or custom user file).
    As a result, exclusion works the same way for all plugins.

!!! note 
    Exclusion patterns are resolved on relative paths (relative to source dir),
    so absolute file path matching will not work. Your pattern must match just "package" and file name parts.
    If you need to exclude on full paths then use [direct source exclusion](#direct-source-exclusions).

### Alternative configurations

All of the following configurations are allowed:

```groovy
quality {
    exclude '**/Sample.java', 'com/foo/**'
}
```

```groovy
quality {
    exclude '**/Sample.java' 
    exclude 'com/foo/**'
}
```

```groovy
quality {
    exclude = ['**/Sample.java', 'com/foo/**']
}
```

## Direct source exclusions

When you need to exclude sources from check, you should consider:

* Extract such sources into it's own source set and exclude this set from check. (generated classes case)
* Use pattern excludes (see above) to exclude sources based on package and (or) file name

If non of the above works for you, then you did sources configuration not according to best practices. 
Anyway, there is last resort option for such cases (when it could not be done the right way).

Suppose we have generated sources, added to main source set:

```groovy
sourceSets.main {
    java {
        srcDir 'build/generated/java'
    }
}
```

Here we have two source dirs for java sources: `src/main/java` and `build/generated/java`.

We want to exclude them from quality check, so we try:

!!! fail
    ```groovy
    quality {
        exclude '**/generated/**'
    }
    ```

which WILL NOT WORK because gradle applies patterns relatively to `build/generated/java` directory
and so our patter will never match.

Instead, specify ignored files directly, using rich 
[gradle files api](https://docs.gradle.org/current/userguide/working_with_files.html):

!!! success
    ```groovy
    quality {
        excludeSources = fileTree('build/generated')
    }
    ```

This will exclude all files in 'generated' directory from quality tools checks.

!!! note
    As with patterns exclude, this will not affect animalsniffer. For findbugs, plugin will
    add excluded classes to exclude filter.

Another example, just to show how flexible it could be configured:

```groovy
quality {
    excludeSources = fileTree('build/generated').matching {
        include '**/sample/**/*.java'
    }
}
```

Exclude all java sources in sample package (in generated directory).
Include pattern here will work relatively to `build/generated` directory.

You can use even single files:

```groovy
quality {
    excludeSources = files('build/generated/java/com/mypkg/Bad.java')
}
```

Exclude options could be used together (exclude files and patterns).
