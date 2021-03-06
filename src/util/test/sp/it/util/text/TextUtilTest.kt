package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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
      "1:2:3".splitTrimmed(":") shouldBe listOf("1", "2", "3")
      ":2:".splitTrimmed(":") shouldBe listOf("2")
   }

   String::camelToDashCase.name - {
      "AaaBbbCCC".camelToDashCase() shouldBe "aaa-bbb-c-c-c"
      "AaaBbbCCC".camelToDashCase() shouldBe "AaaBbbCCC".camelToDashCase().lowercase()
   }

   String::camelToDotCase.name - {
      "AaaBbbCCC".camelToDotCase() shouldBe "aaa.bbb.c.c.c"
      "AaaBbbCCC".camelToDotCase() shouldBe "AaaBbbCCC".camelToDotCase().lowercase()
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
