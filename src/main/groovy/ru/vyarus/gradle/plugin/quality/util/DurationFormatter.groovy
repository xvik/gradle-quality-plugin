package ru.vyarus.gradle.plugin.quality.util

/**
 * Copy of gradle's internal {@link org.gradle.internal.time.TimeFormatting} class, which become internal in
 * gradle 4.2 and broke compatibility.
 * <p>
 * Used to pretty print elapsed tile in human readable form.
 *
 * @author Vyacheslav Rusakov
 * @since 21.09.2017
 */
class DurationFormatter {
    private static final long MILLIS_PER_SECOND = 1000
    private static final long MILLIS_PER_MINUTE = 60000
    private static final long MILLIS_PER_HOUR = 3600000
    private static final long MILLIS_PER_DAY = 86400000

    private DurationFormatter() {
    }

    /**
     * @param duration duration in milliseconds
     * @return human readable (short) duration
     */
    static String format(long duration) {
        if (duration == 0L) {
            return '0s'
        }

        StringBuilder result = new StringBuilder()
        long days = duration / MILLIS_PER_DAY
        duration %= MILLIS_PER_DAY
        if (days > 0L) {
            result.append(days)
            result.append('d')
        }

        long hours = duration / MILLIS_PER_HOUR
        duration %= MILLIS_PER_HOUR
        if (hours > 0L || result.length() > 0) {
            result.append(hours)
            result.append('h')
        }

        long minutes = duration / MILLIS_PER_MINUTE
        duration %= MILLIS_PER_MINUTE
        if (minutes > 0L || result.length() > 0) {
            result.append(minutes)
            result.append('m')
        }

        int secondsScale = result.length() > 0 ? 2 : 3
        result.append(BigDecimal.valueOf(duration)
                .divide(BigDecimal.valueOf(MILLIS_PER_SECOND))
                .setScale(secondsScale, 4))
        result.append('s')
        return result.toString()
    }
}
