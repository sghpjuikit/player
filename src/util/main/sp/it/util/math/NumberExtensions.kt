package sp.it.util.math

import kotlin.math.absoluteValue

infix fun Int.distance(n: Int): Int = (n - this).absoluteValue

infix fun Float.distance(n: Float): Float = (n - this).absoluteValue

infix fun Long.distance(n: Long): Long = (n - this).absoluteValue

infix fun Double.distance(n: Double): Double = (n - this).absoluteValue