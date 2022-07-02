# Checkstyle

!!! summary ""
    Groovy | 
    [Home](https://codenarc.org/) | 
    [Release Notes](https://github.com/CodeNarc/CodeNarc/blob/master/CHANGELOG.md) |
    [Plugin](https://docs.gradle.org/current/userguide/codenarc_plugin.html)     
    
By default, plugin activates if groovy sources available (`src/main/groovy`).    

!!! warning
    Since codenarc 3.1 there is a separate jar for groovy4 (codenarc-groovy4) and
    plugin use it by default. If you need to use lower codenarc version set
    `quality.codenarcGroovy4 = false` in order to switch to usual codenarc jar (groovy 3 based).
    Note that it does not relate to your project's groovy version - codenarc will use its own
    groovy version.

## Output

```
24 (0 / 10 / 14) CodeNarc violations were found in 2 files

[Formatting | ClassJavadoc] sample.(GSample.groovy:3)  [priority 2]
	>> class GSample {
  Class sample.GSample missing Javadoc
  Makes sure each class and interface definition is preceded by javadoc. Enum definitions are not checked, due to strange behavior in the Groovy AST.
  http://codenarc.sourceforge.net/codenarc-rules-formatting.html#ClassJavadoc
  
...  
```

Counts in braces show priorities (p1/p2/p3).

## Config

Tool config options with defaults:

```groovy
quality {
    codenarcVersion = '{{ gradle.codenarc }}'
    codenarc = true // false to disable automatic plugin activation
    // use groovy4-based codenarc version; set to false to use groovy3-based version
    codenarcGroovy4 = true
}
```

## Suppress

To [suppress violation](https://codenarc.org/codenarc-configuring-rules.html#disabling-rules-using-suppresswarnings):

```java
@SuppressWarnings("ClassJavadoc")
```

Since [codenarc 2.2](https://github.com/CodeNarc/CodeNarc/pull/610) `CodeNarc.` prefix could be used to differentiate with pure java suppressions:

```java
@SuppressWarnings("CodeNarc.ClassJavadoc")
```

To suppress all violations use:

```java
@SuppressWarnings('CodeNarc')
```

Also, [comments may be used](https://codenarc.org/codenarc-configuring-rules.html#disabling-rules-from-comments) for disabling blocks of file.

