package sp.it.util.math

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldBe
import sp.it.util.math.intersectsWith

class RangeExtensionsKtTest: FreeSpec({

   ClosedRange<Long>::intersectsWith.name - {

      "should return true when ranges intersect" {
         (10..20) intersectsWith (15..25) shouldBe true
         (15..25) intersectsWith (10..20) shouldBe true
      }

      "should return false when ranges do not intersect" {
         (10..20) intersectsWith (30..40) shouldBe false
         (30..40) intersectsWith (10..20) shouldBe false
      }

      "should return true when one range starts where another ends" {
         (10..11) intersectsWith (9..10) shouldBe true
         (9..10) intersectsWith (10..11) shouldBe true
      }

      "should return false when range empty" {
         (10..9) intersectsWith (10..11) shouldBe false
      }

      "should return true when range single" {
         (10..10) intersectsWith (10..10) shouldBe true
      }

      "should return false when one range is entirely contained within the other" {
         (1..4) intersectsWith (2..3) shouldBe true
      }
   }

})