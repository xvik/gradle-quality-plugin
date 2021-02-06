# Checkstyle

!!! summary ""
    Java | 
    [Home](http://checkstyle.sourceforge.net) | 
    [Release Notes](http://checkstyle.sourceforge.net/releasenotes.html) |
    [Plugin](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)     
    
By default, plugin activates if java sources available (`src/main/java`).    

[Default config](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/checkstyle/checkstyle.xml)
contains all possible checks, but some of them are disabled (note that some checkstyle rules are opposite and 
never intended to be used together). Uncomment check to enable it.

!!! note ""
    In case you will use a custom config: checkstyle config must describe all used rules. So when new version release with new rules,
    config must be manually updated to add new rules (otherwise they would not be used).

## Output

```
8 Checkstyle rule violations were found in 2 files

[Misc | NewlineAtEndOfFile] sample.(Sample.java:1)
  File does not end with a newline.
  http://checkstyle.sourceforge.net/config_misc.html#NewlineAtEndOfFile
  
...
```

## Config

Tool config options with defaults:

```groovy
quality {
    checkstyleVersion = '8.39'
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

### Suppressions file

You can also use external [suppressions file](https://checkstyle.sourceforge.io/config_filters.html#SuppressionFilter).
It may be a good option for disabling entire rules without overriding the default config file.

Suppressions config could:
1. disable some checks in all files
2. disable checks only in some files (or file pattern)
3. disable checks in exact file and under exact lines
4. disable checks by error message

The default config already configures optional suppressions file usage, so to use it just drop in
suppressions file into checkstyle configurations dir: `gradle/config/checkstyle/suppressions.xml`

Alternatively, you can use [configs init task](../task/config.md) which will bring the default (empty) suppressions.xml:

```xml
<?xml version="1.0"?>
<!DOCTYPE suppressions PUBLIC
        "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
        "https://checkstyle.org/dtds/suppressions_1_2.dtd">

<!-- Empty suppressions file: copy it into gradle/config/checkstyle and specify required suppressions
     (https://checkstyle.sourceforge.io/config_filters.html#SuppressionFilter)-->
<suppressions>

    <!--<suppress checks="NewlineAtEndOfFileCheck" files="Sample.java"/>-->

    <!--<suppress message="Missing a Javadoc comment."/>-->

</suppressions>
```

!!! warning
    Pay attention that check names in the file are all have postfix 'Check', 
    whereas violations in the console omit this postfix.

There are many configuration examples in [the checkstyle documentation](https://checkstyle.sourceforge.io/config_filters.html#SuppressionFilter_Examples)
