package sp.it.util.math

import ch.obermuhlner.math.big.BigDecimalMath
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue

/** Range of minimum to maximum value */
val Byte.Companion.range: ClosedRange<Byte> get() = object: ClosedRange<Byte> {
   override val start = MIN_VALUE
   override val endInclusive = MAX_VALUE
}

/** Range of minimum to maximum value */
val UByte.Companion.range: ClosedRange<UByte> get() = object: ClosedRange<UByte> {
   override val start = MIN_VALUE
   override val endInclusive = MAX_VALUE
}

/** Range of minimum to maximum value */
val Short.Companion.range: ClosedRange<Short> get() = object: ClosedRange<Short> {
   override val start = MIN_VALUE
   override val endInclusive = MAX_VALUE
}

/** Range of minimum to maximum value */
val UShort.Companion.range: ClosedRange<UShort> get() = object: ClosedRange<UShort> {
   override val start = MIN_VALUE
   override val endInclusive = MAX_VALUE
}

/** Range of minimum to maximum value */
val Int.Companion.range get() = MIN_VALUE..MAX_VALUE

/** Range of minimum to maximum value */
val UInt.Companion.range get() = MIN_VALUE..MAX_VALUE

/** Range of minimum to maximum value */
val Long.Companion.range get() = MIN_VALUE..MAX_VALUE

/** Range of minimum to maximum value */
val ULong.Companion.range get() = MIN_VALUE..MAX_VALUE

/** Range of minimum to maximum value */
val Float.Companion.range get() = (-MAX_VALUE)..MAX_VALUE

/** Range of minimum to maximum value */
val Double.Companion.range get() = (-MAX_VALUE)..MAX_VALUE


/** Range of minimum to maximum value as [BigDecimal]*/
val Byte.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val UByte.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val Short.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val UShort.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val Int.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val UInt.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val Long.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()

/** Range of minimum to maximum value as [BigDecimal]*/
val ULong.Companion.rangeBigInt get() = MIN_VALUE.toBigInt()..MAX_VALUE.toBigInt()


/** Range of minimum to maximum value as [BigDecimal]*/
val Byte.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val UByte.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val Short.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val UShort.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val Int.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val UInt.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val Long.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val ULong.Companion.rangeBigDec get() = MIN_VALUE.toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val Float.Companion.rangeBigDec get() = (-MAX_VALUE).toBigDec()..MAX_VALUE.toBigDec()

/** Range of minimum to maximum value as [BigDecimal]*/
val Double.Companion.rangeBigDec get() = (-MAX_VALUE).toBigDec()..MAX_VALUE.toBigDec()


fun Byte.toBigInt(): BigInteger = BigInteger.valueOf(toLong())

fun UByte.toBigInt(): BigInteger = BigInteger.valueOf(toLong())

fun Short.toBigInt(): BigInteger = BigInteger.valueOf(toLong())

fun UShort.toBigInt(): BigInteger = BigInteger.valueOf(toLong())

fun Int.toBigInt(): BigInteger = BigInteger.valueOf(toLong())

fun UInt.toBigInt(): BigInteger = BigInteger.valueOf(toLong())

fun Long.toBigInt(): BigInteger = BigInteger.valueOf(this)

fun ULong.toBigInt(): BigInteger = BigInteger(this.toString())


fun Byte.toBigDec(): BigDecimal = BigDecimal(toInt())

fun UByte.toBigDec(): BigDecimal = BigDecimal(toInt())

fun Short.toBigDec(): BigDecimal = BigDecimal(toInt())

fun UShort.toBigDec(): BigDecimal = BigDecimal(toInt())

fun Int.toBigDec(): BigDecimal = BigDecimal(this)

fun UInt.toBigDec(): BigDecimal = BigDecimal(toLong())

fun Long.toBigDec(): BigDecimal = BigDecimal(this)

fun ULong.toBigDec(): BigDecimal = BigDecimalMath.toBigDecimal(this.toString())

fun Float.toBigDec(): BigDecimal = BigDecimalMath.toBigDecimal(toString())

fun Double.toBigDec(): BigDecimal = BigDecimal(this)

fun BigInteger.toBigDec(): BigDecimal = toBigDecimal()

fun Number.toBigDec(): BigDecimal = when (this) {
   is Byte -> this.toBigDec()
   is Short -> this.toBigDec()
   is Int -> this.toBigDec()
   is Long -> this.toBigDec()
   is Float -> this.toBigDec()
   is Double -> this.toBigDec()
   is BigDecimal -> this
   is BigInteger -> this.toBigDecimal()
   else -> BigDecimalMath.toBigDecimal(toString())
}

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