package ru.vyarus.gradle.plugin.quality

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.AbstractTest
import ru.vyarus.gradle.plugin.quality.QualityExtension

/**
 * @author Vyacheslav Rusakov
 * @since 21.03.2017
 */
class ExcludeConfigurationTest extends AbstractTest {

    def "Check single exclude configuration"() {

        when: "single exclude"
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.quality"

            quality {
                exclude '**/*.java'
            }
        }

        then: "configured"
        project.extensions.findByType(QualityExtension).exclude == ['**/*.java']
    }

    def "Check couple of exclude configs"() {

        when: "couple of single exclude"
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.quality"

            quality {
                exclude '**/sample/**'
                exclude '**/other/**'
            }
        }

        then: "configured"
        project.extensions.findByType(QualityExtension).exclude == ['**/sample/**', '**/other/**']

    }

    def "Check multiple excludes"() {

        when: "multiple excludes"
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.quality"

            quality {
                exclude '**/sample/**',
                        '**/other/**'
            }
        }

        then: "configured"
        project.extensions.findByType(QualityExtension).exclude == ['**/sample/**', '**/other/**']

    }

    def "Check direct property"() {

        when: "direct property usage"
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.quality"

            quality {
                exclude = ['**/sample/**',
                           '**/other/**']
            }
        }

        then: "configured"
        project.extensions.findByType(QualityExtension).exclude == ['**/sample/**', '**/other/**']
    }
}