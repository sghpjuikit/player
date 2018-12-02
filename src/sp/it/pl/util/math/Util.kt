@file:JvmName("Util")
@file:Suppress("unused")

package sp.it.pl.util.math

import javafx.util.Duration

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

val Number.millis: Duration get() = Duration.millis(toDouble())!!
val Number.seconds: Duration get() = Duration.seconds(toDouble())!!
val Number.minutes: Duration get() = Duration.minutes(toDouble())!!
val Number.hours: Duration get() = Duration.hours(toDouble())!!

operator fun Duration.minus(d: Duration) = subtract(d)!!
operator fun Duration.plus(d: Duration) = add(d)!!
operator fun Duration.div(d: Number) = Duration(toMillis()/d.toDouble())
operator fun Duration.times(d: Number) = Duration(toMillis()*d.toDouble())

infix fun Duration.divMillis(d: Duration) = toMillis()/d.toMillis()
infix fun Duration.timesMillis(d: Duration) = toMillis()*d.toMillis()