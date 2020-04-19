package sp.it.util.file.json

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import sp.it.util.type.jType
import sp.it.util.type.toRaw
import java.lang.reflect.Type
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

class JsonTest: FreeSpec({
   val j = Json()

   "Reading" - {
      "JsNull" - {
         "should read null" {
            class Data

            val json = JsNull

            j.fromJsonValue<Any>(json).orThrow shouldBe null
            j.fromJsonValue<Any?>(json).orThrow shouldBe null
            j.fromJsonValue<String>(json).orThrow shouldBe null
            j.fromJsonValue<String?>(json).orThrow shouldBe null
            j.fromJsonValue<Int>(json).orThrow shouldBe null
            j.fromJsonValue<Int?>(json).orThrow shouldBe null
            j.fromJsonValue<List<*>>(json).orThrow shouldBe null
            j.fromJsonValue<List<*>?>(json).orThrow shouldBe null
            j.fromJsonValue<Data?>(json).orThrow shouldBe null
            j.fromJsonValue<Data?>(json).orThrow shouldBe null
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
               arg<Short>(),
               arg<Int>(),
               arg<Long>(),
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
            j.fromJsonValue<List<*>>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<List<Any>>(json).orThrow shouldBe listOf(null, null)
            j.fromJsonValue<List<Data>>(json).orThrow shouldBe listOf(null, null)
         }
         "to exact collection item number type" {
            val json = JsArray(listOf(JsNumber(1)))

            j.fromJsonValue<List<Byte>>(json).orThrow!!.first().shouldBeInstance<Byte>()
            j.fromJsonValue<List<Int>>(json).orThrow!!.first().shouldBeInstance<Int>()
            j.fromJsonValue<List<Double>>(json).orThrow!!.first().shouldBeInstance<Double>()
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
})

private inline fun <reified T: Any> Any?.shouldBeInstance() = T::class.isInstance(this) shouldBe true
private infix fun Any?.shouldBeInstance(type: Type) = type.toRaw().isInstance(this) shouldBe true
private inline fun <reified T: Any> arg() = row(jType<T>())