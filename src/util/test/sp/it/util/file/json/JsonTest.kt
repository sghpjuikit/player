package sp.it.util.file.json

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
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
import kotlin.reflect.KType
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.net
import sp.it.util.type.kType
import sp.it.util.type.raw
import sp.it.util.type.type

class JsonTest: FreeSpec({
   val j = Json()

   "Reading (raw)" - {
      "simple json" {
         j.fromJson<String>("\"\"") shouldBe Ok("")
         j.fromJson<UByte>("55") shouldBe Ok(55L.toUByte())
         j.fromJson<Int>("55") shouldBe Ok(55)
         j.fromJson<Double>("55") shouldBe Ok(55.0)
         j.fromJson<Any?>("null") shouldBe Ok(null)
      }
   }
   "Reading" - {
      "JsNull" - {
         "should read null" {
            class Data

            val json = JsNull

            j.fromJsonValue<Any>(json).errorOrThrow.message shouldBe "null is not kotlin.Any"
            j.fromJsonValue<Any?>(json) shouldBe Ok(null)
            j.fromJsonValue<String>(json).errorOrThrow.message shouldBe "null is not kotlin.String"
            j.fromJsonValue<String?>(json) shouldBe Ok(null)
            j.fromJsonValue<Unit>(json).errorOrThrow.message shouldBe "null is not kotlin.Unit"
            j.fromJsonValue<Unit?>(json) shouldBe Ok(null)
            j.fromJsonValue<Int>(json).errorOrThrow.message shouldBe "null is not kotlin.Int"
            j.fromJsonValue<Int?>(json) shouldBe Ok(null)
            j.fromJsonValue<UInt>(json).errorOrThrow.message shouldBe "null is not kotlin.UInt"
            j.fromJsonValue<UInt?>(json) shouldBe Ok(null)
            j.fromJsonValue<List<*>>(json).errorOrThrow.message shouldBe "null is not kotlin.collections.List<*>"
            j.fromJsonValue<List<*>?>(json) shouldBe Ok(null)
            j.fromJsonValue<Data>(json).errorOrThrow.message shouldBe "null is not sp.it.util.file.json.`JsonTest\$1\$2\$1\$1\$Data`"
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

            j.fromJsonValue<Any>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<Any?>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<Array<*>>(json).orThrow shouldBe arrayOf<Any?>(null, null)
            j.fromJsonValue<Array<Any>>(json).errorOrThrow.message shouldBe "null is not kotlin.Any"
            j.fromJsonValue<Array<Any?>>(json).orThrow shouldBe arrayOf<Any?>(null, null)
            j.fromJsonValue<Array<Data>>(json).errorOrThrow.message shouldBe "null is not sp.it.util.file.json.`JsonTest\$1\$2\$3\$1\$Data`"
            j.fromJsonValue<Array<Data?>>(json).orThrow shouldBe arrayOf<Data?>(null, null)
            j.fromJsonValue<List<*>>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<List<Any>>(json).errorOrThrow.message shouldBe "null is not kotlin.Any"
            j.fromJsonValue<List<Any?>>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<List<Data>>(json).errorOrThrow.message shouldBe "null is not sp.it.util.file.json.`JsonTest\$1\$2\$3\$1\$Data`"
            j.fromJsonValue<List<Data?>>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<ByteArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Byte"
            j.fromJsonValue<CharArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Char"
            j.fromJsonValue<ShortArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Short"
            j.fromJsonValue<IntArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Int"
            j.fromJsonValue<LongArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Long"
            j.fromJsonValue<FloatArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Float"
            j.fromJsonValue<DoubleArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Double"
            j.fromJsonValue<BooleanArray>(json).errorOrThrow.message shouldBe "null is not kotlin.Boolean"
         }
         "to exact collection item number type" {
            val json = JsArray(listOf(JsNumber(1)))

            j.fromJsonValue<List<Byte>>(json).orThrow.first().shouldBeInstance<Byte>()
            j.fromJsonValue<List<Int>>(json).orThrow.first().shouldBeInstance<Int>()
            j.fromJsonValue<List<Double>>(json).orThrow.first().shouldBeInstance<Double>()
         }
         "to exact collection type" {
            val json = JsArray(listOf(JsNumber(1), JsNumber(1)))

            forAll(
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

   }
   "Write-read" - {
      "!numbers" {
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
            argIns(Double.MAX_VALUE)
         ) { type, valueIn ->
            val valueOut1 = j.toJsonValue(type, valueIn).net { j.fromJsonValue(type, it) }.orThrow
            valueIn shouldBe valueOut1
            valueIn::class shouldBeSameInstanceAs valueOut1::class

            val valueOut2 = j.toJsonValue(type, valueIn).toCompactS().net { j.fromJson(type, it) }.orThrow
            valueIn shouldBe valueOut2
            valueIn::class shouldBeSameInstanceAs valueOut2::class

            val valueOut3 = j.toJsonValue(type<Any>(), valueIn).net { j.fromJsonValue(type<Any>(), it) }.orThrow
            valueIn shouldBe valueOut3
            valueIn::class shouldBeSameInstanceAs valueOut3::class

            val valueOut4 = j.toJsonValue(type<Any>(), valueIn).toCompactS().net { j.fromJson(type<Any>(), it) }.orThrow
            valueIn shouldBe valueOut4
            valueIn::class shouldBeSameInstanceAs valueOut4::class

            val valueOut5 = j.toJsonValue(type.type, valueIn).toCompactS().net { j.fromJson(type<Any>(), it) }.orThrow
            valueIn shouldBe valueOut5
            valueIn::class shouldBeSameInstanceAs valueOut5::class
         }
      }
   }
})

private inline fun <reified T: Any> Any?.shouldBeInstance() = T::class.isInstance(this) shouldBe true
private infix fun Any?.shouldBeInstance(type: KType) = type.raw.isInstance(this) shouldBe true
private inline fun <reified T: Any> arg() = row(kType<T>())
private inline fun <reified T: Any> argIns(arg: T) = row(type<T>(), arg)