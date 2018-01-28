@file:JvmName("Util")
@file:Suppress("unused")

package sp.it.pl.util.math

import javafx.util.Duration

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

fun millis(value: Int) = Duration.millis(value.toDouble())!!

fun millis(value: Double) = Duration.millis(value)!!

fun seconds(value: Int) = Duration.seconds(value.toDouble())!!

fun seconds(value: Double) = Duration.seconds(value)!!

fun minutes(value: Int) = Duration.minutes(value.toDouble())!!

fun minutes(value: Double) = Duration.minutes(value)!!

operator fun Duration.minus(d: Duration) = subtract(d)!!
operator fun Duration.minus(d: Double) = millis(toMillis()-d)
operator fun Duration.plus(d: Duration) = millis(toMillis()+d.toMillis())
operator fun Duration.plus(d: Double) = millis(toMillis()+d)
operator fun Duration.div(d: Double) = millis(toMillis()/d)
operator fun Duration.times(d: Double) = millis(toMillis()*d)
infix fun Duration.divMillis(d: Duration) = toMillis()/d.toMillis()
infix fun Duration.timesMillis(d: Duration) = toMillis()*d.toMillis()
