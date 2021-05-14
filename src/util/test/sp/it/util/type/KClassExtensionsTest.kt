package sp.it.util.type

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

@Suppress("DEPRECATION")
class KClassExtensionsTest: FreeSpec({

   "Method" - {

      "${Class::class.simpleName}.${Class<*>::isEnum.name}" {
         // simple enum
         TEnumSimple::class.java.isEnum shouldBe true
         TEnumSimple.C1::class.java.isEnum shouldBe true
         // enums with class method body
         TEnumWithMethodBody::class.java.isEnum shouldBe true
         TEnumWithMethodBody.C1::class.java.isEnum shouldBe false  //  does not work, because each constant is own class
      }

      "${Class::class.simpleName}.${Class<*>::isEnumClass.name}" {
         // class is not enum
         TClass::class.java.isEnumClass shouldBe false
         // simple enum
         TEnumSimple::class.java.isEnumClass shouldBe true
         TEnumSimple.C1::class.java.isEnumClass shouldBe true
         // enums with class method body
         TEnumWithMethodBody::class.java.isEnumClass shouldBe true
         TEnumWithMethodBody.C1::class.java.isEnumClass shouldBe true
      }

      "${KClass::class.simpleName}.${KClass<*>::isEnum.name}" {
         // class is not enum
         TClass::class.isEnum shouldBe false
         // simple enum
         TEnumSimple::class.isEnum shouldBe true
         TEnumSimple.C1::class.isEnum shouldBe true
         // enums with class method body
         TEnumWithMethodBody::class.isEnum shouldBe true
         TEnumWithMethodBody.C1::class.isEnum shouldBe true
      }

      "${Class::class.simpleName}.${Class<*>::getEnumConstants.name}" {
         // class is not enum
         TClass::class.java.enumConstants shouldBe null
         // simple enum
         TEnumSimple::class.java.enumConstants shouldBe arrayOf(TEnumSimple.C1, TEnumSimple.C2)
         TEnumSimple.C1::class.java.enumConstants shouldBe arrayOf(TEnumSimple.C1, TEnumSimple.C2)
         // enums with class method body
         TEnumWithMethodBody::class.java.enumConstants shouldBe arrayOf(TEnumWithMethodBody.C1, TEnumWithMethodBody.C2)
         TEnumWithMethodBody.C1::class.java.enumConstants shouldBe null  //  does not work, because each constant is own class
      }

      "${Class::class.simpleName}.${Class<Any>::enumValues.name}" {
         // class is not enum
         shouldThrow<AssertionError> { TClass::class.java.enumValues }
         // simple enum
         TEnumSimple::class.java.enumValues shouldBe arrayOf(TEnumSimple.C1, TEnumSimple.C2)
         TEnumSimple.C1::class.java.enumValues shouldBe arrayOf(TEnumSimple.C1, TEnumSimple.C2)
         // enums with class method body
         TEnumWithMethodBody::class.java.enumValues shouldBe arrayOf(TEnumWithMethodBody.C1, TEnumWithMethodBody.C2)
         TEnumWithMethodBody.C1::class.java.enumValues shouldBe arrayOf(TEnumWithMethodBody.C1, TEnumWithMethodBody.C2)
      }

      "${KClass::class.simpleName}.${KClass<Any>::enumValues.name}" {
         // class is not enum
         shouldThrow<AssertionError> { TClass::class.enumValues }
         // simple enum
         TEnumSimple::class.enumValues shouldBe arrayOf(TEnumSimple.C1, TEnumSimple.C2)
         TEnumSimple.C1::class.enumValues shouldBe arrayOf(TEnumSimple.C1, TEnumSimple.C2)
         // enums with class method body
         TEnumWithMethodBody::class.enumValues shouldBe arrayOf(TEnumWithMethodBody.C1, TEnumWithMethodBody.C2)
         TEnumWithMethodBody.C1::class.enumValues shouldBe arrayOf(TEnumWithMethodBody.C1, TEnumWithMethodBody.C2)
      }

      "${KClass::class.simpleName}.${KClass<*>::isObject.name}" {
         TObject::class.isObject shouldBe true
         TClass::class.isObject shouldBe false
      }

   }

})


inline fun <reified T: Any> test(c: (KClass<T>) -> KCallable<*>): String = "${T::class.simpleName}.${c(T::class).name}"

class TClass
object TObject
enum class TEnumSimple { C1, C2 }
enum class TEnumWithMethodBody {
   C1 {
      override fun doX() = Unit
   },
   C2 {
      override fun doX() = Unit
   };

   abstract fun doX()
}