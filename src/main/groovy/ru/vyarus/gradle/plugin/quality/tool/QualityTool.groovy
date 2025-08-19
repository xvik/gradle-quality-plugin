package ru.vyarus.gradle.plugin.quality.tool

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.vyarus.gradle.plugin.quality.QualityExtension
import ru.vyarus.gradle.plugin.quality.report.Reporter
import ru.vyarus.gradle.plugin.quality.service.ConfigsService

/**
 * Represents quality tool support. Tools declare target language (e.g. target java only or groovy only or both)
 * and the plugin would use only compatible tools with the current project (languages detected by existing sources).
 * <p>
 * Tools with custom console reporting, register custom {@link Reporter}. Reporter instance would also be used under
 * configuration cache (because console reporting must be also performed in this case).
 * <p>
 * When additional data must be loaded from fs or tool jar, use
 * {@link ToolContext#storeReporterData(java.lang.String, java.util.concurrent.Callable)}. The storage will be
 * updated just once. The map will be cached inside tasks listener service (so service could recover it's state under
 * configuration cache).
 *
 * @author Vyacheslav Rusakov
 * @since 15.08.2025
 */
@CompileStatic
interface QualityTool {

    /**
     * @return tool name
     */
    String getToolName()

    /**
     * All tools are always active in case if user manually apply plugin (it must be configured). This list of
     * languages only affects plugin auto registration (which detected language should lead to automatic plugin
     * enabling).
     *
     * @return detected sources to auto activate tool plugin
     */
    List<ProjectSources> getAutoEnableForSources()

    /**
     * Specify required config files. Plugin would decide if actual copying in required (user may provide manual file).
     * <p>
     * IMPORTANT: only files not present in user dir will be copied. Config selector looks first in this tmp dir and
     * only after falls back to user config. This is important for cases like spotbugs exclude filter when
     * user provided xml file could be updated dynamically.
     *
     * @return config file paths
     */
    List<String> getConfigs()

    /**
     * Tool enabled state and used version. Useful for automatically enabled tools.
     * Used for qualityToolVersions task output.
     * <p>
     * Tool should only return info when it's relevant.
     *
     * @param project project
     * @param extension extension
     * @param langs detected source languages in project
     * @return tool info or null if nothing to report
     */
    String getToolInfo(Project project, QualityExtension extension, List<ProjectSources> langs)

    /**
     * Create console reporter for task, if required. When reporter is used, main configuration must call
     * {@link ToolContext#registerTaskForReport(org.gradle.api.Task,
     *ru.vyarus.gradle.plugin.quality.report.model.TaskDesc)} in order store task description (and preserve it
     * inside listener service), This is important because UP_TO_DATE tasks are not called and so the only way
     * to perform custom output is service task listener, which provide only task path (which is not enough
     * for console reporting).
     *
     * @param param stored additional reporting data
     * @param configs configs service
     * @return reporter instance or nul if custom reporting is not required.
     */
    @SuppressWarnings('UnusedMethodParameter')
    default Reporter createReporter(Object param, Provider<ConfigsService> configs) {
        // null means custom reporter not used by tool
        return null
    }

    /**
     * Perform custom tool configurations.
     * <p>
     * All tools are always configured. Tool language only affects tools auto registration, but
     * plugins might be enabled manually and in that case it must be also configured.
     *
     * @param context plugin context
     * @param register auto plugin registration (when related sources detected in project)
     */
    void configure(ToolContext context, boolean register)
}
