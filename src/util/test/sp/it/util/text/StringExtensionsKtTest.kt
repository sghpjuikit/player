package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class StringExtensionsKtTest: FreeSpec({

   String::concatenateWithBackspace.name {
      "a".concatenateWithBackspace("b") shouldBe "ab"

      "abc".concatenateWithBackspace("de\b\b\b\b") shouldBe "a"
      "abc".concatenateWithBackspace("") shouldBe "abc"
      "abc".concatenateWithBackspace("\b\ba\ba\b\b\b\b") shouldBe ""

      "".concatenateWithBackspace("") shouldBe ""
      "".concatenateWithBackspace("\b") shouldBe ""
   }
})
