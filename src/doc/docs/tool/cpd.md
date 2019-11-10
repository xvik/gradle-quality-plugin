# CPD (Copy/Paste Detector)

!!! summary ""
    Java | 
    [Home](https://pmd.github.io/latest/pmd_userdocs_cpd.html) |     
    [Plugin](https://github.com/aaschmid/gradle-cpd-plugin)   
   
CPD support activates only when (de.aaschmid.cpd)[https://github.com/aaschmid/gradle-cpd-plugin] 
gradle plugin applied manually:

```groovy
plugins {        
    id 'de.aaschmid.cpd' version '3.0'
    id 'ru.vyarus.quality' version '4.0.0'
}
```    

CPD is a part of [PMD](pmd.md) project and so there is no need for additional version configuration (pmd version used).

!!! warning
    CPD plugin version 3.0 is [not compatible](https://github.com/aaschmid/gradle-cpd-plugin/issues/36) with gradle 6.
    3.1 will be.     

!!! note
    Special [xsl file](https://github.com/xvik/gradle-quality-plugin/blob/master/src/main/resources/ru/vyarus/quality/config/cpd/hcpdhtml.xslt) 
    used for manual html report generation because plugin does not support html report. This is [official pmd style](https://github.com/pmd/pmd/blob/master/pmd-core/etc/xslt/cpdhtml.xslt),
    but modified to show all duplicates (original script shows only duplicates > 30 lines).
    
## Output

```
2 java duplicates were found by CPD

sample.cpd.(OtherStruct1.java:3)  [16 lines / 75 tokens]
sample.cpd.(OtherStruct2.java:3)
  │
 3│    public class OtherStruct1 {
 4│    
 5│        public static void main(String[] args) {
 6│            Math.sqrt(12);
 7│            Math.sqrt(12);
 8│            Math.sqrt(12);
 9│            Math.sqrt(12);
10│            Math.sqrt(12);
11│            Math.sqrt(12);
12│            Math.sqrt(12);
13│            Math.sqrt(12);
14│            Math.sqrt(12);
15│            Math.sqrt(12);
16│        }
17│    
18│        public void differentMethod1() {
```

Shows all files with duplicate and source sample from the first file.

!!! note
    Order of files may vary on different environments and so you may see sources from different files
    in the resulted report. It doesn't matter much, just don't be surprised. 
    
!!! warning
    Tool does not search for exact matches, instead it tries to find very common blocks:
    for example, some code block copied and slightly modified - cpd will be able to match it.

## Config

Config options with defaults:

```groovy
quality {
    cpd = true // false to disable plugin configuration
    cpdUnifySources = true // false to not modify cpdCheck task sources
}
```

!!! note
    CPD itself must be configured with plugin's [`cpd` closure](https://github.com/aaschmid/gradle-cpd-plugin#options)

Configurations applied by plugin:

```groovy
cpd {
    toolVersion = quality.pmdVersion
    ignoreFailures = !extension.strict
}
```

If only groovy sources available, then `cpd.language` automatically set to `groovy`
(but only if cpd task declared in the same module with quality plugin!). 
    
### Cpd tasks

In contrast to other quality tools CPD plugin registers only one task `cpdCheck` instead of task per source set
(otherwise it would be impossible to find duplicates between source sets or even between different modules).

By default, `cpdCheck` task configured to check all available source sets. But quality plugin will
modify `chpdCheck.source`:

* All files from source sets, not configured for quality checks (`quality.sourceSets`) are removed.
This way cpd scope is unified with other quality plugins scope. 
* Global [excludes](../guide/exclusion.md) are also applied. 

Sources modifications could be disabled with `quality.cpdUnifySources = false` 

If multiple cpd tasks would be registered (other tasks declared manually), sources would be modified only
for default `cpdCheck` task. But console reporting and HTML report will be added for all tasks.
Also, all cpd tasks would be disabled with `quality.enabled = false`    

!!! note
    By default, CPD configured for java sources check. To change target language use:
    
    ```groovy
    cpd {
        language = 'groovy'
    }
    ```
    
### Multiple file types support

If you need to check more than one type of files (for example, java and groovy sources), you'll
need to configure separate tasks per each required type. For example:

```groovy
cpdCheck.exclude '*.groovy'                     
            
task groovyCpdCheck(type:de.aaschmid.gradle.plugins.cpd.Cpd) {
    language = 'groovy'
    source = sourceSets.main.allGroovy 
}
```

Here groovy sources excluded from default `cpdCheck` task (because cpd plugin by default covers all source sets).
Now the default task will search duplicates only in java files (and only within sources configured in quality plugin).

New task is declared for searching groovy duplicates (groovyCpdCheck).

Both tasks will be assigned to `check`, so by calling `gradlew check` you'll run both checks.
Condole report will be shown for both tasks (and HTML reports generated).

Note that task language will be also mentioned in console report:

```
:check
:cpdCheck
2 java duplicates were found by CPD
...

:groovyCpdCheck
2 groovy duplicates were found by CPD
...
```

!!! tip
    CPD understands [many source languages](https://pmd.github.io/latest/pmd_userdocs_cpd.html#supported-languages).
    But, if a specified language cannot be found, a fallback mechanism uses `net.sourceforge.pmd.cpd.AnyLanguage` instead. 
    This fallback language does not run ANTLR and therefore also checks duplicates in comments.
    
    This way you can use cpd for searching for duplicates even in pure text files, web templates or other resources.
    
### Multi-module projects

For multi-module projects, it is [recommended](https://github.com/aaschmid/gradle-cpd-plugin#multi-module-project) 
to apply cpd plugin in the root project. This way only one `cpdCheck` task is created (in the root project), which will
check sources from all modules (it's the only way to find duplicates between different modules).

But quality plugin, should be applied on module level (because all other quality tasks are source set scoped).

Expected root project configuration looks like this:

```groovy
plugins {
    id 'de.aaschmid.cpd' version '3.0'
    id 'ru.vyarus.quality' version '4.0.0' apply false
}
cpd {
    // cpd configuration, if required
}

// possibly other cpd tasks 

subprojects {
    apply plugin: 'java'
    apply plugin: 'ru.vyarus.quality'
    
    quality {
        // plugin configuration, if required    
    }   
}
``` 

This way each module have its own quality plugin, but quality configuration is the same in all modules.

Quality plugin will detect cpd plugin in the root project and configure it the same way as
in single module case. But as quality plugin will apply in each module, only first configured module
will apply `cpd` modifications of `toolVersion` and `ignoreFailures` (assuming this settings are the same for all modules).

Each module will exclude `cpdCheck` task sources, not covered by quality plugin configuration.
For example, if only main source set configured, each module will exclude test sources.
This way, each module control sources only related to its module and overall `cpdCheck` sources
configuration would be correct.

Each module will attach all root project's cpd tasks to its check (sub module's `check` depend on root project tasks).
Imagine if you work in one module you may call `check` only on module level. With common
quality tasks its clear - all configured module's source sets must be validated.
But you may introduce duplicates (e.g. copied method from other module) and the only
way to detect it is to call root project tasks (so its completely logical to assign root cpd check
to module checks).  

As an example, suppose we have 2 modules `mod1` and `mod2` with root project configuration as described above:

* root project's cpd plugin will be configured
* `mod1` will exclude `mod1/src/test/java` from `crpCheck.sources`
* `:mod1:check` depends on `:cpdCheck`
* `mod2` will exclude `mod2/src/test/java` from `crpCheck.sources`
* `:mod2:check` depends on `:cpdCheck` 

You will also notice, that in the multi-module setup console output will identify class module:

```
mod1/sample.cpd.(OtherStruct1.java:3)  [16 lines / 75 tokens]
mod2/sample.cpd.(OtherStruct2.java:3)
  │
 3│    public class OtherStruct1 {
 4│    
 5│        public static void main(String[] args) {
 6│            Math.sqrt(12);
```    

!!! tip
    Quality plugin actually search CPD plugin not in the root-most project but in all
    parent chain. So if you have complex gradle project and cpd plugin declared somewhere
    in the middle of projects hierarchy, everything will still be properly configured.  

## Suppress

CPD violations could be [suppressed](https://pmd.github.io/latest/pmd_userdocs_cpd.html#suppression)
only for some languages (ava, C/C++, Dart, Go, Javascript, Kotlin, Lua, Matlab, Objective-C, PL/SQL, Python and Swift)
with comments containing CPD-OFF and CPD-ON:

```java
public Object someParameterizedFactoryMethod(int x) throws Exception {
    // some unignored code

    // tell cpd to start ignoring code - CPD-OFF

    // mission critical code, manually loop unroll
    goDoSomethingAwesome(x + x / 2);
    goDoSomethingAwesome(x + x / 2);
    goDoSomethingAwesome(x + x / 2);
    goDoSomethingAwesome(x + x / 2);
    goDoSomethingAwesome(x + x / 2);
    goDoSomethingAwesome(x + x / 2);

    // resume CPD analysis - CPD-ON

    // further code will *not* be ignored
}
```

**Java** sources could be also suppressed with annotations:

```java
//enable suppression
@SuppressWarnings("CPD-START")
public Object someParameterizedFactoryMethod(int x) throws Exception {
    // any code here will be ignored for the duplication detection
}

//disable suppression
@SuppressWarnings("CPD-END)
public void nextMethod() {
}
```