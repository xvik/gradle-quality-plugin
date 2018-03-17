# Animalsniffer

!!! summary ""
    Java, Groovy | [Home](http://www.mojohaus.org/animal-sniffer/) | [Plugin](https://github.com/xvik/gradle-animalsniffer-plugin)
    
In contrast to other tools, animalsniffer plugin is never activated automatically. Plugin will only 
apply common configuration (used source sets, strict mode) if [ru.vyarus.animalsniffer](https://github.com/xvik/gradle-animalsniffer-plugin) 
plugin manually applied.

## Config

Animalsniffer version could be defined through quality config:

```groovy
quality {
    animalsnifferVersion = '1.16'
}
```    

Default version is not declared and animalsniffer plugin driven version will be used by default.

Quality configuration is applied to animalsniffer configuration like this:

```groovy
animalsniffer {
    toolVersion = extension.animalsnifferVersion
    ignoreFailures = !extension.strict
    sourceSets = extension.sourceSets    
}
```