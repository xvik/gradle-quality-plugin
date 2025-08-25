# Animalsniffer sample

When [animalsniffer](https://github.com/xvik/gradle-animalsniffer-plugin) plugin is enabled,
it is configured the same as other quality plugins.

Also, animalsniffer tasks attached to checkQualityMain (and other source-set scoped check tasks)

Note: exclusions are not applied to animalsniffer plugin (because it makes no sense)

## Output

```
> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes

> Task :animalsnifferMain

4 AnimalSniffer violations were found in 2 files. See the report at: file:////home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/animalsniffer/build/reports/animalsniffer/main.text

[Undefined reference] sample.(Sample.java:15)
  >> java.util.ArrayDeque

[Undefined reference] sample.(Sample.java:15)
  >> void java.util.ArrayDeque.<init>()

[Undefined reference] sample.(Sample2.java:15)
  >> java.util.ArrayDeque

[Undefined reference] sample.(Sample2.java:15)
  >> void java.util.ArrayDeque.<init>()


> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :check
```