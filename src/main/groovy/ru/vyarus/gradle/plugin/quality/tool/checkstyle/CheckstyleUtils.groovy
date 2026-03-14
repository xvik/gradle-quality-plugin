package ru.vyarus.gradle.plugin.quality.tool.checkstyle

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.JavaVersion

import javax.xml.parsers.SAXParserFactory

/**
 * Checkstyle helper utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 14.03.2026
 */
@CompileStatic(TypeCheckingMode.SKIP)
class CheckstyleUtils {

    // checkstyle 13 requires java 21 - 12.3.1 the latest version compatible with java 17
    static final String CHECKSTYLE_JAVA17 = '12.3.1'
    // rules, incompatible with 12.3.1
    static final List<String> CHECKSTYLE_JAVA17_INCOMPATIBLE_RULES = [
            'MissingOverrideOnRecordAccessor',
            'NumericalPrefixesInfixesSuffixesCharacterCase',
            'LineEnding',
            'GoogleNonConstantFieldName',
            'UseEnhancedSwitch',
    ]
    // checkstyle 11 requires java 17 = 10.26.1 the latest version compatible with java 11
    static final String CHECKSTYLE_JAVA11 = '10.26.1'
    // rules, incompatible with 10.26.1
    static final List<String> CHECKSTYLE_JAVA11_INCOMPATIBLE_RULES = [
            'HexLiteralCase',
            'TextBlockGoogleStyleFormatting',]

    /**
     * Detects if configured version is compatible with current jdk. This is required for case when user manually
     * configures lower checkstyle version so plugin must not be disabled (if provided version is compatible).
     *
     * @param version version to check
     * @return true if version is compatible with current jdk
     */
    static boolean isCheckstyleCompatible(String version) {
        return isCheckstyleCompatible(version, JavaVersion.current())
    }

    static boolean isCheckstyleCompatible(String version, JavaVersion current) {
        int ver = version[0..version.indexOf('.') - 1] as int

        // checkstyle 13 require java 21
        return (ver >= 13 && current.isCompatibleWith(JavaVersion.VERSION_21))
                // checkstyle 11 - 12 require java 17
                || (ver <= 12 && current.isCompatibleWith(JavaVersion.VERSION_17))
                // checkstyle 10 requires java 11
                || (ver == 10 && current.isCompatibleWith(JavaVersion.VERSION_11))
    }

    /**
     * Method returns DEFAULT checkstyle version according to current jdk (and only if fallback enabled).
     *
     * @param fallback true if fallback enabled
     * @param version current default version
     * @return version compatible with jdk or provided version as is (if fallback disabled)
     */
    static String getCompatibleCheckstyleVersion(boolean fallback, String version) {
        return getCompatibleCheckstyleVersion(fallback, version, JavaVersion.current())
    }

    static String getCompatibleCheckstyleVersion(boolean fallback, String version, JavaVersion current) {
        if (fallback && !isCheckstyleCompatible(version, current)) {
            return current.isCompatibleWith(JavaVersion.VERSION_17) ? CHECKSTYLE_JAVA17 : CHECKSTYLE_JAVA11
        }
        return version
    }

    /**
     * Method returns incompatible rules with current checkstyle version according to current jdk
     * (and only if fallback enabled).
     * <p>
     * This is useful only for plugin testing to avoid maintaining multiple configs!
     *
     * @param fallback true if fallback enabled
     * @param version current default version
     * @return version compatible with jdk or provided version as is (if fallback disabled)
     */
    static List<String> getIncompatibleRules(boolean fallback, String version) {
        return getIncompatibleRules(fallback, version, JavaVersion.current())
    }

    static List<String> getIncompatibleRules(boolean fallback, String version, JavaVersion current) {
        if (fallback && !isCheckstyleCompatible(version, current)) {
            if (current.isCompatibleWith(JavaVersion.VERSION_17)) {
                return CHECKSTYLE_JAVA17_INCOMPATIBLE_RULES
            }
            List<String> res = []
            res.addAll(CHECKSTYLE_JAVA17_INCOMPATIBLE_RULES)
            res.addAll(CHECKSTYLE_JAVA11_INCOMPATIBLE_RULES)
            return res
        }
        return []
    }

    /**
     * Remove declared modules from checkstyle file (overwrite config xml).
     *
     * @param src config file
     * @param excluded excluded rules
     */
    @SuppressWarnings('UnnecessaryGetter')
    static void mergeExcludes(File src, List<String> excluded) {
        // checkstyle config use doctype which must be ignored
        SAXParserFactory factory = SAXParserFactory.newInstance()
        factory.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)

        // modules declared in the root module and inside TreeWalker so check two times
        Node xml = new XmlParser(factory.newSAXParser().getXMLReader()).parse(src)
        processNode(xml, excluded)

        Node walker = xml.module.find { it.@name == 'TreeWalker' }
        processNode(walker, excluded)

        Writer writer = src.newWriter(false)
        writer.writeLine('''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE module PUBLIC
        "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">''')

        Writer sw = new StringWriter()
        XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(sw))
        printer.print(xml)
        writer.write(sw.toString())

        // not direct serialization to avoid blank lines
//        XmlUtil.serialize(xml, writer)
        writer.flush()
    }

    @SuppressWarnings(['Println', 'SpaceAfterOpeningBrace', 'SpaceBeforeClosingBrace'])
    private static void processNode(Node node, List<String> excluded) {
        excluded.each { remove ->
            Node toremove = node.find { it.@name == remove }
            if (toremove) {
                println "[quality] suppressed checkstyle rule: $remove"
                toremove.replaceNode {}
            }
        }
    }
}
