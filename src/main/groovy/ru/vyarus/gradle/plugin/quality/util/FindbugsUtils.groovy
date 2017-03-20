package ru.vyarus.gradle.plugin.quality.util

import groovy.xml.XmlUtil
import org.slf4j.Logger

/**
 * Findbugs helper utils.
 *
 * @author Vyacheslav Rusakov
 * @since 17.03.2017
 */
class FindbugsUtils {

    /**
     * Findbugs task is a {@link org.gradle.api.tasks.SourceTask}, but does not properly support exclusions.
     * To overcome this limitation, source exclusions could be added to findbugs exclusions xml file.
     * This also requires conversion of blobs into regex.
     *
     * @param excludes exclude glob patterns
     * @param src original excludes file (default of user defined)
     * @param logger gradle logger for debug info
     * @return extended file (temporary)
     */
    @SuppressWarnings('FileCreateTempFile')
    static File mergeExcludes(Collection<String> excludes, File src, Logger logger) {
        Node xml = new XmlParser().parse(src)

        excludes.each {
            Node match = xml.appendNode('Match').appendNode('Source')
            String regex = GlobUtils.toRegex(it)
            logger.info("[Findbugs] Use source exclude pattern instead of glob '$it': $regex")
            match.attributes().put('name', "~$regex")
        }

        File tmp = File.createTempFile('findbugs-extended-exclude', '.xml')
        tmp.deleteOnExit()
        tmp.withWriter { XmlUtil.serialize(xml, it) }
        return tmp
    }
}
