package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet

/**
 * Quality plugin configuration. Available as 'quality' closure.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @see ru.vyarus.gradle.plugin.quality.QualityPlugin for registration
 */
@CompileStatic
class QualityExtension {

    @CompileStatic(TypeCheckingMode.SKIP)
    QualityExtension(Project project) {
        sourceSets = [project.sourceSets.main] as Collection<SourceSet>
    }

    String checkstyleVersion = '8.26'
    String pmdVersion = '6.18.0'
    String findbugsVersion = '3.0.1'
    String spotbugsVersion = '3.1.12'
    String codenarcVersion = '1.4'

    /**
     * Sets AnimalSniffer version.
     * Works only when 'ru.vyarus.animalsniffer' plugin applied.
     */
    String animalsnifferVersion

    /**
     * Automatically register quality plugins, based on configured (affected) sources ({@link #sourceSets}).
     * For example, if configured sources contain only java sources then only pmd, checkstyle and spotbugs (findbugs)
     * plugins will be activated; if only groovy sources - then codenarc only.
     * <p>
     * When disabled, quality plugins must be registered manually. Only registered plugins will be configured
     * if configuration is not disabled with plugin flags ({@link #pmd}, {@link #checkstyle} etc.).
     * True by default.
     */
    boolean autoRegistration = true

    /**
     * Enable Checkstyle plugin. True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     */
    boolean checkstyle = true

    /**
     * Enable PMD plugin. True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     */
    boolean pmd = true

    /**
     * Enable auto configuration of de.aaschmid.cpd plugin (if applied). CPD is a part of PMD and so should
     * share the same version. Cpd plugin will not be configured at all if option set to false.
     * <p>
     * As cpd plugin is applied manually, then all other cpd configuration may be performed in cpd closure directly
     * (without "afterEvaluate"). Plugin will affect only ignoreFailures, toolVersion, enable xml report and
     * change cpdCheck task sources (see {@link #cpdUnifySources}). cpdCheck task is always linked to check task.
     * In case of multi-module project it means that sub module check will call cpdCheck declared in root project
     * (because module check must also check for potential new duplicate parts).
     */
    boolean cpd = true

    /**
     * Enable FindBugs plugin. True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     * Plugin is not enabled and not configured when spotbugs plugin enabled.
     *
     * @deprecated will be removed later as findbugs plugin is abandoned (spotbugs must be used instead).
     */
    @Deprecated
    boolean findbugs = true

    /**
     * Enable SpotBugs plugin. True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     */
    boolean spotbugs = true

    /**
     * Enable CodeNarc plugin. Ignored if groovy plugin is not applied). True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     */
    boolean codenarc = true

    /**
     * Enable PMD incremental analysis (cache results between builds to speed up processing).
     * This is a shortcut for pmd plugin's {@code pmd.incrementalAnalysis } configuration option.
     * Option is disabled by default due to possible side effects with build gradle cache or incremental builds.
     */
    boolean pmdIncremental = false

    /**
     * By default, cpd looks in all sources (cpd gradle plugin behaviour). When option enabled, quality plugin will
     * exclude all not configured source sets from cpd task sources. In case of multi-module build, where
     * cpd project declared in root project, all subprojects with quality plugin will exclude their sourceSets not
     * configured for quality checks. Also, all custom exclusions ({@link #exclude}, {@link #excludeSources})
     * will also be excluded.
     * <p>
     * When disabled, cpdCheck task sources will not be modified (and so cpd will check all source sets without manual
     * configuration).
     */
    boolean cpdUnifySources = true

    /**
     * The analysis effort level. The value specified should be one of min, default, or max.
     * Higher levels increase precision and find more bugs at the expense of running time and
     * memory consumption. Default is 'max'.
     *
     * @deprecated will be removed later as findbugs plugin is abandoned (spotbugs must be used instead).
     */
    @Deprecated
    String findbugsEffort = 'max'

    /**
     * The priority threshold for reporting bugs. If set to low, all bugs are reported.
     * If set to medium, medium and high priority bugs are reported.
     * If set to high, only high priority bugs are reported. Default is 'medium'.
     *
     * @deprecated will be removed later as findbugs plugin is abandoned (spotbugs must be used instead).
     */
    @Deprecated
    String findbugsLevel = 'medium'

    /**
     * The analysis effort level. The value specified should be one of min, default, or max.
     * Higher levels increase precision and find more bugs at the expense of running time and
     * memory consumption. Default is 'max'.
     */
    String spotbugsEffort = findbugsEffort

    /**
     * The priority threshold for reporting bugs. If set to low, all bugs are reported.
     * If set to medium, medium and high priority bugs are reported.
     * If set to high, only high priority bugs are reported. Default is 'medium'.
     */
    String spotbugsLevel = findbugsLevel

    /**
     * Spotbugs rank should be an integer value between 1 and 20, where 1 to 4 are scariest, 5 to 9 scary,
     * 10 to 14 troubling, and 15 to 20 of concern bugs.
     * <p>
     * This option allows you to filter low-priority ranks: for example, setting {@code spotbugsMaxRank=15} will
     * filter all bugs with ranks 16-20. Note that this is not the same as {@link #spotbugsLevel}:
     * it has a bit different meaning (note that both priority and rank are shown for each spotbugs
     * violation in console).
     * <p>
     * The only way to apply rank filtering is through exclude filter. Plugin will automatically generate
     * additional rule in your exclude filter or in default one. But it may conflict with manual rank rule declaration
     * (in case if you edit exclude filter manually), so be careful when enabling this option.
     */
    int spotbugsMaxRank = 20

    /**
     * Max memory available for spotbugs task. Note that in gradle 4 spotbugs task maximum memory was
     * 1/4 of physical memory, but in gradle 5 it become only 512mb (default for workers api).
     * To minify impact of this gradle 5 change, default value in extension is 1g now, but it may be not
     * enough for large projects (and so you will have to increase it manually).
     * <p>
     * IMPORTANT: setting will not work if heap size configured directly in spotbugs task (for example, with
     * <code>spotbugsMain.maxHeapSize = '2g'</code>. This was done in order to not break current behaviour
     * (when task memory is already configured) and affect only default cases (mostly caused by gradle 5 transition).
     * <p>
     * See: https://github.com/gradle/gradle/issues/6216 (Reduce default memory settings for daemon and
     * workers).
     */
    String spotbugsMaxHeapSize = '1g'

    /**
     * Javac lint options to show compiler warnings, not visible by default.
     * By default enables deprecation and unchecked options. Applies to all JavaCompile tasks.
     * Full list of options: http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html#BHCJCABJ
     */
    List<String> lintOptions = ['deprecation', 'unchecked']

    /**
     * Strict quality leads to build fail on any violation found. If disabled, all violation
     * are just printed to console (if console reporting enabled).
     * True by default.
     */
    boolean strict = true

    /**
     * When false, disables quality tasks execution. Allows disabling tasks without removing plugins.
     * Quality tasks are still registered, but skip execution, except when task called directly or through
     * checkQualityMain (or other source set) grouping task.
     * True by default.
     */
    boolean enabled = true

    /**
     * When false, disables reporting quality issues to console. Only gradle general error messages will
     * remain in logs. This may be useful in cases when project contains too many warnings.
     * Also, console reporting require xml reports parsing, which could be time consuming in case of too
     * many errors (large xml reports).
     * True by default.
     * @see #htmlReports to disable html reporting
     */
    boolean consoleReporting = true

    /**
     * When false, no html report will be built. By default, html reports are always built.
     * @see #consoleReporting for disabling console reporting
     */
    boolean htmlReports = true

    /**
     * Source sets to apply checks on.
     * Default is [project.sourceSets.main] to apply only for project sources, excluding tests.
     */
    Collection<SourceSet> sourceSets

    /**
     * Source patterns (relative to source dir) to exclude from checks. Simply sets exclusions to quality tasks.
     * <p>
     * Animalsniffer is not affected because
     * it's a different kind of check (and, also, it operates on classes so source patterns may not comply).
     * <p>
     * Spotbugs (findbugs) does not support exclusion directly, but plugin will resolve excluded classes and apply
     * them to xml exclude file (default one or provided by user).
     * <p>
     * By default nothing is excluded.
     * <p>
     * IMPORTANT: Patterns are checked relatively to source set dirs (not including them). So you can only
     * match source files and packages, but not absolute file path (this is gradle specific, not plugin).
     *
     * @see org.gradle.api.tasks.SourceTask#exclude(java.lang.Iterable) (base class for all quality tasks)
     */
    Collection<String> exclude = []

    /**
     * Direct sources to exclude from checks (except animalsniffer).
     * This is useful as last resort, when extension or package is not enough for filtering.
     * Use {@link Project#files(java.lang.Object)} or {@link Project#fileTree(java.lang.Object)}
     * to create initial collections and apply filter on it (using
     * {@link org.gradle.api.file.FileTree#matching(groovy.lang.Closure)}).
     * <p>
     * Plugin will include files into spotbugs (findbugs) exclusion filter xml (default one or provided by user).
     * <p>
     * Note: this must be used when excluded classes can't be extracted to different source set and
     * filter by package and filename is not sufficient.
     */
    FileCollection excludeSources

    /**
     * Configuration files directory. It may contain custom plugin configurations (not required).
     * By default its gradle/config/.
     */
    String configDir = 'gradle/config/'

    /**
     * Shortcut for {@link #exclude}
     *
     * @param patterns exclusion patterns (relative to source root)
     */
    @SuppressWarnings('ConfusingMethodName')
    void exclude(String... patterns) {
        exclude.addAll(patterns)
    }
}
