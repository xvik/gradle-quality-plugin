package ru.vyarus.gradle.plugin.quality.util

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.quality.AbstractTest
import ru.vyarus.gradle.plugin.quality.ConfigLoader

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2020
 */
class ConfigInitSyncTest extends AbstractTest {

    ExecutorService executor

    void setup() {
        executor = Executors.newFixedThreadPool(20)
    }

    void cleanup() {
        executor.shutdown()
    }

    def "Check concurrent writes"() {

        setup: "prepare project"
        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'
        }
        ConfigLoader loader = new ConfigLoader(project)
        loader.initUserConfigs(false)
        Map<String, Long> reference = [:]
        loader.configDir.eachFileRecurse {
            if (!it.directory) {
                println "$it.name = ${it.length()}"
                reference[it.name] = it.length()
                it.delete()
            }
        }
        assert reference.size() == 7


        when: "initializing configs concurrently"
        List<Future<?>> executed = []
        int times = 20
        times.times({
            executed << executor.submit({
                loader.initUserConfigs(false)
                return null
            })
        })
        // lock until finish
        executed.each({ it.get() })

        then: "no duplicates"
        loader.configDir.eachFileRecurse {
            if (!it.directory) {
                println "verification $it.name ${it.length()} (${reference[it.name]})"
                assert reference[it.name] == it.length()
            }
        }
    }

    def "Check concurrent writes with override"() {

        setup: "prepare project"
        Project project = project {
            apply plugin: 'java'
            apply plugin: 'ru.vyarus.quality'
        }
        ConfigLoader loader = new ConfigLoader(project)
        loader.initUserConfigs(false)
        Map<String, Long> reference = [:]
        loader.configDir.eachFileRecurse {
            if (!it.directory) {
                println "$it.name = ${it.length()}"
                reference[it.name] = it.length()
                it.delete()
            }
        }
        assert reference.size() == 7


        when: "initializing configs concurrently"
        List<Future<?>> executed = []
        int times = 20
        times.times({
            executed << executor.submit({
                loader.initUserConfigs(true)
                return null
            })
        })
        // lock until finish
        executed.each({ it.get() })

        then: "no duplicates"
        loader.configDir.eachFileRecurse {
            if (!it.directory) {
                println "verification $it.name ${it.length()} (${reference[it.name]})"
                assert reference[it.name] == it.length()
            }
        }
    }
}
