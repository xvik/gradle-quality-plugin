package ru.vyarus.gradle.plugin.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
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

    String checkstyleVersion = '7.1'
    String pmdVersion = '5.5.1'
    String findbugsVersion = '3.0.1'
    String codenarcVersion = '0.25.2'
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
     * True by default
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
     * are just printed to console.
     * True by default.
     */
    boolean strict = true

    /**
     * When false, disables quality tasks execution. Allows disabling tasks without removing plugins.
     * Quality tasks are still registered, but skip execution, except case when task called directly.
     * True by default.
     */
    boolean enabled = true

    /**
     * Source sets to apply checks on.
     * Default is [sourceSets.main] to apply only for project sources, excluding tests.
     */
    Collection<SourceSet> sourceSets

    /**
     * Configuration files directory. It may contain custom plugin configurations (not required).
     * By default its gradle/config/.
     */
    String configDir = 'gradle/config/'
}
