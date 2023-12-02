package sp.it.util.type

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.full.superclasses

class KotlinTest: FreeSpec({
   // https://youtrack.jetbrains.com/issue/KT-41322
   "KT-41322" {
      KotlinKT41322.Foo::class.annotations
      KotlinKT41322.Foo::class.isData shouldBe false
      KotlinKT41322.Foo::class.superclasses
   }
   // https://youtrack.jetbrains.com/issue/KT-41373
   "KT-41373" {
      KotlinKT41373.method()::class.annotations
      KotlinKT41373.method()::class.isData shouldBe false
      KotlinKT41373.method()::class.superclasses
   }
   // https://youtrack.jetbrains.com/issue/KT-22792
   "KT-22792".config(enabled = false) {
      (TestObject::class.objectInstance!=null) shouldBe true
      (TestDataObject::class.objectInstance!=null) shouldBe true
      TestObject::class.isObject shouldBe true
      TestDataObject::class.isObject shouldBe true
   }
   // https://youtrack.jetbrains.com/issue/KT-57641
   "KT-57641" {
      val o = object {}
      o::class.objectInstance shouldBe null
   }
})

private object TestObject
private data object TestDataObject


