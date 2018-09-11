# Checkstyle

!!! summary ""
    Java | 
    [Home](http://checkstyle.sourceforge.net) | 
    [Release Notes](http://checkstyle.sourceforge.net/releasenotes.html) |
    [Plugin](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)     
    
By default, plugin is activated if java sources available (`src/main/java`).    

[Default config](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/checkstyle/checkstyle.xml)
contains all possible checks, but some of them are disabled (note that some checkstyle rules are opposite and 
never intended to be used together). Uncomment check to enable it.

## Output

```
8 Checkstyle rule violations were found in 2 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:0)
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile
  
...
```

## Config

Tool config options with defaults:

```groovy
quality {
    checkstyleVersion = '8.12'
    checkstyle = true // false to disable automatic plugin activation
}
```

## Suppress

To [suppress violation](http://checkstyle.sourceforge.net/config_filters.html#SuppressWarningsFilter):

```java
@SuppressWarnings("NewlineAtEndOfFile")
```

Or [with prefix](http://checkstyle.sourceforge.net/config_annotation.html#SuppressWarningsHolder) (but require lower cased name):

```java
@SuppressWarnings("checkstyle:newlineatendoffile")
```

To suppress all violations:

```java
@SuppressWarnings("all")
```

Or using [comments](http://checkstyle.sourceforge.net/config_filters.html#SuppressionCommentFilter):

```java
// CHECKSTYLE:OFF
..anything..
// CHECKSTYLE:ON
```