package sp.it.util.type

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class KotlinTest: FreeSpec({
   // https://youtrack.jetbrains.com/issue/KT-22792
   "KT-22792" - {
         TestNotObject::class.objectInstance shouldBe null
         TestObject::class.objectInstance shouldNotBe null
         TestDataObject::class.objectInstance shouldNotBe null
      }
})

private class TestNotObject
private object TestObject
private data object TestDataObject