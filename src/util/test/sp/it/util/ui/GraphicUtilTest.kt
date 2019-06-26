package sp.it.util.ui

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType

class GraphicUtilTest: FreeSpec({
   "Method" - {

      ::typeText.name {
         val text = "Text with spaces and special characters like tab=\t or newline=\n inside."
         typeText("")(0.0) shouldBe ""
         typeText("")(1.0) shouldBe ""
         typeText(text)(0.0) shouldBe ""
         typeText(text)(1.0) shouldBe text
         typeText("ex", '_')(0.0) shouldBe "__"
         typeText(text, '_')(0.0).length shouldBe text.length
         typeText(text, '_')(1.0) shouldBe text
      }

   }
})

private inline fun <reified T: Any> rowProp(property: KFunction<Any?>) = row(property.returnType.javaType, T::class.javaObjectType)