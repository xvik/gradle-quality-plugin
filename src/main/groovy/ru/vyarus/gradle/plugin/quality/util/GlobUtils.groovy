package ru.vyarus.gradle.plugin.quality.util

import java.util.regex.PatternSyntaxException

/**
 * Glob conversion utility.
 * <p>
 * Based on <a href="
 * http://salsahpc.indiana.edu/tutorial/apps/hadoop-0.20.203.0/src/core/org/apache/hadoop/fs/GlobPattern.java">
 * apache 2 code</a>.
 *
 * @author Vyacheslav Rusakov
 * @since 17.03.2017
 */
@SuppressWarnings('CyclomaticComplexity')
class GlobUtils {

    private static final char BACKSLASH = '\\'
    private static final char DOT = '.'

    /**
     * @param glob glob to convert to regex
     * @return regex matching specified glob
     */
    @SuppressWarnings(['MethodSize', 'DuplicateStringLiteral'])
    static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder()
        int setOpen = 0
        int curlyOpen = 0
        int len = glob.length()

        if (glob.charAt(0) != '*' as char) {
            regex.append('^')
        }

        for (int i = 0; i < len; i++) {
            char c = glob.charAt(i)

            switch (c) {
                case BACKSLASH:
                    if (++i >= len) {
                        throw new PatternSyntaxException('Missing escaped character', glob, i)
                    }
                    regex.append(c).append(glob.charAt(i))
                    continue
                case '/':
                    // match both path separator variants
                    regex.append(File.separator)
                    continue
                case DOT:
                case '$':
                case '(':
                case ')':
                case '|':
                case '+':
                    // escape regex special chars that are not glob special chars
                    regex.append(BACKSLASH)
                    break
                case '*':
                    // skip double asterisk
                    if (i > 0 && glob.charAt(i - 1) == '*' as char) {
                        continue
                    }
                    regex.append(DOT)
                    break
                case '?':
                    regex.append(DOT)
                    continue
                case '{': // start of a group
                    regex.append('(?:') // non-capturing
                    curlyOpen++
                    continue
                case ',':
                    regex.append(curlyOpen > 0 ? '|' : c)
                    continue
                case '}':
                    if (curlyOpen > 0) {
                        // end of a group
                        curlyOpen--
                        regex.append(')')
                        continue
                    }
                    break
                case '[':
                    if (setOpen > 0) {
                        throw new PatternSyntaxException('Unclosed character class', glob, i)
                    }
                    setOpen++
                    break
                case '^': // ^ inside [...] can be unescaped
                    if (setOpen == 0) {
                        regex.append(BACKSLASH)
                    }
                    break
                case '!': // [! needs to be translated to [^
                    regex.append(setOpen > 0 && '[' as char == glob.charAt(i - 1) ? '^' : '!')
                    continue
                case ']':
                    // Many set errors like [][] could not be easily detected here,
                    // as []], []-] and [-] are all valid POSIX glob and java regex.
                    // We'll just let the regex compiler do the real work.
                    setOpen = 0
                    break
            }
            regex.append(c)
        }

        if (glob.charAt(glob.length() - 1) != '*' as char) {
            regex.append('$')
        }

        if (setOpen > 0) {
            throw new PatternSyntaxException('Unclosed character class', glob, len)
        }
        if (curlyOpen > 0) {
            throw new PatternSyntaxException('Unclosed group', glob, len)
        }
        regex.toString()
    }

}
