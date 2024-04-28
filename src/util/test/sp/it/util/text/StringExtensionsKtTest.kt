package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class StringExtensionsKtTest: FreeSpec({

   String::concatApplyBackspace.name - {
      "none" {
         "a".concatApplyBackspace("b") shouldBe "ab"
      }

      "\\b" {
         "abc".concatApplyBackspace("de\b\b\b\b") shouldBe "a"
         "abc".concatApplyBackspace("") shouldBe "abc"
         "abc".concatApplyBackspace("\b\ba\ba\b\b\b\b") shouldBe ""

         "".concatApplyBackspace("") shouldBe ""
         "".concatApplyBackspace("\b") shouldBe "\b"
      }

      "\\r" {
         "".concatApplyBackspace("") shouldBe ""
         "".concatApplyBackspace("\r") shouldBe "\r"
         "a".concatApplyBackspace("\r") shouldBe "\r"
         "abc".concatApplyBackspace("\r") shouldBe "\r"
         "\nabc".concatApplyBackspace("\r") shouldBe "\n"
         "abc\ndef".concatApplyBackspace("\r") shouldBe "abc\n"
         "abc\ndef".concatApplyBackspace("\r\r") shouldBe "abc\n"
         "abc\ndef".concatApplyBackspace("fgi\rjkl\r") shouldBe "abc\n"
      }
   }
})
