package ru.vyarus.gradle.plugin.quality.util

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 11.03.2018
 */
class DurationFormatTest extends Specification {

    def "Check duration format"() {

        expect: 'correct format'
        DurationFormatter.format(2000) == "2.000s"
        DurationFormatter.format(2*24*60*60*1000 + 80*60*1000 + 10*1000) == "2d1h20m10.00s"
    }
}
