package ru.vyarus.gradle.plugin.quality.task

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.quality.AbstractTest


/**
 * @author Vyacheslav Rusakov
 * @since 01.09.2016
 */
class GroupingTasksTest extends AbstractTest {

    def "Check grouping tasks registration"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'

            quality {
                sourceSets = [project.sourceSets.main, project.sourceSets.test]
            }
        }

        then: "grouping tasks registered"
        project.tasks.checkQualityMain
        project.tasks.checkQualityTest

        then: "correct tasks grouped"
        dependsOn(project.tasks.checkQualityMain) == ['checkstyleMain', 'pmdMain', 'spotbugsMain'] as Set
        dependsOn(project.tasks.checkQualityTest) == ['checkstyleTest', 'pmdTest', 'spotbugsTest'] as Set
    }

    def "Check groovy grouping tasks registration"() {

        when: "apply plugin"
        file('src/main/groovy').mkdirs()

        Project project = project {
            apply plugin: 'groovy'
            apply plugin: 'ru.vyarus.quality'

            quality {
                sourceSets = [project.sourceSets.main, project.sourceSets.test]
            }
        }

        then: "grouping tasks registered"
        project.tasks.checkQualityMain
        project.tasks.checkQualityTest

        then: "correct tasks grouped"
        dependsOn(project.tasks.checkQualityMain) == ['codenarcMain'] as Set
        dependsOn(project.tasks.checkQualityTest) == ['codenarcTest'] as Set
    }

    def "Check tasks created for all sources"() {

        when: "apply plugin"
        file('src/main/groovy').mkdirs()

        Project project = project {
            apply plugin: 'groovy'
            apply plugin: 'ru.vyarus.quality'

            sourceSets {
                tata {}
            }

            quality {
                sourceSets = [project.sourceSets.main]
            }
        }

        then: "grouping tasks registered"
        project.tasks.checkQualityMain
        project.tasks.checkQualityTest
        project.tasks.checkQualityTata

        then: "correct tasks grouped"
        dependsOn(project.tasks.checkQualityMain) == ['codenarcMain'] as Set
        dependsOn(project.tasks.checkQualityTest) == ['codenarcTest'] as Set
        dependsOn(project.tasks.checkQualityTata) == ['codenarcTata'] as Set
    }


    def "Check animalsniffer integration"() {

        when: "apply plugin"
        file('src/main/java').mkdirs()

        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.animalsniffer'
            apply plugin: 'ru.vyarus.quality'

            quality {
                sourceSets = [project.sourceSets.main, project.sourceSets.test]
            }
        }

        then: "grouping tasks registered"
        project.tasks.checkQualityMain
        project.tasks.checkQualityTest

        then: "correct tasks grouped"
        dependsOn(project.tasks.checkQualityMain) == ['checkstyleMain', 'pmdMain', 'spotbugsMain', 'animalsnifferMain'] as Set
        dependsOn(project.tasks.checkQualityTest) == ['checkstyleTest', 'pmdTest', 'spotbugsTest', 'animalsnifferTest'] as Set
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