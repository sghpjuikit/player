package sp.it.util.units

import sp.it.util.dev.Dependency
import java.time.Duration
import java.util.UUID
import javafx.util.Duration as Dur

/** Equivalent to [UUID.randomUUID]. */
fun uuid() = UUID.randomUUID()

/** Equivalent to [UUID.fromString]. */
fun uuid(text: String) = UUID.fromString(text)

/** Converts to javafx [Dur] */
val Duration.javafx: Dur get() = toMillis().millis

/**
 * Prints out the string representation of the duration in the format h:m:s - 00:00:00.
 * If padZero is true, any single digit value is padded with '0'.
 * Leading units that are 0 are left out.
 * Example:
 * 1:00:06
 * 4:45
 * 34
 *
 * @return formatted duration
 */
@Dependency("sp.it.util.units.Util.durationOfHMSMs")
@JvmOverloads
fun Dur.toHMSMs(include_zeros: Boolean = true): String {
   val secondsTotal = toMillis()/1000
   val seconds = secondsTotal.toInt()%60
   val minutes = ((secondsTotal - seconds)/60).toInt()%60
   val hours = (secondsTotal - seconds.toDouble() - (60*minutes).toDouble()).toInt()/3600
   return if (include_zeros) {
      when {
         hours>99 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
         hours>0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
         minutes>0 -> String.format("%02d:%02d", minutes, seconds)
         else -> String.format("00:%02d", seconds)
      }
   } else {
      when {
         hours>99 -> String.format("%3d:%2d:%2d", hours, minutes, seconds)
         hours>0 -> String.format("%2d:%2d:%2d", hours, minutes, seconds)
         minutes>0 -> String.format("%2d:%2d", minutes, seconds)
         else -> String.format("%2d", seconds)
      }
   }
}

/** Returns duration with this number of milliseconds. */
val Number.millis: Dur get() = Dur.millis(toDouble())
/** Returns duration with this number of seconds. */
val Number.seconds: Dur get() = Dur.seconds(toDouble())
/** Returns duration with this number of minutes. */
val Number.minutes: Dur get() = Dur.minutes(toDouble())
/** Returns duration with this number of hours. */
val Number.hours: Dur get() = Dur.hours(toDouble())

/** @return [Dur.minus]. */
operator fun Dur.minus(d: Dur) = subtract(d)

/** @return [Dur.plus]. */
operator fun Dur.plus(d: Dur) = add(d)

/** @return duration specified times shorter than this duration. */
operator fun Dur.div(times: Number) = Dur(toMillis()/times.toDouble())

/** @return duration specified times longer than this duration. */
operator fun Dur.times(times: Number) = Dur(toMillis()*times.toDouble())

/** @return how many times is this duration longer than the specified duration, i.e., division of duration's millis */
infix fun Dur.divMillis(d: Dur) = toMillis()/d.toMillis()

/** @return multiplication of duration's millis */
infix fun Dur.timesMillis(d: Dur) = toMillis()*d.toMillis()