package ru.vyarus.gradle.plugin.quality.util

import ru.vyarus.gradle.plugin.quality.AbstractTest
import spock.lang.IgnoreIf

import static ru.vyarus.gradle.plugin.quality.util.FindbugsUtils.mergeExcludes

/**
 * @author Vyacheslav Rusakov
 * @since 20.03.2017
 */
@IgnoreIf({jvm.java9Compatible})
class FindbugsUtilsTest extends AbstractTest {

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

    private File exclude() {
        return new File(getClass().getResource('/ru/vyarus/quality/config/findbugs/exclude.xml').toURI())
    }
}