# Java project with excluded generated code

Swagger codegen used to generate swagger client sources.
Without exclusion, quality tools would also check these sources, which are certainly contain violations.

To ignore violations in the generated sources, these sources must be excluded.

Here exclusion by class path used. 

Note that, sometimes, it is not possible to use path exclusion because sources generated in the same package
as application. In such cases [exclusion by project directory](../java-codegen) should be used

## Output

```
> Task :openApiGenerate
ApiResponse (reserved word) cannot be used as model name. Renamed to ModelApiResponse
############################################################################################
# Thanks for using OpenAPI Generator.                                                      #
# We appreciate your support! Please consider donation to help us maintain this project.   #
# https://opencollective.com/openapi_generator/donate                                      #
############################################################################################
Successfully generated code to /home/xvik/projects/xvik/gradle-quality-plugin/build/gradleTest/8.4/java-codegen2/build/petstore/client

> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :copyQualityConfigs
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :checkstyleMain
> Task :pmdMain
> Task :spotbugsMain
> Task :check
```

No violations were found in generated sources.