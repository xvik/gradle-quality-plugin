package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.api.JavaVersion
import ru.vyarus.gradle.plugin.quality.AbstractTest
import ru.vyarus.gradle.plugin.quality.tool.spotbugs.SpotbugsUtils

import java.nio.file.Files

import static ru.vyarus.gradle.plugin.quality.tool.spotbugs.SpotbugsUtils.mergeExcludes

/**
 * @author Vyacheslav Rusakov
 * @since 21.02.2018
 */
class SpotbugsUtilsTest extends AbstractTest {

    def "Check spotbugs compatibility"() {

        expect:
        SpotbugsUtils.isSpotbugsCompatible(ver, java) == res

        where:
        ver     | java                    | res
        '4.9.4' | JavaVersion.VERSION_22 | true
        '4.9.4' | JavaVersion.VERSION_21  | true
        '4.9.4' | JavaVersion.VERSION_17  | true
        '4.9.4' | JavaVersion.VERSION_11  | true
        '4.9.4' | JavaVersion.VERSION_1_8 | false

        '4.8.3' | JavaVersion.VERSION_22  | true
        '4.8.3' | JavaVersion.VERSION_21  | true
        '4.8.3' | JavaVersion.VERSION_17  | true
        '4.8.3' | JavaVersion.VERSION_11  | true
        '4.8.3' | JavaVersion.VERSION_1_8 | true

    }

    def "Check fallback spotbugs version"() {
        expect:
        SpotbugsUtils.getCompatibleSpotbugsVersion(true, ver, java) == res

        where:
        ver     | java                    | res
        '4.9.4' | JavaVersion.VERSION_22  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_21  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_17  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_11  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_1_8 | '4.8.6'
    }

    def "Check exclude xml modification"() {

        expect: "conversion"
        merge([file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/java')] ) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
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
    <Or>
      <Bug pattern="EI_EXPOSE_REP"/>
      <Bug pattern="EI_EXPOSE_REP2"/>
    </Or>
  </Match>
  <Match>
    <Class name="~sample\\.Sample(\\\$.*)?"/>
  </Match>
  <Match>
    <Class name="~other\\.Sample2(\\\$.*)?"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check no excludes"() {

        expect: "no modification"
        merge([], [file('/src/main/java')]) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
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
    <Or>
      <Bug pattern="EI_EXPOSE_REP"/>
      <Bug pattern="EI_EXPOSE_REP2"/>
    </Or>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check no matching roots"() {

        expect: "no changes"
        merge([file('src/main/java/sample/Sample.java'), file('src/main/java/other/Sample2.java')],
                [file('/src/main/bad')]) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
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
    <Or>
      <Bug pattern="EI_EXPOSE_REP"/>
      <Bug pattern="EI_EXPOSE_REP2"/>
    </Or>
  </Match>
</FindBugsFilter>
""" as String
    }

    def "Check custom rank"() {

        expect: "no changes"
        merge([], [], 15) ==
                """<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
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
    <Or>
      <Bug pattern="EI_EXPOSE_REP"/>
      <Bug pattern="EI_EXPOSE_REP2"/>
    </Or>
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
                """<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
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
    <Or>
      <Bug pattern="EI_EXPOSE_REP"/>
      <Bug pattern="EI_EXPOSE_REP2"/>
    </Or>
  </Match>
  <Match>
    <Class name="~sample\\.Sample(\\\$.*)?"/>
  </Match>
  <Match>
    <Class name="~other\\.Sample2(\\\$.*)?"/>
  </Match>
  <Match>
    <Rank value="15"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    private String merge(Collection<File> exclude, Collection<File> roots, Integer rank = null) {
        File tmp = Files.createTempFile("test", "spotbugs").toFile()
        tmp.text = new File(getClass().getResource('/ru/vyarus/quality/config/spotbugs/exclude.xml').toURI()).text
        mergeExcludes(tmp,
                exclude,  roots, rank)

        def res = tmp.text.replace('\r', '')
                // on java 11 groovy inserts blank lines between tags
                .replaceAll('\n {1,}\n', '\n')
        tmp.delete()
        return res
    }
}
