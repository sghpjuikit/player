package sp.it.util.bool

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BooleanExtensionsKtTest: FreeSpec({

   Boolean::toByte.name {
      true.toByte() shouldBe 1.toByte()
      false.toByte() shouldBe 0.toByte()
      true.toByte(10, 20) shouldBe 10.toByte()
      false.toByte(10, 20) shouldBe 20.toByte()
   }

})