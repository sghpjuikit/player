package sp.it.util.file.json

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.BigInteger
import java.util.ArrayDeque
import java.util.Deque
import java.util.LinkedList
import java.util.NavigableSet
import java.util.PriorityQueue
import java.util.Queue
import java.util.SortedSet
import java.util.Stack
import java.util.TreeSet
import java.util.Vector
import kotlin.reflect.KClass
import kotlin.reflect.KType
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.getAny
import sp.it.util.functional.net
import sp.it.util.type.kType
import sp.it.util.type.raw
import sp.it.util.type.type

@Suppress("RemoveExplicitTypeArguments")
class JsonTest: FreeSpec({
   val j = Json()
   val nf = "\n"

   "Equals" {
      // @formatter:off
                            JsNull shouldBe JsNull
                            JsTrue shouldBe JsTrue
                           JsFalse shouldBe JsFalse
                  JsString("text") shouldBe JsString("text")
               JsNumber(123456789) shouldBe JsNumber(123456789)
                         JsArray() shouldBe JsArray()
              JsArray(JsNumber(1)) shouldBe JsArray(JsNumber(1))
                        JsObject() shouldBe JsObject()
      JsObject("a" to JsNumber(1)) shouldBe JsObject("a" to JsNumber(1))
      // @formatter:on
   }
   "Read (raw)" - {
      "ast" {
         j.ast("") shouldBeTry Error("Missing json AST node")
         j.ast(" ") shouldBeTry Error("Missing json AST node")
         j.ast("text") shouldBeTry Error("Unrecognized token 'text': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n at [Source: (String)\"text\"; line: 1, column: 5]")
         j.ast("null") shouldBe Ok(JsNull)
         j.ast("""""""") shouldBe Ok(JsString(""))
         j.ast("""" """") shouldBe Ok(JsString(" "))
         j.ast(""""null"""") shouldBe Ok(JsString("null"))
         j.ast(""""text"""") shouldBe Ok(JsString("text"))
         j.ast("123456789") shouldBe Ok(JsNumber(123456789))
         j.ast("[1, 2, 3]") shouldBe Ok(JsArray(JsNumber(1), JsNumber(2), JsNumber(3)))
         j.ast("[1,\n 2, 3\n]") shouldBe Ok(JsArray(JsNumber(1), JsNumber(2), JsNumber(3)))
         j.ast("""{"name": "John", "age": 42}""") shouldBe Ok(JsObject("name" to JsString("John"), "age" to JsNumber(42)))
         j.ast("""{$nf"name":$nf "John", "age": 42}""") shouldBe Ok(JsObject("name" to JsString("John"), "age" to JsNumber(42)))
      }
      "simple json" {
         j.fromJson<String>("\"\"") shouldBe Ok("")
         j.fromJson<UByte>("55") shouldBe Ok(55L.toUByte())
         j.fromJson<Int>("55") shouldBe Ok(55)
         j.fromJson<Double>("55") shouldBe Ok(55.0)
         j.fromJson<Any?>("null") shouldBe Ok(null)
         j.ast("1 2") should { it.isError  }
         j.ast("1 2 3 4") should { it.isError  }
      }
   }
   "Read" - {
      "JsNull" - {
         "should read null" {
            class Data

            val json = JsNull

            j.fromJsonValue<Any>(json) shouldBeTry Error("null is not kotlin.Any")
            j.fromJsonValue<Any?>(json) shouldBe Ok(null)
            j.fromJsonValue<String>(json) shouldBeTry Error("null is not kotlin.String")
            j.fromJsonValue<String?>(json) shouldBe Ok(null)
            j.fromJsonValue<Unit>(json) shouldBeTry Error("null is not kotlin.Unit")
            j.fromJsonValue<Unit?>(json) shouldBe Ok(null)
            j.fromJsonValue<Int>(json) shouldBeTry Error("null is not kotlin.Int")
            j.fromJsonValue<Int?>(json) shouldBe Ok(null)
            j.fromJsonValue<UInt>(json) shouldBeTry Error("null is not kotlin.UInt")
            j.fromJsonValue<UInt?>(json) shouldBe Ok(null)
            j.fromJsonValue<List<*>>(json) shouldBeTry Error("null is not kotlin.collections.List<*>")
            j.fromJsonValue<List<*>?>(json) shouldBe Ok(null)
            j.fromJsonValue<Data>(json) shouldBeTry Error("null is not sp.it.util.file.json.`JsonTest\$1\$3\$1\$1\$Data`")
            j.fromJsonValue<Data?>(json) shouldBe Ok(null)
         }
      }
      "JsNumber" - {
         "to Any should preserve type" {
            j.fromJsonValue<Any>(JsNumber(1)).orThrow.shouldBeInstance<Int>()
            j.fromJsonValue<Any>(JsNumber(1L)).orThrow.shouldBeInstance<Long>()
            j.fromJsonValue<Any>(JsNumber(1.0)).orThrow.shouldBeInstance<Double>()
            j.fromJsonValue<Any>(JsNumber(BigDecimal.valueOf(1.0))).orThrow.shouldBeInstance<BigDecimal>()
         }
         "to Number should respect type" {
            val json = JsNumber(1)

            forAll(
               arg<Byte>(),
               arg<UByte>(),
               arg<Short>(),
               arg<UShort>(),
               arg<Int>(),
               arg<UInt>(),
               arg<Long>(),
               arg<ULong>(),
               arg<Float>(),
               arg<Double>(),
               arg<BigInteger>(),
               arg<BigDecimal>()
            ) {
               j.fromJsonValue(it, json).orThrow shouldBeInstance it
            }
         }
         "to non Number should fail" {
            class Data

            val json = JsNumber(1)
            j.fromJsonValue<Data>(json).isOk shouldBe false
         }
      }
      "JsArray" - {
         "of nulls" {
            class Data

            val json = JsArray(listOf(JsNull, JsNull))

            j.fromJsonValue<Any>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<Any?>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<Array<*>>(json) shouldBeTry Ok(arrayOf<Any?>(null, null))
            j.fromJsonValue<Array<Any>>(json) shouldBeTry Error("null is not kotlin.Any")
            j.fromJsonValue<Array<Any?>>(json) shouldBeTry Ok(arrayOf<Any?>(null, null))
            j.fromJsonValue<Array<Data>>(json) shouldBeTry Error("null is not sp.it.util.file.json.`JsonTest\$1\$3\$3\$1\$Data`")
            j.fromJsonValue<Array<Data?>>(json) shouldBeTry Ok(arrayOf<Data?>(null, null))
            j.fromJsonValue<List<*>>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<List<Any>>(json) shouldBeTry Error("null is not kotlin.Any")
            j.fromJsonValue<List<Any?>>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<List<Data>>(json) shouldBeTry Error("null is not sp.it.util.file.json.`JsonTest\$1\$3\$3\$1\$Data`")
            j.fromJsonValue<List<Data?>>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<ByteArray>(json) shouldBeTry Error("null is not kotlin.Byte")
            j.fromJsonValue<CharArray>(json) shouldBeTry Error("null is not kotlin.Char")
            j.fromJsonValue<ShortArray>(json) shouldBeTry Error("null is not kotlin.Short")
            j.fromJsonValue<IntArray>(json) shouldBeTry Error("null is not kotlin.Int")
            j.fromJsonValue<LongArray>(json) shouldBeTry Error("null is not kotlin.Long")
            j.fromJsonValue<FloatArray>(json) shouldBeTry Error("null is not kotlin.Float")
            j.fromJsonValue<DoubleArray>(json) shouldBeTry Error("null is not kotlin.Double")
            j.fromJsonValue<BooleanArray>(json) shouldBeTry Error("null is not kotlin.Boolean")
         }
         "to exact collection item type" {
            val json = JsArray(listOf(JsNumber(1), JsNumber(1)))
            j.fromJsonValue<Array<Byte>>(json).orThrow shouldBe Array(2) { 1.toByte() }
            j.fromJsonValue<ByteArray>(json).orThrow shouldBe Array(2) { 1.toByte() }
            j.fromJsonValue<Array<Short>>(json).orThrow shouldBe Array(2) { 1.toShort() }
            j.fromJsonValue<ShortArray>(json).orThrow shouldBe ShortArray(2) { 1.toShort() }
            j.fromJsonValue<Array<Int>>(json).orThrow shouldBe Array(2) { 1 }
            j.fromJsonValue<IntArray>(json).orThrow shouldBe IntArray(2) { 1 }
            j.fromJsonValue<Array<Long>>(json).orThrow shouldBe Array(2) { 1L }
            j.fromJsonValue<LongArray>(json).orThrow shouldBe LongArray(2) { 1L }
            j.fromJsonValue<Array<Float>>(json).orThrow shouldBe Array(2) { 1f }
            j.fromJsonValue<FloatArray>(json).orThrow shouldBe FloatArray(2) { 1f }
            j.fromJsonValue<Array<Double>>(json).orThrow shouldBe Array(2) { 1.0 }
            j.fromJsonValue<DoubleArray>(json).orThrow shouldBe DoubleArray(2) { 1.0 }
            j.fromJsonValue<Array<BigInteger>>(json).orThrow shouldBe Array(2) { BigInteger("1") }
            j.fromJsonValue<Array<BigDecimal>>(json).orThrow shouldBe Array(2) { BigDecimal("1") }
            j.fromJsonValue<List<Byte>>(json).orThrow.first() shouldBe 1.toByte()
            j.fromJsonValue<List<Short>>(json).orThrow.first() shouldBe 1.toShort()
            j.fromJsonValue<List<Int>>(json).orThrow.first() shouldBe 1
            j.fromJsonValue<List<Long>>(json).orThrow.first() shouldBe 1L
            j.fromJsonValue<List<Float>>(json).orThrow.first() shouldBe 1f
            j.fromJsonValue<List<Double>>(json).orThrow.first() shouldBe 1.0
            j.fromJsonValue<List<BigInteger>>(json).orThrow.first() shouldBe BigInteger("1")
            j.fromJsonValue<List<BigDecimal>>(json).orThrow.first() shouldBe BigDecimal("1")
         }
         "to exact collection type" {
            val json = JsArray(listOf(JsNumber(1), JsNumber(1)))

            forAll(
               arg<Array<*>>(),
               arg<Set<*>>(),
               arg<MutableSet<*>>(),
               arg<HashSet<*>>(),
               arg<SortedSet<*>>(),
               arg<NavigableSet<*>>(),
               arg<TreeSet<*>>(),
               arg<List<*>>(),
               arg<MutableList<*>>(),
               arg<LinkedList<*>>(),
               arg<ArrayList<*>>(),
               arg<Vector<*>>(),
               arg<Stack<*>>(),
               arg<Queue<*>>(),
               arg<Deque<*>>(),
               arg<ArrayDeque<*>>(),
               arg<PriorityQueue<*>>()
            ) {
               j.fromJsonValue(it, json).orThrow shouldBeInstance it
            }
         }
      }
      "inline" {
         val json = JsNumber(1)
         j.fromJsonValue<Int>(json).orThrow shouldBeInstance Int::class
         j.fromJsonValue<Int>(json).orThrow shouldBe 1
         j.fromJsonValue<InlineInt>(json).orThrow shouldBeInstance InlineInt::class
         j.fromJsonValue<InlineInt>(json).orThrow shouldBe InlineInt(1)
      }
   }
   "Write-read" - {
      "numbers" {
         forAll(
            argIns(Byte.MIN_VALUE),
            argIns(Byte.MAX_VALUE),
            argIns(UByte.MIN_VALUE),
            argIns(UByte.MAX_VALUE),
            argIns(Short.MIN_VALUE),
            argIns(Short.MAX_VALUE),
            argIns(UShort.MIN_VALUE),
            argIns(UShort.MAX_VALUE),
            argIns(Int.MIN_VALUE),
            argIns(Int.MAX_VALUE),
            argIns(UInt.MIN_VALUE),
            argIns(UInt.MAX_VALUE),
            argIns(Long.MIN_VALUE),
            argIns(Long.MAX_VALUE),
            argIns(ULong.MIN_VALUE),
            argIns(ULong.MAX_VALUE),
            argIns(Float.MIN_VALUE),
            argIns(Float.MAX_VALUE),
            argIns(Double.MIN_VALUE),
            argIns(Double.MAX_VALUE),
            argIns(BigInteger.ONE),
            argIns(BigDecimal("1.0")),
         ) { type, valueIn ->

            // exact type -> exact type
            val valueOut1 = j.toJsonValue(type, valueIn).net { j.fromJsonValue(type, it) }.orThrow
            valueIn shouldBe valueOut1
            valueIn::class shouldBe valueOut1::class

            val valueOut2 = j.toJsonValue(type, valueIn).toCompactS().net { j.fromJson(type, it) }.orThrow
            valueIn shouldBe valueOut2
            valueIn::class shouldBe valueOut2::class

            // unknown type -> unknown type
            val valueOut3 = j.toJsonValue(type<Any>(), valueIn).net { j.fromJsonValue(type<Any>(), it) }.orThrow
            valueIn shouldBe valueOut3
            valueIn::class shouldBe valueOut3::class

            val valueOut4 = j.toJsonValue(type<Any>(), valueIn).toCompactS().net { j.fromJson(type<Any>(), it) }.orThrow
            valueIn shouldBe valueOut4
            valueIn::class shouldBe valueOut4::class

            // unknown type -> exact type
            val valueOut5 = j.toJsonValue(type<Any>(), valueIn).net { j.fromJsonValue(type, it) }.orThrow
            valueIn shouldBe valueOut5
            valueIn::class shouldBe valueOut5::class

            val valueOut6 = j.toJsonValue(type<Any>(), valueIn).toCompactS().net { j.fromJson(type, it) }.orThrow
            valueIn shouldBe valueOut6
            valueIn::class shouldBe valueOut6::class

            // exact type -> unknown type (this case loses exact type because 1 numbers do not know their type and 2 due to unsigned types being converted to a number)
            val valueOut7 = j.toJsonValue(type, valueIn).net { j.fromJsonValue(type<Any>(), it) }.orThrow
            valueIn.toString().toDouble() shouldBe valueOut7.toString().toDouble()
            valueOut7 shouldBeInstance Number::class

            val valueOut8 = j.toJsonValue(type, valueIn).toCompactS().net { j.fromJson(type<Any>(), it) }.orThrow
            valueIn.toString().toDouble() shouldBe valueOut8.toString().toDouble()
            valueOut8 shouldBeInstance Number::class
         }
      }
   }
   "inline" {
      val simple = 1
      val complex = Complex(1.0, 1.0)
      val iSimple = InlineInt(simple)
      val iComplex = InlineComplex(complex)

      // format
      j.toJsonValue(iSimple).toCompactS() shouldBe "1"
      j.toJsonValue<Any>(iSimple).toCompactS() shouldBe """{"_type":"sp.it.util.file.json.InlineInt","value":1}"""
      j.toJsonValue(iComplex).toCompactS() shouldBe """{"x":1.0,"y":1.0}"""
      j.toJsonValue<Any>(iComplex).toCompactS() shouldBe """{"_type":"sp.it.util.file.json.InlineComplex","value":{"x":1.0,"y":1.0}}"""
      // simple value class
      j.toJsonValue<Any>(iSimple).net { j.fromJsonValue<InlineInt>(it) }.orThrow shouldBe iSimple
      j.toJsonValue<Any>(iSimple).net { j.fromJsonValue<Any>(it) }.orThrow shouldBe iSimple
      j.toJsonValue(iSimple).net { j.fromJsonValue<InlineInt>(it) }.orThrow shouldBe iSimple
      j.toJsonValue(iSimple).net { j.fromJsonValue<Any>(it) }.orThrow shouldBe simple   // expected type loss to json representation
      // complex value class
      j.toJsonValue<Any>(iComplex).net { j.fromJsonValue<InlineComplex>(it) }.orThrow shouldBe iComplex
      j.toJsonValue<Any>(iComplex).net { j.fromJsonValue<Any>(it) }.orThrow shouldBe iComplex
      j.toJsonValue(iComplex).net { j.fromJsonValue<InlineComplex>(it) }.orThrow shouldBe iComplex
      j.toJsonValue(iComplex).net { j.fromJsonValue<Any>(it) }.orThrow shouldBe mapOf("x" to 1.0, "y" to 1.0)   // expected type loss to json representation
   }
   "generic types" - {
      val gDat= GenDat(1)
      val gCls= GenCls(2)
      val g = Gen(GenDat(1), GenCls(2))

      "infer from type call-site argument" - {
         "write" - {
            "normal class" {
               j.toJsonValue             (gCls).toCompactS() shouldBe """{"value":2}"""
               j.toJsonValue<GenCls<Int>>(gCls).toCompactS() shouldBe """{"value":2}"""
               j.toJsonValue<GenCls<Any>>(gCls).toCompactS() shouldBe """{"value":{"_type":"int","value":2}}"""
               j.toJsonValue<GenCls< * >>(gCls).toCompactS() shouldBe """{"value":{"_type":"int","value":2}}"""
            }
            "data class" {
               j.toJsonValue             (gDat).toCompactS() shouldBe """{"value":1}"""
               j.toJsonValue<GenDat<Int>>(gDat).toCompactS() shouldBe """{"value":1}"""
               j.toJsonValue<GenDat<Any>>(gDat).toCompactS() shouldBe """{"value":{"_type":"int","value":1}}"""
               j.toJsonValue<GenDat< * >>(gDat).toCompactS() shouldBe """{"value":{"_type":"int","value":1}}"""
            }
         }
         "write-read" - {
            "normal class" {
               j.toJsonValue             (gDat).net { j.fromJsonValue<GenDat<Int>>(it) }.orThrow shouldBe gDat
               j.toJsonValue<GenDat<Int>>(gDat).net { j.fromJsonValue<GenDat<Int>>(it) }.orThrow shouldBe gDat
               j.toJsonValue<GenDat<Any>>(gDat).net { j.fromJsonValue<GenDat<Any>>(it) }.orThrow shouldBe gDat
               j.toJsonValue<GenDat< * >>(gDat).net { j.fromJsonValue<GenDat< * >>(it) }.orThrow shouldBe gDat
            }
            "data class" {
               j.toJsonValue             (gCls).net { j.fromJsonValue<GenCls<Int>>(it) }.orThrow shouldBe gCls
               j.toJsonValue<GenCls<Int>>(gCls).net { j.fromJsonValue<GenCls<Int>>(it) }.orThrow shouldBe gCls
               j.toJsonValue<GenCls<Any>>(gCls).net { j.fromJsonValue<GenCls<Any>>(it) }.orThrow shouldBe gCls
               j.toJsonValue<GenCls< * >>(gCls).net { j.fromJsonValue<GenCls< * >>(it) }.orThrow shouldBe gCls
            }
         }
      }
      "infer from declaration argument" - {
         "write" {
            j.toJsonValue(g).toCompactS() shouldBe """{"genCls":{"value":2},"genDat":{"value":1}}"""
            j.toJsonValue(g).toCompactS() shouldBe """{"genCls":{"value":2},"genDat":{"value":1}}"""
         }
         "write-read" {
            j.toJsonValue<Any>(g).net { j.fromJsonValue<Any>(it) }.orThrow shouldBe g
         }
      }
   }
})

private data class Gen          (val genDat: GenDat<Int>, val genCls: GenCls<Int>)
private data class GenDat<out T>(val value: T)
private      class GenCls<out T>(val value: T) {
   override fun equals(other: Any?) = other is GenCls<*> && other.value == value
   override fun hashCode() = value?.hashCode() ?: 0
}

            data class Complex(val x: Double, val y: Double)
@JvmInline value class InlineInt(val value: Int)
@JvmInline value class InlineComplex(val value: Complex)

private infix  fun Try<*,Throwable>.shouldBeTry(o: Try<*,String>) { this::class shouldBe o::class; mapError { it.message }.getAny() shouldBe o.getAny() }
private inline fun <reified T: Any> Any?.shouldBeInstance() = T::class.isInstance(this) shouldBe true
private infix  fun                  Any?.shouldBeInstance(type: KClass<*>) = type.isInstance(this) shouldBe true
private infix  fun                  Any?.shouldBeInstance(type: KType) = type.raw.isInstance(this) shouldBe true

private inline fun <reified T: Any> arg()          = row(kType<T>())
private inline fun <reified T: Any> argIns(arg: T) = row(type<T>(), arg)