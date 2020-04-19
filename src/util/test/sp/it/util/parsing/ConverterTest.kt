package sp.it.util.parsing

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.type.type
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class ConverterTest: FreeSpec({

   Converter::class.jvmName - {
      val c = object: Converter() {
         override fun <T: Any> ofS(type: KClass<T>, text: String) = Try.ok(text.toIntOrNull()).asIs<Try<T?, String>>()
         override fun <T> toS(o: T) = if (o is Int) o.toString() else ""
      }

      "ofS" {
         c.ofS(Int::class, "") shouldBe Try.ok(null)
         c.ofS(Int::class, "5") shouldBe Try.ok(5)

         c.ofS(type<Int?>(), "") shouldBe Try.ok(null)
         c.ofS(type<Int?>(), "5") shouldBe Try.ok(5)
         c.ofS(type<Int>(), "") should  { it.isError }
         c.ofS(type<Int>(), "5") shouldBe Try.ok(5)

         c.ofS<Int?>("") shouldBe Try.ok(null)
         c.ofS<Int?>("5") shouldBe Try.ok(5)
         c.ofS<Int>("") should { it.isError }
         c.ofS<Int>("5") shouldBe Try.ok(5)
      }

      "toConverterOf" {
         c.toConverterOf(Int::class).ofS("") shouldBe c.ofS(Int::class, "")
         c.toConverterOf(Int::class).ofS("5") shouldBe c.ofS(Int::class, "5")
      }
   }

})