package sp.it.util.type

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class KotlinTest: FreeSpec({
   "KT" - {
      // https://youtrack.jetbrains.com/issue/KT-41373
      "KT-41373" {
         KotlinKT41373.method()::class.annotations
      }
      // https://youtrack.jetbrains.com/issue/KT-22792
      "KT-22792".config(enabled = false) {
         TestObject::class.isObject shouldBe true
         TestDataObject::class.isObject shouldBe true
      }
      // https://youtrack.jetbrains.com/issue/KT-57641
      "KT-57641" {
         val o = object {}
         o::class.objectInstance shouldBe null
      }
   }
})

private object TestObject
private data object TestDataObject