package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class StringExtensionsKtTest: FreeSpec({

   String::concatApplyBackspace.name {
      "a".concatApplyBackspace("b") shouldBe "ab"

      "abc".concatApplyBackspace("de\b\b\b\b") shouldBe "a"
      "abc".concatApplyBackspace("") shouldBe "abc"
      "abc".concatApplyBackspace("\b\ba\ba\b\b\b\b") shouldBe ""

      "".concatApplyBackspace("") shouldBe ""
      "".concatApplyBackspace("\b") shouldBe ""
   }
})
