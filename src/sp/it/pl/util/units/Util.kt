package sp.it.pl.util.units

import javafx.util.Duration
import sp.it.pl.util.dev.Dependency
import java.util.UUID

/** Equivalent to [UUID.fromString]. */
fun uuid(text: String) = UUID.fromString(text)

/** Converts to javafx [Duration] */
val java.time.Duration.javafx: Duration get() = toMillis().millis

/**
 * Prints out the value of Duration - string representation of the duration in the format h:m:s - 00:00:00.
 * If padZero is true, any single digit value is padded with '0'.
 * Leading units that are 0 are left out.
 * Example:
 * 1:00:06
 * 4:45
 * 34
 *
 * @return formatted duration
 */
@Dependency("sp.it.pl.util.units.Util.durationOfHMSMs")
@JvmOverloads
fun Duration.toHMSMs(include_zeros: Boolean = true): String {
    val secondsTotal = toMillis()/1000
    val seconds = secondsTotal.toInt()%60
    val minutes = ((secondsTotal-seconds)/60).toInt()%60
    val hours = (secondsTotal-seconds.toDouble()-(60*minutes).toDouble()).toInt()/3600
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
val Number.millis: Duration get() = Duration.millis(toDouble())!!
/** Returns duration with this number of seconds. */
val Number.seconds: Duration get() = Duration.seconds(toDouble())!!
/** Returns duration with this number of minutes. */
val Number.minutes: Duration get() = Duration.minutes(toDouble())!!
/** Returns duration with this number of hours. */
val Number.hours: Duration get() = Duration.hours(toDouble())!!

/** @return [Duration.minus]. */
operator fun Duration.minus(d: Duration) = subtract(d)!!
/** @return [Duration.plus]. */
operator fun Duration.plus(d: Duration) = add(d)!!

/** @return duration specified times shorter than this duration. */
operator fun Duration.div(times: Number) = Duration(toMillis()/times.toDouble())
/** @return duration specified times longer than this duration. */
operator fun Duration.times(times: Number) = Duration(toMillis()*times.toDouble())

/** @return how many times is this duration longer than the specified duration, i.e., division of duration's millis */
infix fun Duration.divMillis(d: Duration) = toMillis()/d.toMillis()
/** @return multiplication of duration's millis */
infix fun Duration.timesMillis(d: Duration) = toMillis()*d.toMillis()