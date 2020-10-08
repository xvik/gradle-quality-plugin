package ru.vyarus.gradle.plugin.quality.tools.spotbugs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.quality.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2020
 */
class ExcludeGeneratedSourcesKitTest extends AbstractKitTest {

    def "Check apt generated exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
                
                excludeSources = fileTree('build/generated')
            }
            
            // errors contained in generated sources and they must be excluded
            repositories { mavenCentral() }
            dependencies {
                annotationProcessor "org.mapstruct:mapstruct-processor:1.3.1.Final"
                implementation "org.mapstruct:mapstruct:1.3.1.Final"
            }            
        """)

        fileFromClasspath('src/main/java/com/something/Partner.java', '/ru/vyarus/gradle/plugin/quality/gen/Partner.java')
        fileFromClasspath('src/main/java/com/something/PartnerAccount.java', '/ru/vyarus/gradle/plugin/quality/gen/PartnerAccount.java')
        fileFromClasspath('src/main/java/com/something/PartnerAccountData.java', '/ru/vyarus/gradle/plugin/quality/gen/PartnerAccountData.java')
        fileFromClasspath('src/main/java/com/something/PartnerMapper.java', '/ru/vyarus/gradle/plugin/quality/gen/PartnerMapper.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('SpotBugs violations were found')
    }

    def "Check apt generated exclusion 2"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
            }

            quality {
                checkstyle false
                pmd false
                strict false
                
                exclude '**/*MapperImpl.java'
            }

            // errors contained in generated sources and they must be excluded
            repositories { mavenCentral() }
            dependencies {
                annotationProcessor "org.mapstruct:mapstruct-processor:1.3.1.Final"
                implementation "org.mapstruct:mapstruct:1.3.1.Final"
            }
        """)

        fileFromClasspath('src/main/java/com/something/Partner.java', '/ru/vyarus/gradle/plugin/quality/gen/Partner.java')
        fileFromClasspath('src/main/java/com/something/PartnerAccount.java', '/ru/vyarus/gradle/plugin/quality/gen/PartnerAccount.java')
        fileFromClasspath('src/main/java/com/something/PartnerAccountData.java', '/ru/vyarus/gradle/plugin/quality/gen/PartnerAccountData.java')
        fileFromClasspath('src/main/java/com/something/PartnerMapper.java', '/ru/vyarus/gradle/plugin/quality/gen/PartnerMapper.java')

        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('SpotBugs violations were found')
    }
}
