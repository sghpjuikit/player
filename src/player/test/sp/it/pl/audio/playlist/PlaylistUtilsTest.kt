package sp.it.pl.audio.playlist

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PlaylistUtilsTest: FreeSpec({
   "methods" - {
      "moveItemsBy" {
         moveItemsBy(listOf<Int>(), listOf(1, 3), 1) shouldBe (listOf<Int>() to listOf(1, 3))
         moveItemsBy(listOf(1, 2), listOf(), 1) shouldBe (listOf(1, 2) to listOf())

         moveItemsBy(listOf(1, 2, 3, 4, 5), listOf(1, 3),  0) shouldBe (listOf(1, 2, 3, 4, 5) to listOf(1, 3))
         moveItemsBy(listOf(1, 2, 3, 4, 5), listOf(1, 3), +1) shouldBe (listOf(1, 3, 2, 5, 4) to listOf(2, 4))
         moveItemsBy(listOf(1, 2, 3, 4, 5), listOf(1, 3), -1) shouldBe (listOf(2, 1, 4, 3, 5) to listOf(0, 2))

         moveItemsBy(listOf(1, 2, 3, 4, 5), listOf(1, 3),  0) shouldBe (listOf(1, 2, 3, 4, 5) to listOf(1, 3))
         moveItemsBy(listOf(1, 2, 3, 4, 5), listOf(1, 3), +2) shouldBe (listOf(1, 3, 2, 5, 4) to listOf(2, 4))
         moveItemsBy(listOf(1, 2, 3, 4, 5), listOf(1, 3), -2) shouldBe (listOf(2, 1, 4, 3, 5) to listOf(0, 2))
      }
   }
})
