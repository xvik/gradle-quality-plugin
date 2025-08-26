# Gradle compatibility

Plugin compiled for java 8, compatible with java 11 and above

| Gradle | Version                                                      |
|--------|--------------------------------------------------------------|
| 7.1-9  | 6.0.0                                                        |
| 7.0    | [5.0.0](https://xvik.github.io/gradle-quality-plugin/5.0.0/) |
| 5.6-6  | [4.9.0](https://xvik.github.io/gradle-quality-plugin/4.9.0/) |
| 5.1    | [4.2.2](http://xvik.github.io/gradle-quality-plugin/4.2.2)   |
| 4.1    | [3.4.0](http://xvik.github.io/gradle-quality-plugin/3.4.0)   |
| older  | [2.4.0](http://xvik.github.io/gradle-quality-plugin/2.4.0)   |

Java requirements for quality tools:

| Tool       | Default version | Java version |
|------------|-----------------|--------------|
| Checkstyle | 11.0.0          | 17           |
| PMD        | 7.16.0          | 8            |
| SpotBugs   | 4.9.4           | 11           |
| CodeNarc   | 3.6.0           | 8            |

Incompatible tools will not be enabled: for example, on java 11 Checkstyle will not be enabled.