package ru.vyarus.gradle.plugin.quality.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import static ru.vyarus.gradle.plugin.quality.util.FindbugsUtils.mergeExcludes


/**
 * @author Vyacheslav Rusakov
 * @since 20.03.2017
 */
class FindbugsUtilsTest extends Specification {

    Logger logger = LoggerFactory.getLogger(FindbugsUtilsTest.class)

    def "Check exclude xml modification"() {

        def sep = File.separator

        expect: "conversion"
        mergeExcludes(['**/sample/*', '**/*.java'], exclude(), logger).text.replaceAll('\r', '') ==
"""<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter>
  <Match>
    <Source name="~.*\\.groovy"/>
  </Match>
  <Match>
    <Source name="~.*${sep}sample${sep}.*"/>
  </Match>
  <Match>
    <Source name="~.*${sep}.*\\.java\$"/>
  </Match>
</FindBugsFilter>
""" as String
    }

    private File exclude() {
        return new File(getClass().getResource('/ru/vyarus/quality/config/findbugs/exclude.xml').toURI())
    }
}