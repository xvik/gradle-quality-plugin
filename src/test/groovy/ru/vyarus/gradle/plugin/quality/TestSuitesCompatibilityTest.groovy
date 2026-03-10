package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 10.03.2026
 */
class TestSuitesCompatibilityTest extends AbstractKitTest {

    def "Check test suites source sets compatibility"() {
        setup:
        build("""
            plugins {
                id 'groovy'
                id 'ru.vyarus.quality'
                id 'jvm-test-suite'
            }

            quality {
                sourceSets = ['main', 'test', 'integrationTest']
                strict = false
                fallbackToCompatibleToolVersion = true
                consoleReporting = false
            }
            
            testing {
                suites {
                    configureEach {
                        useJUnitJupiter()
                        dependencies {
                            implementation "org.testcontainers:junit-jupiter:1.21.4"
                            implementation "org.testcontainers:postgresql:1.21.4"
                            implementation 'org.springframework.boot:spring-boot-testcontainers:3.5.9'
            
                            implementation platform("org.junit:junit-bom:6.0.1")
                            implementation platform("org.testcontainers:testcontainers-bom:2.0.2")
                        }
                    }
                    test {
                        targets {
                            configureEach {
                                testTask.configure {
                                    systemProperty "spring.profiles.active", "test"
                                    dependsOn 'checkstyleMain', 'checkstyleTest', 'cleanTest'
                                }
                            }
                        }
                        sources {
                            compileClasspath += sourceSets.main.output
                            runtimeClasspath += sourceSets.main.output
                        }
                    }
                    integrationTest(JvmTestSuite) {
                        sources {
                            java {
                                srcDirs = ['src/integrationTest/java']
                            }
                            resources {
                                srcDirs = ['src/integrationTest/resources']
                            }
                        }
                        targets {
                            configureEach {
                                testTask.configure {
                                    systemProperty "spring.profiles.active", "integrationTest"
                                    dependsOn 'checkstyleIntegrationTest', 'checkstyleIntegrationTest', 'cleanIntegrationTest'
                                    shouldRunAfter(test)
                                }
                            }
                        }
                    }
                }
            }

            repositories {
                mavenCentral() //required for testKit run
            }

            dependencies {
                implementation localGroovy()
            }
        """)

        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/test/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')
        fileFromClasspath('src/integrationTest/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')

        when: "run check task with both sources"
        BuildResult result = run('check')

        then: "all plugins detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
    }
}
