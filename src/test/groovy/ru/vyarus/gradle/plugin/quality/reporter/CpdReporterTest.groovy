package ru.vyarus.gradle.plugin.quality.reporter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 08.11.2019
 */
class CpdReporterTest extends AbstractKitTest {

    def "Check cpd report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'de.aaschmid.cpd' version '3.3'
                id 'ru.vyarus.quality'
            }

            task testReport() {
                doLast {
                    new ru.vyarus.gradle.plugin.quality.report.CpdReporter(
                        new ru.vyarus.gradle.plugin.quality.ConfigLoader(project)
                    ).report(cpdCheck, 'cpdCheck')
                }
            }
        """)
        file('src/main/java').mkdirs()
        String report = getClass().getResourceAsStream('/ru/vyarus/gradle/plugin/quality/report/cpd/cpdCheck.xml').text
                .replaceAll('\\$\\{srcMainRoot}', file('src/main/java').canonicalPath.replaceAll('\\\\', '\\\\\\\\'))
                .replaceAll('\\$\\{srcTestRoot}', file('src/test/java').canonicalPath.replaceAll('\\\\', '\\\\\\\\'))
        File target = file('build/reports/cpd/cpdCheck.xml')
        target.parentFile.mkdirs()
        target << report

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        unifyString(error).contains """
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


sample.cpd.(Struct2.java:6)  [12 lines / 63 tokens]
sample.cpd.(Struct1.java:6)
  │
 6│    public class Struct2 {
 7│    
 8│        public static void main(String[] args) {
 9│            final Deque res = new ArrayDeque();
10│            System.out.println(res);
11│            System.out.println(res);
12│            System.out.println(res);
13│            System.out.println(res);
14│            System.out.println(res);
15│        }
16│    
17│        public void differentMethod2() {
""" as String
    }


    def "Check non source set file in report"() {

        setup: "prepare project"
        build("""
            plugins {
                id 'groovy'
                id 'de.aaschmid.cpd' version '3.3'
                id 'ru.vyarus.quality'
            }

            task testReport() {
                doLast {
                    new ru.vyarus.gradle.plugin.quality.report.CpdReporter(
                        new ru.vyarus.gradle.plugin.quality.ConfigLoader(project)
                    ).report(cpdCheck, 'cpdCheck')
                }
            }
        """)
        file('src/main/java').mkdirs()
        String report = getClass().getResourceAsStream('/ru/vyarus/gradle/plugin/quality/report/cpd/cpdCheck2.xml').text
                .replaceAll('\\$\\{srcMainRoot}', file('src/main/java').canonicalPath.replaceAll('\\\\', '\\\\\\\\'))
                .replaceAll('\\$\\{generatedRoot}', file('build/generated/java').canonicalPath.replaceAll('\\\\', '\\\\\\\\'))
        File target = file('build/reports/cpd/cpdCheck.xml')
        target.parentFile.mkdirs()
        target << report

        when: "call reporter"
        BuildResult result = run('testReport')
        def error = result.output

        then: "output valid"
        result.task(':testReport').outcome == TaskOutcome.SUCCESS
        unifyString(error).contains """
2 java duplicates were found by CPD

sample.cpd.(OtherStruct1.java:3)  [16 lines / 75 tokens]
build/generated/java/sample/cpd.(OtherStruct2.java:3)
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


build/generated/java/sample/cpd.(Struct2.java:6)  [12 lines / 63 tokens]
sample.cpd.(Struct1.java:6)
  │
 6│    public class Struct2 {
 7│    
 8│        public static void main(String[] args) {
 9│            final Deque res = new ArrayDeque();
10│            System.out.println(res);
11│            System.out.println(res);
12│            System.out.println(res);
13│            System.out.println(res);
14│            System.out.println(res);
15│        }
16│    
17│        public void differentMethod2() {
""" as String
    }
}
