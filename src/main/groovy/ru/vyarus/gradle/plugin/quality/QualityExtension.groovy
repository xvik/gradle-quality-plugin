package ru.vyarus.gradle.plugin.quality

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

/**
 * Quality plugin configuration. Available as 'quality' closure.
 *
 * @author Vyacheslav Rusakov
 * @since 12.11.2015
 * @see ru.vyarus.gradle.plugin.quality.QualityPlugin for registration
 */
class QualityExtension {

    QualityExtension(Project project) {
        sourceSets = [project.sourceSets.main] as Collection<SourceSet>
    }

    String checkstyleVersion = '6.16.1'
    String pmdVersion = '5.4.1'
    String findbugsVersion = '3.0.1'
    String codenarcVersion = '0.25.1'
    /**
     * Sets AnimalSniffer version.
     * Works only when 'ru.vyarus.animalsniffer' plugin applied.
     */
    String animalsnifferVersion

    /**
     * Enable Checkstyle plugin. True by default.
     */
    boolean checkstyle = true

    /**
     * Enable PMD plugin. True by default.
     */
    boolean pmd = true

    /**
     * Enable FindBugs plugin. True by default.
     */
    boolean findbugs = true

    /**
     * Enable CodeNarc plugin. Ignored if groovy plugin is not applied). True by default.
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
     * By default enables deprecation and unchecked options.
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
