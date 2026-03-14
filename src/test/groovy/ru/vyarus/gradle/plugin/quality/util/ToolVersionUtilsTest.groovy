package ru.vyarus.gradle.plugin.quality.util

import org.gradle.api.JavaVersion
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.03.2026
 */
class ToolVersionUtilsTest extends Specification {

    def "Check checkstyle compatibility"() {

        expect:
        ToolVersionUtils.isCheckstyleCompatible(ver, java) == res

        where:
        ver       | java                    | res
        '13.3.0'  | JavaVersion.VERSION_22  | true
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
        ToolVersionUtils.getCompatibleCheckstyleVersion(true, ver, java) == res

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

    def "Check spotbugs compatibility"() {

        expect:
        ToolVersionUtils.isSpotbugsCompatible(ver, java) == res

        where:
        ver     | java                    | res
        '4.9.4' | JavaVersion.VERSION_22  | true
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
        ToolVersionUtils.getCompatibleSpotbugsVersion(true, ver, java) == res

        where:
        ver     | java                    | res
        '4.9.4' | JavaVersion.VERSION_22  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_21  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_17  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_11  | '4.9.4'
        '4.9.4' | JavaVersion.VERSION_1_8 | '4.8.6'
    }
}
