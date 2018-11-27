@file:JvmName("Util")
@file:Suppress("unused")

package sp.it.pl.util.math

import javafx.util.Duration

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

val Int.millis: Duration get() = toDouble().millis
val Long.millis: Duration get() = toDouble().millis
val Float.millis: Duration get() = toDouble().millis
val Double.millis: Duration get() = Duration.millis(this)!!
val Int.seconds: Duration get() = toDouble().seconds
val Long.seconds: Duration get() = toDouble().seconds
val Float.seconds: Duration get() = toDouble().seconds
val Double.seconds: Duration get() = Duration.seconds(this)!!
val Int.minutes: Duration get() = toDouble().minutes
val Long.minutes: Duration get() = toDouble().minutes
val Float.minutes: Duration get() = toDouble().minutes
val Double.minutes: Duration get() = Duration.minutes(this)!!
val Int.hours: Duration get() = toDouble().hours
val Long.hours: Duration get() = toDouble().hours
val Float.hours: Duration get() = toDouble().hours
val Double.hours: Duration get() = Duration.hours(this)!!

operator fun Duration.minus(d: Duration) = subtract(d)!!
operator fun Duration.minus(d: Double) = Duration(toMillis()-d)
operator fun Duration.minus(d: Float) = minus(d.toDouble())
operator fun Duration.minus(d: Int) = minus(d.toDouble())
operator fun Duration.plus(d: Duration) = add(d)!!
operator fun Duration.plus(d: Double) = Duration(toMillis()+d)
operator fun Duration.plus(d: Float) = plus(d.toDouble())
operator fun Duration.plus(d: Int) = plus(d.toDouble())
operator fun Duration.div(d: Double) = Duration(toMillis()/d)
operator fun Duration.div(d: Float) = div(d.toDouble())
operator fun Duration.div(d: Int) = div(d.toDouble())
operator fun Duration.times(d: Double) = Duration(toMillis()*d)
operator fun Duration.times(d: Float) = times(d.toDouble())
operator fun Duration.times(d: Int) = times(d.toDouble())

infix fun Duration.divMillis(d: Duration) = toMillis()/d.toMillis()
infix fun Duration.timesMillis(d: Duration) = toMillis()*d.toMillis()