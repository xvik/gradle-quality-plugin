package ru.vyarus.gradle.plugin.quality.tools.cpd

import de.aaschmid.gradle.plugins.cpd.CpdExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.quality.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 07.11.2019
 */
class CpdIntegrationTest extends AbstractTest {

    def "Check cpd integration"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()
        file('src/test/java').mkdirs()

        file('src/main/java/Sample.java').createNewFile()
        file('src/test/java/SampleTest.java').createNewFile()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            quality {
                strict = false
            }
        }

        then: "cpd configured"
        CpdExtension extension = project.extensions.cpd
        extension.ignoreFailures
        extension.toolVersion == project.extensions.quality.pmdVersion
        extension.language == 'java'
        def task = project.tasks.cpdCheck
        task.source.files.collect { it.name } == ['Sample.java']
        task.ignoreFailures
        dependsOn(project.check).contains task.name
    }

    def "Check cpd groovy sources cleanup"() {

        when: "apply plugin"
        file('src/main/groovy').mkdirs()
        file('src/test/groovy').mkdirs()

        file('src/main/groovy/GSample.groovy').createNewFile()
        file('src/test/groovy/GSampleTest.groovy').createNewFile()

        Project project = project {
            apply plugin: 'groovy'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            quality {
                strict = false
            }
        }

        then: "cpd sources cleared"
        project.tasks.cpdCheck.source.files.collect { it.name } == ['GSample.groovy']
    }


    def "Check disabled cpd sources override"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()
        file('src/test/java').mkdirs()

        file('src/main/java/Sample.java').createNewFile()
        file('src/test/java/SampleTest.java').createNewFile()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            quality {
                strict = false
                cpdUnifySources = false
            }
        }

        then: "cpd configured"
        CpdExtension extension = project.extensions.cpd
        extension.ignoreFailures
        extension.toolVersion == project.extensions.quality.pmdVersion
        extension.language == 'java'
        def task = project.tasks.cpdCheck
        task.source.files.collect { it.name } == ['Sample.java', 'SampleTest.java']
        task.ignoreFailures
    }

    def "Check extra source dirs applied"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()
        file('build/generated/java').mkdirs()

        file('src/main/java/Sample.java').createNewFile()
        file('build/generated/java/SampleTest.java').createNewFile()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            sourceSets.main {
                java {
                    srcDir 'build/generated/java'
                }
            }

            quality {
                strict = false
            }
        }

        then: "sources correct"
        project.tasks.cpdCheck.source.files.collect { it.name } as Set == ['Sample.java', 'SampleTest.java'] as Set
    }


    def "Check custom source set applied"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()
        file('src/custom/java').mkdirs()

        file('src/main/java/Sample.java').createNewFile()
        file('src/custom/java/SampleTest.java').createNewFile()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            sourceSets {
                custom {
                    java {
                        srcDirs = ['src/custom/java']
                    }
                }
            }

            quality {
                strict = false
                sourceSets = [project.sourceSets.main, project.sourceSets.custom]
            }
        }

        then: "sources correct"
        project.tasks.cpdCheck.source.files.collect { it.name } as Set == ['Sample.java', 'SampleTest.java'] as Set
    }

    def "Check cpd support disabled"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()
        file('src/test/java').mkdirs()

        file('src/main/java/Sample.java').createNewFile()
        file('src/test/java/SampleTest.java').createNewFile()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            quality {
                strict = false
                cpd = false
            }
        }

        then: "cpd configured"
        CpdExtension extension = project.extensions.cpd
        !extension.ignoreFailures
        extension.toolVersion != project.extensions.quality.pmdVersion
        extension.language == 'java'
        def task = project.tasks.cpdCheck
        task.source.files.collect { it.name } == ['Sample.java', 'SampleTest.java']
        !task.ignoreFailures
    }


    def "Check multi-module cpd integration"() {

        when: "apply plugin"
        file('sub/src/main/java').mkdirs()
        file('sub/src/test/java').mkdirs()

        file('sub/src/main/java/Sample.java').createNewFile()
        file('sub/src/test/java/SampleTest.java').createNewFile()

        Project project = projectBuilder {
            apply plugin: 'de.aaschmid.cpd'
        }
                .child('sub') {
                    apply plugin: 'java'
                    apply plugin: 'ru.vyarus.quality'

                    quality {
                        strict = false
                    }
                }
                .build()

        then: "cpd partly configured"
        CpdExtension extension = project.extensions.cpd
        extension.ignoreFailures
        extension.toolVersion == project.project(':sub').extensions.quality.pmdVersion
        def task = project.tasks.cpdCheck
        // sources were not re-configured for root project
        task.source.files.collect { it.name } == ['Sample.java']
        task.ignoreFailures
        dependsOn(project.project(':sub').tasks.check).contains task.name
    }

    def "Check multi-module cpd middle integration"() {

        when: "apply plugin"
        file('subroot/sub/src/main/java').mkdirs()
        file('subroot/sub/src/test/java').mkdirs()

        file('subroot/sub/src/main/java/Sample.java').createNewFile()
        file('subroot/sub/src/test/java/SampleTest.java').createNewFile()

        Project project = projectBuilder()
        // cpd declared in the middle project
                .child('subroot') {
                    apply plugin: 'de.aaschmid.cpd'
                }
                .childOf('subroot', 'sub') {
                    apply plugin: 'java'
                    apply plugin: 'ru.vyarus.quality'

                    quality {
                        strict = false
                    }
                }
                .build()

        then: "cpd partly configured"
        def subroot = project.project(':subroot')
        CpdExtension extension = subroot.extensions.cpd
        extension.ignoreFailures
        extension.toolVersion == project.project(':subroot:sub').extensions.quality.pmdVersion
        def task = subroot.tasks.cpdCheck
        // sources were not re-configured for root project
        task.source.files.collect { it.name } == ['Sample.java']
        task.ignoreFailures
        dependsOn(project.project(':subroot:sub').tasks.check).contains task.name
    }

    def "Check groovy only source detection"() {

        when: "apply plugin"
        file('src/main/groovy').mkdirs()

        file('src/main/groovy/Sample.groovy').createNewFile()

        Project project = project {
            apply plugin: 'groovy'
            apply plugin: 'de.aaschmid.cpd'
            apply plugin: 'ru.vyarus.quality'

            quality {
                strict = false
            }
        }

        then: "cpd language changed"
        project.extensions.cpd.language == 'groovy'
        project.tasks.cpdCheck.language == 'groovy'
    }

    private Set<String> dependsOn(Task task) {
        // dependsOn also contains implicit dependency to task sources
        task.dependsOn.collect { extractDependencies(it)}.flatten().findAll{it} as Set
    }

    private Set<String> extractDependencies(Object dependency) {
        if (dependency instanceof Task) {
            return [dependency.name]
        }
        if (dependency instanceof TaskProvider) {
            return [dependency.get().name]
        }
        if (dependency instanceof TaskCollection) {
            return dependency.names
        }
        return null
    }
}
