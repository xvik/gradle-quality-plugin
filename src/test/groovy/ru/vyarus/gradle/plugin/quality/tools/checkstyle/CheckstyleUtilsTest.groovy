package ru.vyarus.gradle.plugin.quality.tools.checkstyle

import org.gradle.api.JavaVersion
import ru.vyarus.gradle.plugin.quality.tool.checkstyle.CheckstyleUtils
import spock.lang.Specification

import java.nio.file.Files

/**
 * @author Vyacheslav Rusakov
 * @since 14.03.2026
 */
class CheckstyleUtilsTest extends Specification {

    def "Check checkstyle compatibility"() {

        expect:
        CheckstyleUtils.isCheckstyleCompatible(ver, java) == res

        where:
        ver       | java                    | res
        '13.3.0'  | JavaVersion.VERSION_22 | true
        '13.3.0'  | JavaVersion.VERSION_21  | true
        '13.3.0'  | JavaVersion.VERSION_20  | false
        '13.3.0'  | JavaVersion.VERSION_17  | false
        '13.3.0'  | JavaVersion.VERSION_11  | false
        '13.3.0'  | JavaVersion.VERSION_1_8 | false

        '12.3.1'  | JavaVersion.VERSION_22  | true
        '12.3.1'  | JavaVersion.VERSION_21  | true
        '12.3.1'  | JavaVersion.VERSION_20  | true
        '12.3.1'  | JavaVersion.VERSION_17  | true
        '12.3.1'  | JavaVersion.VERSION_11  | false
        '12.3.1'  | JavaVersion.VERSION_1_8 | false

        '11.1.0'  | JavaVersion.VERSION_22  | true
        '11.1.0'  | JavaVersion.VERSION_21  | true
        '11.1.0'  | JavaVersion.VERSION_20  | true
        '11.1.0'  | JavaVersion.VERSION_17  | true
        '11.1.0'  | JavaVersion.VERSION_11  | false
        '11.1.0'  | JavaVersion.VERSION_1_8 | false

        '10.26.1' | JavaVersion.VERSION_22  | true
        '10.26.1' | JavaVersion.VERSION_21  | true
        '10.26.1' | JavaVersion.VERSION_20  | true
        '10.26.1' | JavaVersion.VERSION_17  | true
        '10.26.1' | JavaVersion.VERSION_11  | true
        '10.26.1' | JavaVersion.VERSION_1_8 | false
    }

    def "Check fallback checkstyle version"() {
        expect:
        CheckstyleUtils.getCompatibleCheckstyleVersion(true, ver, java) == res

        where:
        ver       | java                    | res
        '13.3.0'  | JavaVersion.VERSION_22  | '13.3.0'
        '13.3.0'  | JavaVersion.VERSION_21  | '13.3.0'
        '13.3.0'  | JavaVersion.VERSION_20  | '12.3.1'
        '13.3.0'  | JavaVersion.VERSION_17  | '12.3.1'
        '13.3.0'  | JavaVersion.VERSION_11  | '10.26.1'
        '13.3.0'  | JavaVersion.VERSION_1_8 | '10.26.1'

        '12.3.0'  | JavaVersion.VERSION_22  | '12.3.0'
        '12.3.0'  | JavaVersion.VERSION_21  | '12.3.0'
        '12.3.0'  | JavaVersion.VERSION_20  | '12.3.0'
        '12.3.0'  | JavaVersion.VERSION_17  | '12.3.0'
        '12.3.0'  | JavaVersion.VERSION_11  | '10.26.1'
        '12.3.0'  | JavaVersion.VERSION_1_8 | '10.26.1'

        '11.1.0'  | JavaVersion.VERSION_22  | '11.1.0'
        '11.1.0'  | JavaVersion.VERSION_21  | '11.1.0'
        '11.1.0'  | JavaVersion.VERSION_20  | '11.1.0'
        '11.1.0'  | JavaVersion.VERSION_17  | '11.1.0'
        '11.1.0'  | JavaVersion.VERSION_11  | '10.26.1'
        '11.1.0'  | JavaVersion.VERSION_1_8 | '10.26.1'

        '10.26.0' | JavaVersion.VERSION_22  | '10.26.0'
        '10.26.0' | JavaVersion.VERSION_21  | '10.26.0'
        '10.26.0' | JavaVersion.VERSION_20  | '10.26.0'
        '10.26.0' | JavaVersion.VERSION_17  | '10.26.0'
        '10.26.0' | JavaVersion.VERSION_11  | '10.26.0'
        '10.26.0' | JavaVersion.VERSION_1_8 | '10.26.1' // checkstyle not compatible with java 8, but method returns the lowest version
    }


    def "Check excludes"() {

        expect: "conversion"
        merge(['SuppressionCommentFilter', 'MissingOverride', 'SuppressWarnings', 'SuppressWarnings2']) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE module PUBLIC
        "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
  <module name="TreeWalker">
    <module name="AnnotationUseStyle"/>
    <module name="MissingDeprecated"/>
    <module name="PackageAnnotation"/>
    <module name="SuppressWarningsHolder"/>
  </module>
</module>
""" as String
    }

    private String merge(List<String> excludes) {
        File tmp = Files.createTempFile("test", "checkstyle").toFile()
        tmp.text = new File(getClass().getResource('/ru/vyarus/gradle/plugin/quality/config/checkstyle/checkstyle.xml').toURI()).text
        CheckstyleUtils.mergeExcludes(tmp, excludes)

        def res = tmp.text.replace('\r', '')
        // on java 11 groovy inserts blank lines between tags
                .replaceAll('\n {1,}\n', '\n')
        tmp.delete()
        return res
    }
}
