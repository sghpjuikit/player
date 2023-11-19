package sp.it.util.collections.list

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.reflect.jvm.jvmName

class PrefListTest: FreeSpec({

   PrefList::class.jvmName - {

      PrefList<*>::preferred.name - {
         "should set preferred item if it exists in the list" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.preferred shouldBe "b"
         }

         "should not set preferred item if it does not exist in the list" {
            val list = PrefList(listOf("a", "b", "c"), "d")
            list.preferred shouldBe null
         }
      }

      PrefList<*>::addPreferred.name - {
         "should add item to the list and set it as preferred if specified" {
            val list = PrefList<String>()
            list.addPreferred("d", true)
            list.preferred shouldBe "d"
            list shouldContain "d"
         }

         "should add item to the list but not set it as preferred if specified" {
            val list = PrefList<String>()
            list.addPreferred("d", false)
            list.preferred shouldBe null
            list shouldContain "d"
         }
      }

      PrefList<*>::remove.name - {
         "should remove item from the list and clear preferred if it was the preferred item" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.remove("b") shouldBe true
            list.preferred shouldBe null
         }

         "should remove item from the list but not clear preferred if it was not the preferred item" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.remove("a") shouldBe true
            list.preferred shouldBe "b"
         }
      }

      PrefList<*>::removeAt.name - {
         "should remove item at index from the list and clear preferred if it was the preferred item" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.removeAt(1) shouldBe "b"
            list.preferred shouldBe null
         }

         "should remove item at index from the list but not clear preferred if it was not the preferred item" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.removeAt(0) shouldBe "a"
            list.preferred shouldBe "b"
         }
      }

      PrefList<*>::removeIf.name - {
         "should remove matching item from the list and clear preferred if it was the preferred item" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.removeIf { it == "b" } shouldBe true
            list.preferred shouldBe null
         }

         "should remove matching item from the list but not clear preferred if it was not the preferred item" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.removeIf { it == "a" } shouldBe true
            list.preferred shouldBe "b"
         }
      }

      PrefList<*>::removeAll.name - {
         "should remove items from the list and clear preferred if it was in the removed items" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.removeAll(listOf("b", "c")) shouldBe true
            list.preferred shouldBe null
         }

         "should remove items from the list but not clear preferred if it was not in the removed items" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.removeAll(listOf("a")) shouldBe true
            list.preferred shouldBe "b"
         }
      }

      PrefList<*>::replaceAll.name - {
         "should replace all items in the list and update preferred if it was in the list" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.replaceAll { it.uppercase() }
            list.preferred shouldBe "B"
            list shouldBe listOf("A", "B", "C")
         }

         "should replace all items in the list but not update preferred if it was not in the list" {
            val list = PrefList(listOf("a", "b", "c"), "d")
            list.replaceAll { it.uppercase() }
            list.preferred shouldBe null
            list shouldBe listOf("A", "B", "C")
         }
      }

      PrefList<*>::retainAll.name - {
         "should retain items in the list and clear preferred if it was not in the retained items" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.retainAll(listOf("a", "c")) shouldBe true
            list.preferred shouldBe null
            list shouldBe listOf("a", "c")
         }

         "should retain items in the list but not clear preferred if it was in the retained items" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.retainAll(listOf("a", "b")) shouldBe true
            list.preferred shouldBe "b"
            list shouldBe listOf("a", "b")
         }
      }

      PrefList<*>::clear.name - {
         "should clear the list and clear preferred" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            list.clear()
            list.preferred shouldBe null
            list shouldBe emptyList()
         }
      }

      "map" - {
         "should map items in the list and update preferred if it was in the list" {
            val list = PrefList(listOf("a", "b", "c"), "b")
            val mappedList = list.map { it.uppercase() }
            mappedList.preferred shouldBe "B"
            mappedList shouldBe listOf("A", "B", "C")
         }

         "should map items in the list but not update preferred if it was not in the list" {
            val list = PrefList(listOf("a", "b", "c"), "d")
            val mappedList = list.map { it.uppercase() }
            mappedList.preferred shouldBe null
            mappedList shouldBe listOf("A", "B", "C")
         }
      }
   }
})
