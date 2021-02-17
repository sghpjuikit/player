package sp.it.util

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** [ZoneId.systemDefault]. */
val ZONE_ID: ZoneId = ZoneId.systemDefault()

/** @return [System.identityHashCode] */
fun Any?.identityHashCode(): Int = System.identityHashCode(this)

/** @return [LocalDateTime.ofInstant] using [ZoneId.systemDefault] */
fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZONE_ID)

/** @return local date time from epoch millis or null if parameter exceeds the maximum or minimum [java.time.Instant] */
fun Long.localDateTimeFromMillis(): LocalDateTime? =
   try {
      Instant.ofEpochMilli(this).toLocalDateTime()
   } catch (e: DateTimeException) {
      null
   }