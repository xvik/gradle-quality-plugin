# Checkstyle

!!! summary ""
    Groovy | 
    [Home](http://codenarc.sourceforge.net) | 
    [Release Notes](https://github.com/CodeNarc/CodeNarc/blob/master/CHANGELOG.md) |
    [Plugin](https://docs.gradle.org/current/userguide/codenarc_plugin.html)     
    
By default, plugin is activated if groovy sources available (`src/main/groovy`).    


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
    codenarcVersion = '1.1'
    codenarc = true // false to disable automatic plugin activation
}
```

## Suppress

To [suppress violation](http://codenarc.sourceforge.net/codenarc-configuring-rules.html#Suppressing_A_Rule_From_Within_Source_Code):

```java
@SuppressWarnings("ClassJavadoc")
```