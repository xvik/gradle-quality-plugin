# Java project with excluded generated code

Swagger codegen used to generate swagger client sources.
Without exclusion, quality tools would also check these sources, which are certainly contain violations.

To ignore violations in the generated sources, these sources must be excluded.

Here exclusion by class path used. 

Note that, sometimes, it is not possible to use path exclusion because sources generated in the same package
as application. In such cases [exclusion by project directory](../java-codegen) should be used