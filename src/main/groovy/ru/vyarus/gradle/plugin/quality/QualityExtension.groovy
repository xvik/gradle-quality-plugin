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

    String checkstyleVersion = '7.6'
    String pmdVersion = '5.5.4'
    String findbugsVersion = '3.0.1'
    String codenarcVersion = '0.27.0'
    /**
     * Sets AnimalSniffer version.
     * Works only when 'ru.vyarus.animalsniffer' plugin applied.
     */
    String animalsnifferVersion

    /**
     * Automatically register quality plugins, based on configured (affected) sources ({@link #sourceSets}).
     * For example, if configured sources contain only java sources then only pmd, checkstyle and findbugs plugins
     * will be activated; if only groovy sources - then codenarc only.
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
     * Enable FindBugs plugin. True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     */
    boolean findbugs = true

    /**
     * Enable CodeNarc plugin. Ignored if groovy plugin is not applied). True by default.
     * If plugin enabled manually then disabling this option will prevent applying plugin configuration.
     */
    boolean codenarc = true

    /**
     * The analysis effort level. The value specified should be one of min, default, or max.
     * Higher levels increase precision and find more bugs at the expense of running time and
     * memory consumption. Default is 'max'.
     */
    String findbugsEffort = 'max'

    /**
     * The priority threshold for reporting bugs. If set to low, all bugs are reported.
     * If set to medium, medium and high priority bugs are reported.
     * If set to high, only high priority bugs are reported. Default is 'medium'.
     */
    String findbugsLevel = 'medium'

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
     */
    boolean consoleReporting = true

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
     * Findbugs does not support exclusion directly, but plugin will resolve excluded classes and apply
     * them to xml exclude file (default one or provided by user).
     * <p>
     * By default nothing is excluded.
     * <p>
     * IMPORTANT: Patterns are checked relatively to source set dirs (not including them). So you can only
     * match source files and packages, but not absolute file path (this is gradle specific, not plugin).
     *
     * @see org.gradle.api.tasks.SourceTask#exclude(java.lang.Iterable) (base class for all quality tasks)
     */
    Collection<String> exclude

    /**
     * Direct sources to exclude from checks (except animalsniffer).
     * This is useful as last resort, when extension or package is not enough for filtering.
     * Use {@link Project#files(java.lang.Object)} or {@link Project#fileTree(java.lang.Object)}
     * to create initial collections and apply filter on it (using
     * {@link org.gradle.api.file.FileTree#matching(groovy.lang.Closure)}).
     * <p>
     * Plugin will include files into findbugs exclusion filter xml (default one or provided by user).
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
}
