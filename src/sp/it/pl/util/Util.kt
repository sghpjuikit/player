package sp.it.pl.util

import java.io.PrintWriter
import java.io.StringWriter
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** System default zone id.  */
val ZONE_ID = ZoneId.systemDefault()

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