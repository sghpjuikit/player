package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.streams.toList

@Suppress("SpellCheckingInspection")
class TextUtilTest: FreeSpec({
   "String.split (Kotlin vs Java)" - {

      "1:2:3".split(":") shouldBe listOf("1", "2", "3")
      "1:2:3".split(":", limit = 3) shouldBe listOf("1", "2", "3")
      "1:2:3".split(":", limit = 2) shouldBe listOf("1", "2:3")
      "1:2:3".split(":", limit = 1) shouldBe listOf("1:2:3")
      "1:2:3".split(":", limit = 0) shouldBe listOf("1", "2", "3")

      ":2:".split(":") shouldBe listOf("", "2", "")
      ":2:".split(":", limit = 3) shouldBe listOf("", "2", "")
      ":2:".split(":", limit = 2) shouldBe listOf("", "2:")
      ":2:".split(":", limit = 1) shouldBe listOf(":2:")
      ":2:".split(":", limit = 0) shouldBe listOf("", "2", "")

      "1:2:3".split(":".toRegex()) shouldBe listOf("1", "2", "3")
      ":2:".split(":".toRegex()) shouldBe listOf("", "2", "")

      "1:2:3".split(":".toRegex()) shouldBe listOf("1", "2", "3")
      ":2:".split(":".toRegex()) shouldBe listOf("", "2", "")
      ":2:".split(":".toRegex()).dropLastWhile { it.isEmpty() } shouldBe listOf("", "2")
      ":2:".split(":".toRegex()).dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() } shouldBe listOf("2")
   }

   String::splitTrimmed.name - {
      "".splitTrimmed(":") shouldBe listOf("")
      "1".splitTrimmed(":") shouldBe listOf("1")
      "1:2:3".splitTrimmed(":") shouldBe listOf("1", "2", "3")
      ":2:".splitTrimmed(":") shouldBe listOf("2")
      "::::".splitTrimmed(":") shouldBe listOf("", "", "")
      ":1:2:3:".splitTrimmed(":") shouldBe listOf("1", "2", "3")
   }

   String::splitNoEmpty.name - {
      "".splitNoEmpty(":").toList() shouldBe listOf()
      "1".splitNoEmpty(":").toList() shouldBe listOf("1")
      "1:2:".splitNoEmpty(":").toList() shouldBe listOf("1", "2")
      "::::".splitNoEmpty(":").toList() shouldBe listOf()
      ":1::2:".splitNoEmpty(":").toList() shouldBe listOf("1", "2")
   }

   String::camelToDashCase.name - {
      "AaaBbbCCC".camelToDashCase() shouldBe "aaa-bbb-c-c-c"
      "AaaBbbCCC".camelToDashCase() shouldBe "AaaBbbCCC".camelToDashCase().lowercase()
   }

   String::camelToDotCase.name - {
      "AaaBbbCCC".camelToDotCase() shouldBe "aaa.bbb.c.c.c"
      "AaaBbbCCC".camelToDotCase() shouldBe "AaaBbbCCC".camelToDotCase().lowercase()
   }

   "characters" - {

      // https://engineering.linecorp.com/en/blog/the-7-ways-of-counting-characters/

      "A".chars().toList() shouldBe listOf(0x0041)
      "A".chars16().toList() shouldBe listOf(0x0041).map { it.toChar16() }
      "A".codePoints().toList() shouldBe listOf(0x0041)
      "A".chars32().toList() shouldBe listOf(0x0041).map { it.toChar32() }
      "A".graphemes().toList() shouldBe listOf("A")
      "A".lengthInChars shouldBe 1
      "A".lengthInCodePoints shouldBe 1
      "A".lengthInGraphemes shouldBe 1
      "A"[0] shouldBe 0x0041.toChar()
      "A".char16At(0) shouldBe 0x0041.toChar16()
      "A".codePointAt(0) shouldBe 0x0041
      "A".char32At(0) shouldBe 0x0041.toChar32()
      "A".graphemeAt(0) shouldBe "A"

      "𝔊".chars().toList() shouldBe listOf(0xD835, 0xDD0A)
      "𝔊".chars16().toList() shouldBe listOf(0xD835, 0xDD0A).map { it.toChar16() }
      "𝔊".codePoints().toList() shouldBe listOf(0x1D50A)
      "𝔊".chars32().toList() shouldBe listOf(0x1D50A).map { it.toChar32() }
      "𝔊".graphemes().toList() shouldBe listOf("𝔊")
      "𝔊".lengthInChars shouldBe 2
      "𝔊".lengthInCodePoints shouldBe 1
      "𝔊".lengthInGraphemes shouldBe 1
      "𝔊"[0] shouldBe 0xD835.toChar()
      "𝔊".char16At(0) shouldBe 0xD835.toChar16()
      "𝔊".codePointAt(0) shouldBe 0x1D50A
      "𝔊".char32At(0) shouldBe 0x1D50A.toChar32()
      "𝔊".graphemeAt(0) shouldBe "𝔊"

      "क्तु".chars().toList() shouldBe listOf(0x0915, 0x094D, 0x0924, 0x0941)
      "क्तु".chars16().toList() shouldBe listOf(0x0915, 0x094D, 0x0924, 0x0941).map { it.toChar16() }
      "क्तु".codePoints().toList() shouldBe listOf(0x0915, 0x094D, 0x0924, 0x0941)
      "क्तु".chars32().toList() shouldBe listOf(0x0915, 0x094D, 0x0924, 0x0941).map { it.toChar32() }
      "क्तु".graphemes().toList() shouldBe listOf("क्तु")
      "क्तु".lengthInChars shouldBe 4
      "क्तु".lengthInCodePoints shouldBe 4
      "क्तु".lengthInGraphemes shouldBe 1
      "क्तु"[0] shouldBe 0x0915.toChar()
      "क्तु".char16At(0) shouldBe 0x0915.toChar16()
      "क्तु".codePointAt(0) shouldBe 0x0915
      "क्तु".char32At(0) shouldBe 0x0915.toChar32()
      "क्तु".graphemeAt(0) shouldBe "क्तु"

      "🅱️".chars().toList() shouldBe listOf(0xD83C, 0xDD71, 0xFE0F)
      "🅱️".chars16().toList() shouldBe listOf(0xD83C, 0xDD71, 0xFE0F).map { it.toChar16() }
      "🅱️".codePoints().toList() shouldBe listOf(0x1f171, 0xFE0F)
      "🅱️".chars32().toList() shouldBe listOf(0x1f171, 0xFE0F).map { it.toChar32() }
      "🅱️".graphemes().toList() shouldBe listOf("🅱️")
      "🅱️".lengthInChars shouldBe 3
      "🅱️".lengthInCodePoints shouldBe 2
      "🅱️".lengthInGraphemes shouldBe 1
      "🅱️"[0] shouldBe 0xD83C.toChar()
      "🅱️".char16At(0) shouldBe 0xD83C.toChar16()
      "🅱️".codePointAt(0) shouldBe 0x1f171
      "🅱️".char32At(0) shouldBe 0x1f171.toChar32()
      "🅱️".graphemeAt(0) shouldBe "🅱️"

      "A𝔊क्तु🅱🅱️".graphemes().toList() shouldBe listOf("A", "𝔊", "क्तु", "🅱", "🅱️")
      "A𝔊क्तु🅱🅱️".graphemeAt(0) shouldBe "A"
      "A𝔊क्तु🅱🅱️".graphemeAt(1) shouldBe "𝔊"
      "A𝔊क्तु🅱🅱️".graphemeAt(2) shouldBe "क्तु"
      "A𝔊क्तु🅱🅱️".graphemeAt(3) shouldBe "🅱"
      "A𝔊क्तु🅱🅱️".graphemeAt(4) shouldBe "🅱️"
   }

   String::capital.name - {
      "basic" {
         "lol".capital() shouldBe "Lol"
         "♡♡♡".capital() shouldBe "♡♡♡"
      }
      "ligatures" {
         "ﬃtext".capital() shouldBe "Ffitext"
      }
      "digraphs" {
         "ǆentlmen".capital() shouldBe "ǅentlmen"
         "Ǆentlmen".capital() shouldBe "Ǆentlmen"
      }
   }
})