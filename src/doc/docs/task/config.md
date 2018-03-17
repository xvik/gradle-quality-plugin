# Copy configs task

!!! summary ""
    initQualityConfig task
    

Plugin contains predefined configurations for all plugins.
During execution default files are copied into `$buildDir/quality-configs` (if no custom user configs provided).
  
If you want to customize default configs then use the following task to copy everything into project:

```bash
$ gradlew initQualityConfig
```  

It will copy all configs into configured (`quality.configDir`) folder (will not override existing configs).

```
gradle\
    config\
        checkstyle\
            checkstyle.xml		
        codenarc\
            codenarc.xml		
        findbugs\
            exclude.xml			
            html-report-style.xsl	
        pmd\
            pmd.xml			
```

Task copies all configs, but you may remove all files you don't want to customize (plugin will use default versions for them).
File names are important: if you rename files plugin will not find them and use defaults.

Configuration files contain all possible rules. Not used rules are commented (or excluded).
