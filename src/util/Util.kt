package util

import util.Util.ZONE_ID
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime

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