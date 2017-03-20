package ru.vyarus.gradle.plugin.quality.util

import spock.lang.Specification

import static ru.vyarus.gradle.plugin.quality.util.GlobUtils.toRegex


/**
 * @author Vyacheslav Rusakov
 * @since 20.03.2017
 */
class GlobUtilsTest extends Specification {

    def "Check glob conversion"() {

        def sep = File.separator

        expect: "converting glob"
        toRegex('**/sample/*') == ".*${sep}sample${sep}.*" as String
        toRegex('**/*.java') == ".*${sep}.*\\.java\$" as String
        toRegex('**/Sample.java') == ".*${sep}Sample\\.java\$" as String
        toRegex('/opt/**') == "^${sep}opt${sep}.*" as String
    }
}