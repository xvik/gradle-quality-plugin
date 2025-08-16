package ru.vyarus.gradle.plugin.quality.tool

import groovy.transform.CompileStatic

/**
 * Project language (detected by analyzing project source files).
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 */
@CompileStatic
@SuppressWarnings('FieldName')
enum ProjectLang {
    Java, Groovy
}
