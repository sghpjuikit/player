package sp.it.util.units

import javafx.util.Duration as Dur
import java.time.Duration
import sp.it.util.dev.Dependency
import sp.it.util.dev.failIf
import sp.it.util.functional.Try
import sp.it.util.functional.Util
import kotlin.time.Duration as DurationKt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit.NANOSECONDS
import sp.it.util.functional.runTry

/** Converts to javafx [Dur] */
val Duration.javafx: Dur get() = toMillis().millis

/** Converts to javafx [Dur] */
val DurationKt.javafx: Dur get() = toDouble(NANOSECONDS).millis

/** Converts to [Duration] */
val Dur.java: Duration get() = Duration.ofMillis(toMillis().toLong())

/** Converts to [DurationKt] */
val Dur.kt: DurationKt get() = toMillis().milliseconds

/** @return human-readable duration value in the smallest possible units (millis if less than second, etc.)*/
fun Dur.formatToSmallestUnit(): String {
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

/**
 * Returns string representation of the duration in the format h:m:s - 00:00:00.
 * If padZero is true, any single digit value is padded with '0'.
 * Leading units that are 0 are left out.
 *
 * Examples:
 * * 1:00:06
 * * 4:05
 * * 34
 * * 5
 * @return formatted duration
 */
@Dependency("sp.it.util.units.durationOfHMSMs")
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
         hours>99 -> String.format("%d:%2d:%2d", hours, minutes, seconds)
         hours>0 -> String.format("%2d:%02d:%02d", hours, minutes, seconds)
         minutes>9 -> String.format("%2d:%02d", minutes, seconds)
         minutes>0 -> String.format("%1d:%02d", minutes, seconds)
         else -> String.format("0:%02d", seconds)
      }
   }
}

@Dependency("sp.it.util.units.toHMSMs")
fun durationOfHMSMs(s: String): Try<Dur, String> {
   return try {

      // try parsing in Duration.toString format
      runTry {
         val sFixed = s.replace(" ", "") // fixes Duration.toString() inconsistency with Duration.valueOf()
         Dur.valueOf(sFixed)
      }.apply {
         if (this.isOk)
            return this.mapError { it.message ?: "Unknown error" }
      }

      // try parsing in hh:mm:ss format
      if (s.contains(":")) {
         val ls = Util.split(s, ":")
         var unit = 1000
         var sumT = 0.0
         for (i in ls.indices.reversed()) {
            if (i<ls.size - 1) unit *= 60
            val amount = ls[i].toInt()
            failIf(amount<0 || unit<=60000 && amount>59) { "Minutes and seconds must be >0 and <60" }
            val t = unit*amount
            sumT += t.toDouble()
         }
         return Try.Java.ok(Dur(sumT))
      }

      // parse normally
      var index = -1
      for (i in s.indices) {
         val c = s[i]
         if (!Character.isDigit(c) && c!='.' && c!='-') {
            index = i
            break
         }
      }
      val value: Double = (if (index==-1) s else s.substring(0, index)).toDouble()

      if (index==-1)
         Try.Java.ok(Dur(value))
      else {
         when (s.substring(index)) {
            "ms" -> Try.Java.ok(Dur(value))
            "s" -> Try.Java.ok(Dur(1000*value))
            "m" -> Try.Java.ok(Dur(60000*value))
            "h" -> Try.Java.ok(Dur(3600000*value))
            else -> Try.Java.error("Must have suffix from [ms|s|m|h]")
         }
      }
   } catch (e: IllegalArgumentException) {
      Try.Java.error(e.message ?: "Unknown error")
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
operator fun Dur.minus(d: Dur): Dur = subtract(d)

/** @return [Dur.plus]. */
operator fun Dur.plus(d: Dur): Dur = add(d)

/** @return duration specified times shorter than this duration. */
operator fun Dur.div(times: Number) = Dur(toMillis()/times.toDouble())

/** @return duration specified times longer than this duration. */
operator fun Dur.times(times: Number) = Dur(toMillis()*times.toDouble())

/** @return how many times is this duration longer than the specified duration, i.e., division of duration's millis */
infix fun Dur.divMillis(d: Dur) = toMillis()/d.toMillis()

/** @return multiplication of duration's millis */
infix fun Dur.timesMillis(d: Dur) = toMillis()*d.toMillis()
