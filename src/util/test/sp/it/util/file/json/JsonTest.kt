package sp.it.util.file.json

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.io.File
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
import sp.it.util.dev.printIt
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.getAny
import sp.it.util.functional.net
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.toChar32
import sp.it.util.type.kType
import sp.it.util.type.raw
import sp.it.util.type.type

@Suppress("SpellCheckingInspection")
class JsonTest: FreeSpec({
   val j = Json()
   val nf = "\n"

   "Equals" {
      // @formatter:off
                                  JsNull shouldBe JsNull
                                  JsTrue shouldBe JsTrue
                                 JsFalse shouldBe JsFalse
                        JsString("text") shouldBe JsString("text")
                       JsString("日本語") shouldBe JsString("日本語")
                     JsNumber(123456789) shouldBe JsNumber(123456789)
              JsNumber(-Float.MAX_VALUE) shouldBe JsNumber(-Float.MAX_VALUE)
               JsNumber(Float.MIN_VALUE) shouldBe JsNumber(Float.MIN_VALUE)
               JsNumber(Float.MAX_VALUE) shouldBe JsNumber(Float.MAX_VALUE)
       JsNumber(Float.POSITIVE_INFINITY) shouldBe JsNumber(Float.POSITIVE_INFINITY)
       JsNumber(Float.NEGATIVE_INFINITY) shouldBe JsNumber(Float.NEGATIVE_INFINITY)
                     JsNumber(Float.NaN) shouldBe JsNumber(Float.NaN)
             JsNumber(-Double.MIN_VALUE) shouldBe JsNumber(-Double.MIN_VALUE)
              JsNumber(Double.MIN_VALUE) shouldBe JsNumber(Double.MIN_VALUE)
              JsNumber(Double.MAX_VALUE) shouldBe JsNumber(Double.MAX_VALUE)
      JsNumber(Double.POSITIVE_INFINITY) shouldBe JsNumber(Double.POSITIVE_INFINITY)
      JsNumber(Double.NEGATIVE_INFINITY) shouldBe JsNumber(Double.NEGATIVE_INFINITY)
                    JsNumber(Double.NaN) shouldBe JsNumber(Double.NaN)
                               JsArray() shouldBe JsArray()
                    JsArray(JsNumber(1)) shouldBe JsArray(JsNumber(1))
                          JsNumber(1.0f) shouldBe JsNumber(1.0f)
                           JsNumber(1.0) shouldBe JsNumber(1.0)
             JsNumber(BigDecimal("1.0")) shouldBe JsNumber(BigDecimal("1.0"))
                              JsObject() shouldBe JsObject()
            JsObject("a" to JsNumber(1)) shouldBe JsObject("a" to JsNumber(1))
      // @formatter:on
   }
   "Read (raw)" - {
      "ast" {
         j.ast("") shouldBeTry Error("Invalid token at position 0")
         j.ast(" ") shouldBeTry Error("Invalid token at position 0")
         j.ast("xxx") shouldBeTry Error("Invalid token at position 0")
         j.ast("text") shouldBeTry Error("Invalid token at position 1")
         j.ast("nulL") shouldBeTry Error("Invalid token at position 3")
         j.ast("nul") shouldBeTry Error("Invalid token at position 3")
         j.ast("null") shouldBe Ok(JsNull)
         j.ast("null ") shouldBe Ok(JsNull)
         j.ast(" null") shouldBe Ok(JsNull)
         j.ast("\nnull\n") shouldBe Ok(JsNull)
         j.ast("true") shouldBe Ok(JsTrue)
         j.ast(" true ") shouldBe Ok(JsTrue)
         j.ast("false") shouldBe Ok(JsFalse)
         j.ast(" false ") shouldBe Ok(JsFalse)
         j.ast("""""""") shouldBe Ok(JsString(""))
         j.ast("""" """") shouldBe Ok(JsString(" "))
         j.ast(""""null"""") shouldBe Ok(JsString("null"))
         j.ast(""""null """") shouldBe Ok(JsString("null "))
         j.ast("""" null"""") shouldBe Ok(JsString(" null"))
         j.ast(""""text"""") shouldBe Ok(JsString("text"))
         j.ast(""""text """") shouldBe Ok(JsString("text "))
         j.ast("""" text"""") shouldBe Ok(JsString(" text"))
         j.ast("0") shouldBe Ok(JsNumber(0))
         j.ast("1") shouldBe Ok(JsNumber(1))
         j.ast("01") shouldBeTry Error("Invalid token at position 1")
         j.ast("-43") shouldBe Ok(JsNumber(-43))
         j.ast("2.3") shouldBe Ok(JsNumber(BigDecimal("2.3")))
         j.ast("1.") shouldBeTry Error("Invalid token at position 2")
         j.ast("34E4") shouldBe Ok(JsNumber(BigDecimal("34E4")))
         j.ast("E4") shouldBeTry Error("Invalid token at position 0")
         j.ast("+E4") shouldBeTry Error("Invalid token at position 0")
         j.ast("-E4") shouldBeTry Error("Invalid token at position 1")
         j.ast("0e1") shouldBe Ok(JsNumber(BigDecimal("0E+1")))
         j.ast("22e+2") shouldBe Ok(JsNumber(BigDecimal("22e+2")))
         j.ast("22e-2") shouldBe Ok(JsNumber(BigDecimal("22e-2")))
         j.ast("431e-3") shouldBe Ok(JsNumber(BigDecimal("431e-3")))
         j.ast("4.2e+1") shouldBe Ok(JsNumber(BigDecimal("4.2e+1")))
         j.ast("1234567891234567891234567891") shouldBe Ok(JsNumber(BigInteger("1234567891234567891234567891")))
         j.ast("12345678912345678912345678.9") shouldBe Ok(JsNumber(BigDecimal("12345678912345678912345678.9")))
         j.ast("123456789") shouldBe Ok(JsNumber(123456789))
         j.ast("123456789 ") shouldBe Ok(JsNumber(123456789))
         j.ast(" 123456789") shouldBe Ok(JsNumber(123456789))
         j.ast("[]") shouldBe Ok(JsArray())
         j.ast(" [] ") shouldBe Ok(JsArray())
         j.ast("\n[\n ] ") shouldBe Ok(JsArray())
         j.ast("[1, 2, 3]") shouldBe Ok(JsArray(JsNumber(1), JsNumber(2), JsNumber(3)))
         j.ast("[1,\n 2, 3\n]") shouldBe Ok(JsArray(JsNumber(1), JsNumber(2), JsNumber(3)))
         j.ast("""{}""") shouldBe Ok(JsObject())
         j.ast(""" {} """) shouldBe Ok(JsObject())
         j.ast("""{$nf} $nf""") shouldBe Ok(JsObject())
         j.ast("""{"name": "John", "age": 42}""") shouldBe Ok(JsObject("name" to JsString("John"), "age" to JsNumber(42)))
         j.ast("""{$nf"name":$nf "John", "age": 42}""") shouldBe Ok(JsObject("name" to JsString("John"), "age" to JsNumber(42)))
         j.ast("1x2") shouldBeTry Error("Invalid token at position 1")
         j.ast("1_2") shouldBeTry Error("Invalid token at position 1")
         j.ast("1+2") shouldBeTry Error("Invalid token at position 1")
         j.ast("1-2") shouldBeTry Error("Invalid token at position 1")
         j.ast("1 2") shouldBeTry Error("Invalid token at position 1")
         j.ast("1 2 3") shouldBeTry Error("Invalid token at position 1")
         j.ast("1 2 3 4") shouldBeTry Error("Invalid token at position 1")
      }
      "simple json" {
         // @formatter:off
                 j.fromJson<String>("""""""") shouldBeTry Ok("")
                j.fromJson<String>("""" """") shouldBeTry Ok(" ")
             j.fromJson<String>(""""text"""") shouldBeTry Ok("text")

                j.fromJson<String>(""""a"""") shouldBeTry Ok("a")
                j.fromJson<String>(""""1"""") shouldBeTry Ok("1")
               j.fromJson<String>(""""\""""") shouldBeTry Ok("\"")
              j.fromJson<String>(""""\\""""") shouldBeTry Ok("\\")
              j.fromJson<String>(""""\/""""") shouldBeTry Ok("/")
              j.fromJson<String>(""""\b""""") shouldBeTry Ok("\b")
              j.fromJson<String>(""""\f""""") shouldBeTry Ok("\u000c")
               j.fromJson<String>(""""\n"""") shouldBeTry Ok("\n")
               j.fromJson<String>(""""\r"""") shouldBeTry Ok("\r")
               j.fromJson<String>(""""\t"""") shouldBeTry Ok("\t")
            j.fromJson<String>(""""日本語"""") shouldBeTry Ok("日本語")
           j.fromJson<String>(""""\u0001"""") shouldBeTry Ok("\u0001")
           j.fromJson<String>(""""\u01AF"""") shouldBeTry Ok("\u01AF")
           j.fromJson<String>(""""\u01af"""") shouldBeTry Ok("\u01af")

                  j.fromJson<Char>(""""a"""") shouldBeTry Ok('a')
                  j.fromJson<Char>(""""1"""") shouldBeTry Ok('1')
                 j.fromJson<Char>(""""\""""") shouldBeTry Ok('"')
                j.fromJson<Char>(""""\\""""") shouldBeTry Ok('\\')
                j.fromJson<Char>(""""\/""""") shouldBeTry Ok('/')
                j.fromJson<Char>(""""\b""""") shouldBeTry Ok('\b')
                j.fromJson<Char>(""""\f""""") shouldBeTry Ok('\u000c')
                 j.fromJson<Char>(""""\n"""") shouldBeTry Ok('\n')
                 j.fromJson<Char>(""""\r"""") shouldBeTry Ok('\r')
                 j.fromJson<Char>(""""\t"""") shouldBeTry Ok('\t')
             j.fromJson<Char>(""""\u0001"""") shouldBeTry Ok('\u0001')
             j.fromJson<Char>(""""\u01AF"""") shouldBeTry Ok('\u01AF')
             j.fromJson<Char>(""""\u01af"""") shouldBeTry Ok('\u01af')

             j.fromJson<Char>(""""\u0001"""") shouldBeTry Ok('\u0001')
             j.fromJson<Char>(""""\u00FF"""") shouldBeTry Ok('\u00FF')
             j.fromJson<Char>(""""\u0FFF"""") shouldBeTry Ok('\u0FFF')
                  j.fromJson<Char>(""""t"""") shouldBeTry Ok('t')
                 j.fromJson<Char>(""""語"""") shouldBeTry Ok('語')
                 j.fromJson<Char>(""""𝔊"""") shouldBeTry Error("𝔊 is not Char")
                 j.fromJson<Char>(""""क्तु"""") shouldBeTry Error("क्तु is not Char")
              j.fromJson<Char>(""""日本語"""") shouldBeTry Error("日本語 is not Char")
           j.fromJson<Char16>(""""\u0001"""") shouldBeTry Ok('\u0001')
           j.fromJson<Char16>(""""\u00FF"""") shouldBeTry Ok('\u00FF')
           j.fromJson<Char16>(""""\u0FFF"""") shouldBeTry Ok('\u0FFF')
                j.fromJson<Char16>(""""t"""") shouldBeTry Ok('t')
               j.fromJson<Char16>(""""語"""") shouldBeTry Ok('語')
                j.fromJson<Char16>(""""𝔊"""") shouldBeTry Error("𝔊 is not Char")
               j.fromJson<Char16>(""""क्तु"""") shouldBeTry Error("क्तु is not Char")
            j.fromJson<Char16>(""""日本語"""") shouldBeTry Error("日本語 is not Char")
           j.fromJson<Char32>(""""\u0001"""") shouldBeTry Ok('\u0001'.toChar32())
           j.fromJson<Char32>(""""\u00FF"""") shouldBeTry Ok('\u00FF'.toChar32())
           j.fromJson<Char32>(""""\u0FFF"""") shouldBeTry Ok('\u0FFF'.toChar32())
                j.fromJson<Char32>(""""t"""") shouldBeTry Ok('t'.toChar32())
               j.fromJson<Char32>(""""語"""") shouldBeTry Ok('語'.toChar32())
                j.fromJson<Char32>(""""𝔊"""") shouldBeTry Ok(Char32(0x1D50A))
               j.fromJson<Char32>(""""क्तु"""") shouldBeTry Error("क्तु is not Char32")
            j.fromJson<Char32>(""""日本語"""") shouldBeTry Error("日本語 is not Char32")

                       j.fromJson<Byte>("55") shouldBeTry Ok(55.toByte())
                      j.fromJson<UByte>("55") shouldBeTry Ok(55L.toUByte())
                      j.fromJson<Short>("55") shouldBeTry Ok(55.toShort())
                     j.fromJson<UShort>("55") shouldBeTry Ok(55.toUShort())
                        j.fromJson<Int>("55") shouldBeTry Ok(55)
                       j.fromJson<UInt>("55") shouldBeTry Ok(55.toUInt())
                       j.fromJson<Long>("55") shouldBeTry Ok(55L)
                      j.fromJson<ULong>("55") shouldBeTry Ok(55.toULong())
                      j.fromJson<Float>("55") shouldBeTry Ok(55f)
                     j.fromJson<Double>("55") shouldBeTry Ok(55.0)
                 j.fromJson<BigInteger>("55") shouldBeTry Ok(BigInteger("55"))
                 j.fromJson<BigDecimal>("55") shouldBeTry Ok(BigDecimal("55"))

                      j.fromJson<Byte>("+55") shouldBeTry Error("Invalid token at position 0")
                     j.fromJson<UByte>("+55") shouldBeTry Error("Invalid token at position 0")
                     j.fromJson<Short>("+55") shouldBeTry Error("Invalid token at position 0")
                    j.fromJson<UShort>("+55") shouldBeTry Error("Invalid token at position 0")
                       j.fromJson<Int>("+55") shouldBeTry Error("Invalid token at position 0")
                      j.fromJson<UInt>("+55") shouldBeTry Error("Invalid token at position 0")
                      j.fromJson<Long>("+55") shouldBeTry Error("Invalid token at position 0")
                     j.fromJson<ULong>("+55") shouldBeTry Error("Invalid token at position 0")
                     j.fromJson<Float>("+55") shouldBeTry Error("Invalid token at position 0")
                    j.fromJson<Double>("+55") shouldBeTry Error("Invalid token at position 0")
                j.fromJson<BigInteger>("+55") shouldBeTry Error("Invalid token at position 0")
                j.fromJson<BigDecimal>("+55") shouldBeTry Error("Invalid token at position 0")

                      j.fromJson<Byte>("-55") shouldBeTry Ok((-55).toByte())
                     j.fromJson<UByte>("-55") shouldBeTry Error("-55 not UByte")
                     j.fromJson<Short>("-55") shouldBeTry Ok((-55).toShort())
                    j.fromJson<UShort>("-55") shouldBeTry Error("-55 not UShort")
                       j.fromJson<Int>("-55") shouldBeTry Ok(-55)
                      j.fromJson<UInt>("-55") shouldBeTry Error("-55 not UInt")
                      j.fromJson<Long>("-55") shouldBeTry Ok(-55L)
                     j.fromJson<ULong>("-55") shouldBeTry Error("-55 not ULong")
                     j.fromJson<Float>("-55") shouldBeTry Ok(-55f)
                    j.fromJson<Double>("-55") shouldBeTry Ok(-55.0)
                j.fromJson<BigInteger>("-55") shouldBeTry Ok(BigInteger("-55"))
                j.fromJson<BigDecimal>("-55") shouldBeTry Ok(BigDecimal("-55"))

               j.fromJson<Byte>("2147483648") shouldBeTry Error("2147483648 not Byte")
                      j.fromJson<Byte>("128") shouldBeTry Error("128 not Byte")
                     j.fromJson<Byte>("-129") shouldBeTry Error("-129 not Byte")
              j.fromJson<UByte>("2147483648") shouldBeTry Error("2147483648 not UByte")
                     j.fromJson<UByte>("256") shouldBeTry Error("256 not UByte")
                    j.fromJson<UByte>("-256") shouldBeTry Error("-256 not UByte")
              j.fromJson<Short>("2147483648") shouldBeTry Error("2147483648 not Short")
                   j.fromJson<Short>("32768") shouldBeTry Error("32768 not Short")
                  j.fromJson<Short>("-32769") shouldBeTry Error("-32769 not Short")
             j.fromJson<UShort>("2147483648") shouldBeTry Error("2147483648 not UShort")
                  j.fromJson<UShort>("65537") shouldBeTry Error("65537 not UShort")
                 j.fromJson<UShort>("-65538") shouldBeTry Error("-65538 not UShort")
       j.fromJson<Int>("9223372036854775808") shouldBeTry Error("9223372036854775808 not Int")
                j.fromJson<Int>("2147483648") shouldBeTry Error("2147483648 not Int")
               j.fromJson<Int>("-2147483649") shouldBeTry Error("-2147483649 not Int")
      j.fromJson<UInt>("9223372036854775808") shouldBeTry Error("9223372036854775808 not UInt")
               j.fromJson<UInt>("4294967296") shouldBeTry Error("4294967296 not UInt")
              j.fromJson<UInt>("-4294967297") shouldBeTry Error("-4294967297 not UInt")
      j.fromJson<Long>("9223372036854775808") shouldBeTry Error("9223372036854775808 not Long")
     j.fromJson<Long>("-9223372036854775809") shouldBeTry Error("-9223372036854775809 not Long")
    j.fromJson<ULong>("92233720368547758080") shouldBeTry Error("92233720368547758080 not ULong")
   j.fromJson<ULong>("-92233720368547758080") shouldBeTry Error("-92233720368547758080 not ULong")
            j.fromJson<Float>("3.4028235E39") shouldBeTry Error("3.4028235E+39 not Float")
           j.fromJson<Float>("-3.4028235E39") shouldBeTry Error("-3.4028235E+39 not Float")
 j.fromJson<Double>("1.7976931348623157E309") shouldBeTry Error("1.7976931348623157E+309 not Double")
j.fromJson<Double>("-1.7976931348623157E309") shouldBeTry Error("-1.7976931348623157E+309 not Double")

                      j.fromJson<Int>("34E4") shouldBeTry Ok(340000)
                     j.fromJson<Int>("22e+2") shouldBeTry Ok(2200)
                     j.fromJson<Long>("34E4") shouldBeTry Ok(340000L)
                    j.fromJson<Long>("22e+2") shouldBeTry Ok(2200L)

               j.fromJson<Float>(""""NaN"""") shouldBeTry Ok(Float.NaN)
          j.fromJson<Float>(""""Infinity"""") shouldBeTry Ok(Float.POSITIVE_INFINITY)
         j.fromJson<Float>(""""-Infinity"""") shouldBeTry Ok(Float.NEGATIVE_INFINITY)
                     j.fromJson<Double>("55") shouldBeTry Ok(55.0)
              j.fromJson<Double>(""""NaN"""") shouldBeTry Ok(Double.NaN)
         j.fromJson<Double>(""""Infinity"""") shouldBeTry Ok(Double.POSITIVE_INFINITY)
        j.fromJson<Double>(""""-Infinity"""") shouldBeTry Ok(Double.NEGATIVE_INFINITY)

                 j.fromJson<String>("""""""") shouldBeTry Ok("")
                j.fromJson<String>("""" """") shouldBeTry Ok(" ")
             j.fromJson<String>(""""null"""") shouldBeTry Ok("null")
                     j.fromJson<Any?>("null") shouldBeTry Ok(null)
            j.fromJson<String?>(""""null"""") shouldBeTry Ok("null")
                  j.fromJson<String?>("null") shouldBeTry Ok(null)
         // @formatter:on
      }
   }
   "Write" - {
      "simple" {
         j.write(true) shouldBe "true"
         j.write(false) shouldBe "false"
         j.write(12) shouldBe "12"
         j.write(12.toShort()) shouldBe "12"
         j.write(12.toByte()) shouldBe "12"
         j.write(Int.MAX_VALUE) shouldBe "2147483647"
         j.write(Int.MIN_VALUE) shouldBe "-2147483648"
         j.write(12L) shouldBe "12"
         j.write(4000000000000L) shouldBe "4000000000000"
         j.write(9223372036854775807L) shouldBe "9223372036854775807"
         j.write(12.5f) shouldBe "12.5"
         j.write(Float.POSITIVE_INFINITY) shouldBe "\"Infinity\""
         j.write(Float.NEGATIVE_INFINITY) shouldBe "\"-Infinity\""
         j.write(Float.NaN) shouldBe "\"NaN\""
         j.write(12.5) shouldBe "12.5"
         j.write(Double.POSITIVE_INFINITY) shouldBe "\"Infinity\""
         j.write(Double.NEGATIVE_INFINITY) shouldBe "\"-Infinity\""
         j.write(Double.NaN) shouldBe "\"NaN\""
         j.write('o') shouldBe "\"o\""
         j.write("omg") shouldBe "\"omg\""
         j.write<Any>(null) shouldBe "null"
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
            j.fromJsonValue<Data>(json) shouldBeTry Error("null is not sp.it.util.file.json.`JsonTest\$1\$4\$1\$1\$Data`")
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
            j.fromJsonValue<Array<Data>>(json) shouldBeTry Error("null is not sp.it.util.file.json.`JsonTest\$1\$4\$3\$1\$Data`")
            j.fromJsonValue<Array<Data?>>(json) shouldBeTry Ok(arrayOf<Data?>(null, null))
            j.fromJsonValue<List<*>>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<List<Any>>(json) shouldBeTry Error("null is not kotlin.Any")
            j.fromJsonValue<List<Any?>>(json) shouldBeTry Ok(listOf(null, null))
            j.fromJsonValue<List<Data>>(json) shouldBeTry Error("null is not sp.it.util.file.json.`JsonTest\$1\$4\$3\$1\$Data`")
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
      "object" {
         j.toJsonValue(JsonTestObject).net { j.fromJsonValue<Any>(it) }.orThrow
         j.toJsonValue(JsonTestDataObject).net { j.fromJsonValue<Any>(it) }.orThrow
      }
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
            argIns(-Float.MAX_VALUE),
            argIns(Float.MIN_VALUE),
            argIns(Float.MAX_VALUE),
            argIns(Float.POSITIVE_INFINITY),
            argIns(Float.NEGATIVE_INFINITY),
            argIns(Float.NaN),
            argIns(-Double.MAX_VALUE),
            argIns(Double.MIN_VALUE),
            argIns(Double.MAX_VALUE),
            argIns(Double.POSITIVE_INFINITY),
            argIns(Double.NEGATIVE_INFINITY),
            argIns(Double.NaN),
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

            // exact type -> unknown type (this case loses exact type because 1 - numbers do not know their type and 2 - due to unsigned types being converted to a number)
            val valueOut7 = j.toJsonValue(type, valueIn).net { j.fromJsonValue(type<Any>(), it) }.orThrow
            valueIn.toString().toDouble() shouldBe valueOut7.toString().toDouble()
            valueOut7 shouldBeInstance Number::class

            val valueOut8 = j.toJsonValue(type, valueIn).toCompactS().net { j.fromJson(type<Any>(), it) }.orThrow
            valueIn.toString().toDouble() shouldBe valueOut8.toString().toDouble()
            if (valueOut8 !in setOf("NaN", "Infinity", "+Infinity", "-Infinity")) valueOut8 shouldBeInstance Number::class
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
   "toS" - {
      val json1 = JsTrue
      val json2 = JsArray()
      val json3 = JsObject()
      val json4 = JsArray(JsTrue)
      val json5 = JsObject("a" to JsTrue)
      val json6 = JsArray(JsTrue, JsFalse)
      val json7 = JsObject("a" to JsTrue, "b" to JsFalse)
      val json8 = JsArray(JsNull, JsString(""), JsNumber(1), JsArray(), JsObject(), JsArray(JsNumber(1), JsNumber(2)), JsObject("a" to JsTrue, "b" to JsFalse))

      "tokens" {
         json1.tokens().toList() shouldBe listOf(JsToken.Tru)
         json2.tokens().toList() shouldBe listOf(JsToken.Lbk, JsToken.Rbk)
         json3.tokens().toList() shouldBe listOf(JsToken.Lbc, JsToken.Rbc)
         json4.tokens().toList() shouldBe listOf(JsToken.Lbk, JsToken.Tru, JsToken.Rbk)
         json5.tokens().toList() shouldBe listOf(JsToken.Lbc, JsToken.Str("a"), JsToken.Col, JsToken.Tru, JsToken.Rbc)
         json6.tokens().toList() shouldBe listOf(JsToken.Lbk, JsToken.Tru, JsToken.Com, JsToken.Fal, JsToken.Rbk)
         json7.tokens().toList() shouldBe listOf(JsToken.Lbc, JsToken.Str("a"), JsToken.Col, JsToken.Tru, JsToken.Com, JsToken.Str("b"), JsToken.Col, JsToken.Fal, JsToken.Rbc)
         json8.tokens().toList() shouldBe listOf(
            JsToken.Lbk,
               JsToken.Nul,
               JsToken.Com,
               JsToken.Str(""),
               JsToken.Com,
               JsToken.Num(1),
               JsToken.Com,
               JsToken.Lbk,
               JsToken.Rbk,
               JsToken.Com,
               JsToken.Lbc,
               JsToken.Rbc,
               JsToken.Com,
               JsToken.Lbk,
                  JsToken.Num(1),
                  JsToken.Com,
                  JsToken.Num(2),
               JsToken.Rbk,
               JsToken.Com,
               JsToken.Lbc,
                  JsToken.Str("a"),
                  JsToken.Col,
                  JsToken.Tru,
                  JsToken.Com,
                  JsToken.Str("b"),
                  JsToken.Col,
                  JsToken.Fal,
               JsToken.Rbc,
            JsToken.Rbk
         )
      }
      "toCompactS" {
         json1.toCompactS() shouldBe """true"""
         json2.toCompactS() shouldBe """[]"""
         json3.toCompactS() shouldBe """{}"""
         json4.toCompactS() shouldBe """[true]"""
         json5.toCompactS() shouldBe """{"a":true}"""
         json6.toCompactS() shouldBe """[true,false]"""
         json7.toCompactS() shouldBe """{"a":true,"b":false}"""
         json8.toCompactS() shouldBe """[null,"",1,[],{},[1,2],{"a":true,"b":false}]"""
      }
      "toPrettyS" {
         val n = '\n'
         json1.toPrettyS() shouldBe """true"""
         json2.toPrettyS() shouldBe """[]"""
         json3.toPrettyS() shouldBe """{}"""
         json4.toPrettyS() shouldBe """[$n  true$n]"""
         json5.toPrettyS() shouldBe """{$n  "a": true$n}"""
         json6.toPrettyS() shouldBe """[$n  true,$n  false$n]"""
         json7.toPrettyS() shouldBe """{$n  "a": true,$n  "b": false$n}"""
         json8.toPrettyS() shouldBe """[$n  null,$n  "",$n  1,$n  [],$n  {},$n  [$n    1,$n    2$n  ],$n  {$n    "a": true,$n    "b": false$n  }$n]"""
      }
   }

   val dirJSONTestSuite = (File("").absoluteFile.parentFile?.parentFile?.parentFile?.net { it / "JSONTestSuite" }).printIt()
   "comprehensive (https://github.com/nst/JSONTestSuite)".config(enabled = dirJSONTestSuite?.exists()==true) - {
      for (f in (dirJSONTestSuite!! / "test_parsing").children().toList()) {
         val n = f.name
         val disabled = setOf(
            "n_multidigit_number_then_00.json",       // no idea how this should behave, but it is benign
            "n_structure_100000_opening_arrays.json", // enable one recursion is gone
            "n_structure_open_array_object.json"      // enable one recursion is gone
         )
         n.config(enabled = n !in disabled) {
            true shouldBe true
            if (n.startsWith("y")) j.ast(f)         .apply { if (isError) f.readText().printIt() }.orThrow
            if (n.startsWith("n")) j.ast(f).switch().apply { if (isError) f.readText().printIt() }.orThrow
         }
      }
   }
})

private object JsonTestObject
private data object JsonTestDataObject
private data class Gen          (val genDat: GenDat<Int>, val genCls: GenCls<Int>)
private data class GenDat<out T>(val value: T)
private      class GenCls<out T>(val value: T) {
   override fun equals(other: Any?) = other is GenCls<*> && other.value == value
   override fun hashCode() = value?.hashCode() ?: 0
}

            data class Complex(val x: Double, val y: Double)
@JvmInline value class InlineInt(val value: Int)
@JvmInline value class InlineComplex(val value: Complex)

private infix  fun Try<*,Throwable>.shouldBeTry(o: Try<*,String>) { if (this::class!=o::class) this shouldBe o; mapError { it.message }.getAny() shouldBe o.getAny() }
private inline fun <reified T: Any> Any?.shouldBeInstance() = T::class.isInstance(this) shouldBe true
private infix  fun                  Any?.shouldBeInstance(type: KClass<*>) = type.isInstance(this) shouldBe true
private infix  fun                  Any?.shouldBeInstance(type: KType) = type.raw.isInstance(this) shouldBe true

private inline fun <reified T: Any> arg()          = row(kType<T>())
private inline fun <reified T: Any> argIns(arg: T) = row(type<T>(), arg)

private inline fun <reified T: Any> Json.write(o: T?) = toJsonValue(o).toCompactS()