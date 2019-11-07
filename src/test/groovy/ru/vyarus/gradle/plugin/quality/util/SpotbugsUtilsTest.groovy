package ru.vyarus.gradle.plugin.quality.util

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.AbstractTest

import static ru.vyarus.gradle.plugin.quality.util.SpotbugsUtils.mergeExcludes

/**
 * @author Vyacheslav Rusakov
 * @since 21.02.2018
 */
class SpotbugsUtilsTest extends AbstractTest {

    def "Check exclude xml modification"() {

        expect: "conversion"
        merge([file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/java')] ) ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
  <Match>
    <Bug pattern="NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION"/>
  </Match>
  <Match>
    <Bug pattern="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"/>
  </Match>
  <Match>
    <Class name="sample.Sample"/>
  </Match>
  <Match>
    <Class name="other.Sample2"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check no excludes"() {

        expect: "no modification"
        merge([], [file('/src/main/java')]) ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
  <Match>
    <Bug pattern="NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION"/>
  </Match>
  <Match>
    <Bug pattern="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check no matching roots"() {

        expect: "no changes"
        merge([file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/bad')]) ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
  <Match>
    <Bug pattern="NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION"/>
  </Match>
  <Match>
    <Bug pattern="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check custom rank"() {

        expect: "no changes"
        merge([], [], 15) ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
  <Match>
    <Bug pattern="NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION"/>
  </Match>
  <Match>
    <Bug pattern="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"/>
  </Match>
  <Match>
    <Rank value="15"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check excludes with rank together"() {

        expect: "conversion"
        merge([file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/java')],
                15) ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
  <Match>
    <Bug pattern="NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION"/>
  </Match>
  <Match>
    <Bug pattern="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"/>
  </Match>
  <Match>
    <Class name="sample.Sample"/>
  </Match>
  <Match>
    <Class name="other.Sample2"/>
  </Match>
  <Match>
    <Rank value="15"/>
  </Match>
</FindBugsFilter>
""" as String
    }


    def "Check plugin detection"() {

        when: "spotbugs plugin enabled"
        Project project = project {
            apply plugin: 'java'
            apply plugin: 'com.github.spotbugs'
        }

        then: "detect ok"
        SpotbugsUtils.isPluginEnabled(project)
    }

    private String merge(Collection<File> exclude, Collection<File> roots, Integer rank = null) {
        return mergeExcludes(new File(getClass().getResource('/ru/vyarus/quality/config/spotbugs/exclude.xml').toURI()),
                exclude,  roots, rank)
                .text.replace('\r', '')
                // on java 11 groovy inserts blank lines between tags
                .replaceAll('\n {1,}\n', '\n')
    }
}
