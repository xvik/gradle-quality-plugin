# CPD sample

When [CPD](https://github.com/aaschmid/gradle-cpd-plugin) plugin is applied, it is configured the same way
as other quality plugins.

Note that exclusion (`quality.exclude`) affects CPD tasks 

## Output

```
> Task :copyQualityConfigs
> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE

> Task :cpdCheck
CPD found duplicate code. See the report at file:///home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/cpd/build/reports/cpd/cpdCheck.xml

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


sample.cpd.(OtherStruct1.java:6)  [9 lines / 54 tokens]
sample.cpd.(OtherStruct2.java:7)
  │
 6│            Math.sqrt(12);
 7│            Math.sqrt(12);
 8│            Math.sqrt(12);
 9│            Math.sqrt(12);
10│            Math.sqrt(12);
11│            Math.sqrt(12);
12│            Math.sqrt(12);
13│            Math.sqrt(12);
14│            Math.sqrt(12);


CPD HTML report: file:///home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/cpd/build/reports/cpd/cpdCheck.html

> Task :check
```

Violations were found only in `Other*` files because `Struct2` was excluded.