package ru.vyarus.gradle.plugin.quality.util

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ProviderFactory
import ru.vyarus.gradle.plugin.quality.AbstractTest
import ru.vyarus.gradle.plugin.quality.service.ConfigsService

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
        ConfigsService loader = new ConfigServiceImpl(project)
        loader.initUserConfigs(false)
        Map<String, Long> reference = [:]
        loader.parameters.configDir.get().asFile.eachFileRecurse {
            if (!it.directory) {
                println "$it.name = ${it.length()}"
                reference[it.name] = it.length()
                it.delete()
            }
        }
        assert reference.size() == 6


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
        loader.parameters.configDir.get().asFile.eachFileRecurse {
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
        ConfigsService loader = new ConfigServiceImpl(project)
        loader.initUserConfigs(false)
        Map<String, Long> reference = [:]
        loader.parameters.configDir.get().asFile.eachFileRecurse {
            if (!it.directory) {
                println "$it.name = ${it.length()}"
                reference[it.name] = it.length()
                it.delete()
            }
        }
        assert reference.size() == 6


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
        loader.parameters.configDir.get().asFile.eachFileRecurse {
            if (!it.directory) {
                println "verification $it.name ${it.length()} (${reference[it.name]})"
                assert reference[it.name] == it.length()
            }
        }
    }

    class ConfigServiceImpl extends ConfigsService {

        Project project

        ConfigServiceImpl(Project project) {
            this.project = project
        }

        @Override
        ProviderFactory getProviderFactory() {
            return project.providers
        }

        @Override
        Params getParameters() {
            Params params = new Params() {

                DirectoryProperty dir = project.objects.directoryProperty()
                DirectoryProperty temp = project.objects.directoryProperty()

                @Override
                DirectoryProperty getConfigDir() {
                    return dir
                }

                @Override
                DirectoryProperty getTempDir() {
                    return temp
                }

            }
            params.configDir.set(new File("/tmp/some"))
            params.tempDir.set(new File("/tmp/other"))
            return params
        }
    }
}
