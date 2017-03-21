package ru.vyarus.gradle.plugin.quality.util

import spock.lang.Specification

import static ru.vyarus.gradle.plugin.quality.util.FileUtils.extractJavaClass

/**
 * @author Vyacheslav Rusakov
 * @since 21.03.2017
 */
class FileUtilsTest extends Specification {

    def "Check java class resolution"() {

        expect: 'correct'
        extractJavaClass([new File('src/')], new File('src/some/Sample.java')) == 'some.Sample'
        extractJavaClass([new File('fooo/')], new File('src/some/Sample.java')) == null
        extractJavaClass([new File('fooo/'), new File('src/')], new File('src/some/Sample.java')) == 'some.Sample'

    }
}
