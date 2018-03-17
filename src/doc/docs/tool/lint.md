# Javac lint

!!! summary ""
    Java | [Home](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html)
    
Javac linter could show more useful warnings (not visible by default).  
See the [list of all options](http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html#BHCJCABJ).

## Configuration

By default, plugin will enable deprecation and unchecked warnings (as the most useful):

```groovy
quality {
    lintOptions = ['deprecation', 'unchecked']
}
``` 

These lint options are applied to all registered `CompileJava` tasks.