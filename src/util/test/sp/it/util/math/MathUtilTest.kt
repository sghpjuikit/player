package sp.it.util.math

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe

class MathUtilTest: FreeSpec({
   "Method" - {

      "Comparable.min/max" {
         (1.0 min 3.0 + 5.5) shouldBeExactly (1.0 min (3.0 + 5.5))

         (5.0 + 1.0 min 3.0 + 5.5) shouldBeExactly ((5.0 + 1.0) min (3.0 + 5.5))

         (1.0 max 3.0 min .5) shouldBeExactly ((1.0 max 3.0) min .5)

         (1.0 max 3.0*0.1 min 0.5 + 10.0) shouldBeExactly (1.0 max (3.0*0.1) min (0.5 + 10.0))

         (1000.0 + 5.0*1.0 min 3.0*5.5) shouldBeExactly ((1000.0 + 5.0*1.0) min (3.0*5.5))
      }

      @Suppress("SimplifyBooleanWithConstants")
      "Boolean.times/plus" {
         (true && false plus true) shouldBe ((false && false) plus true)

         (true plus true && false) shouldBe ((true plus true) && false)

         (false times false || true) shouldBe ((false times false) || true)

         (true || false times false) shouldBe (true || (false times false))

         (false times false plus true) shouldBe ((false times false) plus true)
      }

   }
})
