# Grouping tasks

!!! summary ""
    checkQuality\[Main] task

Each quality plugin (checkstyle, pmd, findbugs etc) registers separate quality task for each source set. 
For example, `checkstyleMain` and `checkstyleTest`.

But `check`  task will only depend on tasks for configured source sets (`quality.sourceSets`).

For example, by default, only main source set is configured, so only `checkstyleMain` assigned to `check`.
Anyway, `checkstyleTest` task is registered and may be called directly (even if it's not used for project validation).

By analogy, quality plugin register grouping task for each available source set: `checkQualityMain`, `checkQualityTest` etc.
These tasks simply calls all quality tasks relative to source set. 
For example, if we have java quality plugins registered then calling `checkQualityMain` will call
`checkstyleMain`, `pmdMain` and `findbugsMain`.

This is just a handy shortcut to run quality check tasks for exact source set without running tests (like main `check`).
Generally usable to periodically check code violations. 