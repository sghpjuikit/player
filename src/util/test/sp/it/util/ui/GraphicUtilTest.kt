package sp.it.util.ui

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import javafx.scene.Node
import javafx.scene.control.TreeItem
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

      Node::isParentOf.name {
         val cc = stackPane()
         val c = stackPane { lay += cc }
         val p = stackPane { lay += c }
         val pp = stackPane { lay += p }

         p.isParentOf(pp) shouldBe false
         p.isParentOf(p) shouldBe false
         p.isParentOf(c) shouldBe true
         p.isParentOf(cc) shouldBe false

         p.isAnyParentOf(pp) shouldBe false
         p.isAnyParentOf(p) shouldBe false
         p.isAnyParentOf(c) shouldBe true
         p.isAnyParentOf(cc) shouldBe true

         pp.isChildOf(p) shouldBe false
         p.isChildOf(p) shouldBe false
         c.isChildOf(p) shouldBe true
         cc.isChildOf(p) shouldBe false

         pp.isAnyChildOf(p) shouldBe false
         p.isAnyChildOf(p) shouldBe false
         c.isAnyChildOf(p) shouldBe true
         cc.isAnyChildOf(p) shouldBe true
      }

      TreeItem<Any>::root.name {
         val ttt = TreeItem<Any>()
         val tt = TreeItem<Any>().apply { this.children += ttt }
         val t = TreeItem<Any>().apply { this.children += tt }

         ttt.root shouldBe t
         tt.root shouldBe t
         t.root shouldBe t
      }
   }
})

private inline fun <reified T: Any> rowProp(property: KFunction<Any?>) = row(property.returnType.javaType, T::class.javaObjectType)