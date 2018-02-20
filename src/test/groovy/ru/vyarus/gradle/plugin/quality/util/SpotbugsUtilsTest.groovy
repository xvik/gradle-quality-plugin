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
        mergeExcludes(exclude(),
                [file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/java')]
        ).text.replaceAll('\r', '') ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
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
        mergeExcludes(exclude(),
                [],
                [file('/src/main/java')]
        ).text.replaceAll('\r', '') ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check no matching roots"() {

        expect: "no changes"
        mergeExcludes(exclude(),
                [file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/bad')]
        ).text.replaceAll('\r', '') ==
                """<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
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

    private File exclude() {
        return new File(getClass().getResource('/ru/vyarus/quality/config/spotbugs/exclude.xml').toURI())
    }
}
