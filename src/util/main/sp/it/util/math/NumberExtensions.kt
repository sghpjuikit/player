package sp.it.util.math

import kotlin.math.absoluteValue

val Byte.abs: Byte get() = if (this<0) (-this).toByte() else this

val Short.abs: Short get() = if (this<0) (-this).toShort() else this

val Int.abs: Int get() = absoluteValue

val Long.abs: Long get() = absoluteValue

val Float.abs: Float get() = absoluteValue

val Double.abs: Double get() = absoluteValue

infix fun Byte.dist(n: Byte): Byte = (n - this).absoluteValue.toByte()

infix fun Short.dist(n: Short): Short = (n - this).absoluteValue.toShort()

infix fun Int.dist(n: Int): Int = (n - this).absoluteValue

infix fun Float.dist(n: Float): Float = (n - this).absoluteValue

infix fun Long.dist(n: Long): Long = (n - this).absoluteValue

infix fun Double.dist(n: Double): Double = (n - this).absoluteValue