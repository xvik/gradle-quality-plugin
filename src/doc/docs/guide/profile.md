# Profiling

Gradle profile report (`--profile` [option](https://docs.gradle.org/current/userguide/gradle_command_line.html)) 
shows quality tools tasks time (checkstyleMain, pmdMain etc), 
which includes both tool execution time and console reporting (performed by quality plugin). 

If you need to know exact console reporting time use `--info` option. Plugin writes reporting execution time as info log 
(see log messages starting with `[plugin:quality]` just after quality tools logs).

Alternatively, you can disable console reporting and run quality tasks with `--profile` again to see "pure" quality plugins time. 
