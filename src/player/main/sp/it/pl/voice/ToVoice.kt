package sp.it.pl.voice

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

fun LocalDate.toVoiceS(): String =
   format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH))

fun LocalTime.toVoiceS(): String =
   format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
