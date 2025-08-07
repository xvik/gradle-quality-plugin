package ru.vyarus.gradle.plugin.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 07.10.2020
 */
@IgnoreIf({jvm.java8})
class GeneratedSourceExcludeKitTest extends AbstractKitTest {

    def "Check generated check failed without exclusion"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'org.hidetake.swagger.generator' version '2.19.2' 
            }           
            
            repositories { mavenCentral() }
            dependencies {
                swaggerCodegen 'io.swagger.codegen.v3:swagger-codegen-cli:3.0.19'
                implementation 'io.swagger.core.v3:swagger-annotations:2.1.0'

                implementation 'com.fasterxml.jackson.core:jackson-core:2.10.4'        
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.10.4'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.10.4'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.4'
                
                implementation 'org.glassfish.jersey.core:jersey-client:2.29.1'          
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:2.29.1'            
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:2.29.1'
            }
            
            swaggerSources {
                petstoreApi {
                    inputFile = file('src/main/swagger/petstore.yaml')
                    code {
                        language = 'java'
                        configFile = file('src/main/swagger/config.json')
                    }
                }
            }
            compileJava.dependsOn swaggerSources.petstoreApi.code
            
            sourceSets.main.java.srcDir "\${swaggerSources.petstoreApi.code.outputDir}/src/main/java"
            sourceSets.main.resources.srcDir "\${swaggerSources.petstoreApi.code.outputDir}/src/main/resources"

            quality {
                pmd = false
                spotbugs = false 
                strict = false  
                fallbackToCompatibleToolVersion = true
            }
        """)

        fileFromClasspath('src/main/swagger/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/swagger/petstore.yaml')
        fileFromClasspath('src/main/swagger/config.json', '/ru/vyarus/gradle/plugin/quality/gen/swagger/config.json')
        // need at least one source to activate quality
        fileFromClasspath('src/main/java/sample/ValidSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/ValidSample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "spotbugs does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle violations were found')
    }

    def "Check generated exclusion by path"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                 id 'org.hidetake.swagger.generator' version '2.18.2' 
            }           
            
            repositories { mavenCentral() }
            dependencies {
                swaggerCodegen 'io.swagger.codegen.v3:swagger-codegen-cli:3.0.19'
                implementation 'io.swagger.core.v3:swagger-annotations:2.1.0'

                implementation 'com.fasterxml.jackson.core:jackson-core:2.10.4'        
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.10.4'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.10.4'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.4'
                
                implementation 'org.glassfish.jersey.core:jersey-client:2.29.1'          
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:2.29.1'            
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:2.29.1'
            }
            
            swaggerSources {
                petstoreApi {
                    inputFile = file('src/main/swagger/petstore.yaml')
                    code {
                        language = 'java'
                        configFile = file('src/main/swagger/config.json')
                    }
                }
            }
            compileJava.dependsOn swaggerSources.petstoreApi.code
            
            sourceSets.main.java.srcDir "\${swaggerSources.petstoreApi.code.outputDir}/src/main/java"
            sourceSets.main.resources.srcDir "\${swaggerSources.petstoreApi.code.outputDir}/src/main/resources"

            quality {
                pmd = false
                spotbugs = false
                strict = false
                fallbackToCompatibleToolVersion = true
                
                excludeSources = fileTree(swaggerSources.petstoreApi.code.outputDir)
            }
        """)

        fileFromClasspath('src/main/swagger/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/swagger/petstore.yaml')
        fileFromClasspath('src/main/swagger/config.json', '/ru/vyarus/gradle/plugin/quality/gen/swagger/config.json')
        // need at least one source to activate quality
        fileFromClasspath('src/main/java/sample/ValidSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/ValidSample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "plugins does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle violations were found')
    }


    def "Check generated exclusion by package"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'
                id 'org.hidetake.swagger.generator' version '2.18.2' 
            }           
            
            repositories { mavenCentral() }
            dependencies {
                swaggerCodegen 'io.swagger.codegen.v3:swagger-codegen-cli:3.0.19'
                implementation 'io.swagger.core.v3:swagger-annotations:2.1.0'

                implementation 'com.fasterxml.jackson.core:jackson-core:2.10.4'        
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.10.4'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.10.4'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.4'
                
                implementation 'org.glassfish.jersey.core:jersey-client:2.29.1'          
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:2.29.1'            
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:2.29.1'
            }
            
            swaggerSources {
                petstoreApi {
                    inputFile = file('src/main/swagger/petstore.yaml')
                    code {
                        language = 'java'
                        configFile = file('src/main/swagger/config.json')
                    }
                }
            }
            compileJava.dependsOn swaggerSources.petstoreApi.code
            
            sourceSets.main.java.srcDir "\${swaggerSources.petstoreApi.code.outputDir}/src/main/java"
            sourceSets.main.resources.srcDir "\${swaggerSources.petstoreApi.code.outputDir}/src/main/resources"

            quality {
                pmd = false
                spotbugs = false
                strict = false 
                fallbackToCompatibleToolVersion = true
                
                exclude 'com/petstore/**'
            }
        """)

        fileFromClasspath('src/main/swagger/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/swagger/petstore.yaml')
        fileFromClasspath('src/main/swagger/config.json', '/ru/vyarus/gradle/plugin/quality/gen/swagger/config.json')
        // need at least one source to activate quality
        fileFromClasspath('src/main/java/sample/ValidSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/ValidSample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "plugins does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle violations were found')
    }
}
