package sp.it.pl.util

import javafx.util.Duration
import java.io.PrintWriter
import java.io.StringWriter
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** System default zone id. */
val ZONE_ID = ZoneId.systemDefault()!!

/** @return [System.identityHashCode] */
fun Any?.identityHashCode() = System.identityHashCode(this)

/** @return local date time from this instant */
fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZONE_ID)

/** @return local date time from epoch millis or null if parameter exceeds the maximum or minimum [java.time.Instant] */
fun Long.localDateTimeFromMillis(): LocalDateTime? =
        try {
            Instant.ofEpochMilli(this).toLocalDateTime()
        } catch (e: DateTimeException) {
            null
        }

/** @return string of printed stacktrace of this throwable */
val Throwable.stacktraceAsString: String
    get() = StringWriter().also { printStackTrace(PrintWriter(it)) }.toString()

/** @return human readable duration value in smallest possible units (millis if less than second, etc.)*/
fun Duration.formatToSmallestUnit(): String {
    val ms = toMillis()
    return when {
        ms<1000 -> "%.0f ms".format(ms)
        ms<10000 -> "%.1f s".format(toSeconds())
        ms<60000 -> "%.0f s".format(toSeconds())
        ms<3600000 -> "%.1f m".format(toMinutes())
        ms<86400000 -> "%.1f h".format(toHours())
        else -> "${toHours().toInt()/24} d"
    }
}