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
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'org.openapi.generator' version '7.14.0'
            }           
            
            repositories { mavenCentral() }
            dependencies {
                compileOnly 'io.swagger.core.v3:swagger-annotations:2.2.30'

                implementation 'com.github.scribejava:scribejava-core:8.3.3'
                implementation 'com.fasterxml.jackson.core:jackson-core:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.19.2'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2'
            
                implementation 'org.glassfish.jersey.core:jersey-client:3.1.11'
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:3.1.11'
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:3.1.11'
            }            
            
            // https://openapi-generator.tech/docs/generators/java
            openApiGenerate {
                generatorName = "java"
                inputSpec = "\$projectDir/src/main/openapi/petstore.yaml"
                outputDir = "\$buildDir/petstore/client"
                apiPackage = "com.petstore.api"
                invokerPackage = "com.petstore"
                modelPackage = "com.petstore.api.model"
                configOptions = [
                        library: "jersey3",
                        dateLibrary: "java8",
                        openApiNullable: "false",
                        hideGenerationTimestamp: "true"
                ]
            }
            compileJava.dependsOn 'openApiGenerate'
            sourceSets.main.java.srcDir "\${openApiGenerate.outputDir.get()}/src/main/java"

            quality {
                strict = false  
                fallbackToCompatibleToolVersion = true
            }
        """)

        fileFromClasspath('src/main/openapi/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/openapi/petstore.yaml')
        // need at least one source to activate quality
        fileFromClasspath('src/main/java/sample/ValidSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/ValidSample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "spotbugs does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found')
        result.output.contains('SpotBugs violations were found')
        result.output.contains('PMD rule violations were found')
    }

    def "Check generated exclusion by path"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'     
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'org.openapi.generator' version '7.14.0'
            }           
            
            repositories { mavenCentral() }
            dependencies {
                compileOnly 'io.swagger.core.v3:swagger-annotations:2.2.30'

                implementation 'com.github.scribejava:scribejava-core:8.3.3'
                implementation 'com.fasterxml.jackson.core:jackson-core:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.19.2'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2'
            
                implementation 'org.glassfish.jersey.core:jersey-client:3.1.11'
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:3.1.11'
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:3.1.11'
            }            
            
            // https://openapi-generator.tech/docs/generators/java
            openApiGenerate {
                generatorName = "java"
                inputSpec = "\$projectDir/src/main/openapi/petstore.yaml"
                outputDir = "\$buildDir/petstore/client"
                apiPackage = "com.petstore.api"
                invokerPackage = "com.petstore"
                modelPackage = "com.petstore.api.model"
                configOptions = [
                        library: "jersey3",
                        dateLibrary: "java8",
                        openApiNullable: "false",
                        hideGenerationTimestamp: "true"
                ]
            }
            compileJava.dependsOn 'openApiGenerate'
            sourceSets.main.java.srcDir "\${openApiGenerate.outputDir.get()}/src/main/java"

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                
                excludeSources = fileTree(openApiGenerate.outputDir.get())
            }
        """)

        fileFromClasspath('src/main/openapi/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/openapi/petstore.yaml')
        // need at least one source to activate quality
        fileFromClasspath('src/main/java/sample/ValidSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/ValidSample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "plugins does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('SpotBugs violations were found')
        !result.output.contains('PMD rule violations were found')
    }


    def "Check generated exclusion by path 2"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'     
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'org.openapi.generator' version '7.14.0'
            }           
            
            repositories { mavenCentral() }
            dependencies {
                compileOnly 'io.swagger.core.v3:swagger-annotations:2.2.30'

                implementation 'com.github.scribejava:scribejava-core:8.3.3'
                implementation 'com.fasterxml.jackson.core:jackson-core:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.19.2'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2'
            
                implementation 'org.glassfish.jersey.core:jersey-client:3.1.11'
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:3.1.11'
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:3.1.11'
            }            
            
            // https://openapi-generator.tech/docs/generators/java
            openApiGenerate {
                generatorName = "java"
                inputSpec = "\$projectDir/src/main/openapi/petstore.yaml"
                outputDir = "\$buildDir/petstore/client"
                apiPackage = "com.petstore.api"
                invokerPackage = "com.petstore"
                modelPackage = "com.petstore.api.model"
                configOptions = [
                        library: "jersey3",
                        dateLibrary: "java8",
                        openApiNullable: "false",
                        hideGenerationTimestamp: "true"
                ]
            }
            compileJava.dependsOn 'openApiGenerate'
            sourceSets.main.java.srcDir "\${openApiGenerate.outputDir.get()}/src/main/java"

            quality {
                strict = false
                fallbackToCompatibleToolVersion = true
                
                excludeSources = fileTree(openApiGenerate.outputDir.get())
            }
        """)

        fileFromClasspath('src/main/openapi/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/openapi/petstore.yaml')
        // tools must react on this source to show that exclusion applied correctly
        fileFromClasspath('src/main/java/sample/Sample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/Sample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "plugins does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        result.output.contains('Checkstyle rule violations were found in 1 file')
        result.output.contains('SpotBugs violations were found in 1 file')
        result.output.contains('PMD rule violations were found in 1 file')
    }


    def "Check generated exclusion by package"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.quality'  
                id 'com.github.spotbugs' version '$SPOTBUGS_PLUGIN'
                id 'org.openapi.generator' version '7.14.0'
            }           
            
            repositories { mavenCentral() }
            dependencies {
                compileOnly 'io.swagger.core.v3:swagger-annotations:2.2.30'

                implementation 'com.github.scribejava:scribejava-core:8.3.3'
                implementation 'com.fasterxml.jackson.core:jackson-core:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.19.2'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.19.2'
                implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2'
            
                implementation 'org.glassfish.jersey.core:jersey-client:3.1.11'
                implementation 'org.glassfish.jersey.media:jersey-media-json-jackson:3.1.11'
                implementation  'org.glassfish.jersey.media:jersey-media-multipart:3.1.11'
            }            
            
            // https://openapi-generator.tech/docs/generators/java
            openApiGenerate {
                generatorName = "java"
                inputSpec = "\$projectDir/src/main/openapi/petstore.yaml"
                outputDir = "\$buildDir/petstore/client"
                apiPackage = "com.petstore.api"
                invokerPackage = "com.petstore"
                modelPackage = "com.petstore.api.model"
                configOptions = [
                        library: "jersey3",
                        dateLibrary: "java8",
                        openApiNullable: "false",
                        hideGenerationTimestamp: "true"
                ]
            }
            compileJava.dependsOn 'openApiGenerate'
            sourceSets.main.java.srcDir "\${openApiGenerate.outputDir.get()}/src/main/java"

            quality {
                strict = false 
                fallbackToCompatibleToolVersion = true
                
                exclude 'com/petstore/**'
            }
        """)

        fileFromClasspath('src/main/openapi/petstore.yaml', '/ru/vyarus/gradle/plugin/quality/gen/openapi/petstore.yaml')
        // need at least one source to activate quality
        fileFromClasspath('src/main/java/sample/ValidSample.java', '/ru/vyarus/gradle/plugin/quality/java/sample/ValidSample.java')


        when: "run check task with java sources"
        BuildResult result = run('check')

        then: "plugins does not detect violations"
        result.task(":check").outcome == TaskOutcome.SUCCESS
        !result.output.contains('Checkstyle rule violations were found')
        !result.output.contains('SpotBugs violations were found')
        !result.output.contains('PMD rule violations were found')
    }
}
