package sp.it.util.collections.list

import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class DestructuredListTest: FreeSpec({

   val l = DestructuredList(listOf(1, 2, 3, 4, 5))

   "should contain the elements in the original list" {
      l shouldBe listOf(1, 2, 3, 4, 5)
   }
   "should return the first element" {
      val (first) = l
      first shouldBe 1
   }
   "should return the second element" {
      val (_, second) = l
      second shouldBe 2
   }
   "should return the third element" {
      val (_, _, third) = l
      third shouldBe 3
   }
   "should fail out of bounds" {
      @Suppress("UNUSED_VARIABLE")
      shouldThrowUnit<IndexOutOfBoundsException> {
         val (_, _, _, _, _, sixth) = l
      }
   }
   "should not fail out of bounds if unused" {
      val (_, _, _, _, _, _) = l
   }
})