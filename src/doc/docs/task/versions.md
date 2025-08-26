# Versions tasks

!!! summary ""
    qualityToolVersions task

Task prints configured tool versions for active tools.

Example output:

```
> Task :qualityToolVersions
Java version: 17
Gradle version: 8.14.3
Checkstyle: 11.0.0
PMD: 7.16.0
CodeNarc: 3.6.0
```

If tool is assumed to be active (according to sources), but was disabled,
it would be indicated:

```
> Task :qualityToolVersions
Java version: 17
Gradle version: 8.14.3
Checkstyle: disabled
PMD: disabled
SpotBugs: disabled
CodeNarc: disabled
```