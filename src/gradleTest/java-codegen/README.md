# Java project with excluded generated code

Swagger codegen used to generate swagger client sources.
Without exclusion, quality tools would also check these sources, which are certainly contain violations.

To ignore violations in the generated sources, these sources must be excluded.

Here exclusion by project directory used: it is useful when it is impossible to describe [exclusion by
class path](../java-codegen2) (in cases when code generated in the same package as application)