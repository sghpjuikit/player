package sp.it.util.type

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.reflect.KClass
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
   // https://youtrack.jetbrains.com/issue/KT-57641
   "KT-57641" {
      val o = object {}
      o::class.objectInstance shouldBe null
   }
   // https://youtrack.jetbrains.com/issue/KT-22792
   "KT-22792 (failing)" - {
      TestNotObject::class.objectInstance shouldBe null
      shouldThrowAny { TestObject::class.objectInstance shouldNotBe null }
      shouldThrowAny { TestDataObject::class.objectInstance shouldNotBe null }
   }
   "KT-22792" - {
         TestNotObject::class.objectInstanceSafe shouldBe null
         TestObject::class.objectInstanceSafe shouldNotBe null
         TestDataObject::class.objectInstanceSafe shouldNotBe null
      }
})

private class TestNotObject
private object TestObject
private data object TestDataObject