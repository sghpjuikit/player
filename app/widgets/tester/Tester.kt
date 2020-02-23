package tester

import javafx.geometry.Pos.CENTER
import javafx.scene.paint.Color
import javafx.scene.text.Font
import sp.it.pl.gui.objects.form.Form.Companion.form
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.IconOC
import sp.it.pl.main.Key
import sp.it.pl.main.emScaled
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.conf.CheckList
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.c
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cList
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.toConfigurableFx
import sp.it.util.reactive.consumeScrolling
import sp.it.util.type.type
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import java.io.File

@Suppress("RemoveExplicitTypeArguments", "RemoveRedundantBackticks", "RemoveExplicitTypeArguments")
@Widget.Info(
   author = "Martin Polakovic",
   name = "Tester",
   description = "Provides facilities for testing and development.",
   version = "1.0.0",
   year = "2020",
   group = Widget.Group.DEVELOPMENT
)
@ExperimentalController("For development")
class Tester(widget: Widget): SimpleController(widget) {
   val content = stackPane()

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.lay += vBox {
         lay += hBox(5, CENTER) {
            lay += Icon(IconOC.CODE).onClickDo { testFxConfigs() }.withText("Test Fx Configs")
            lay += Icon(IconOC.CODE).onClickDo { testEditors() }.withText("Test Config Editors")
         }
         lay += content
      }

      // Create test inputs/outputs
      io.i.create<Number>("Int", 5) {}
      io.i.create<Number?>("Int?", null) {}
      io.i.create<MutableList<out Number>>("List out Int", null) {}
      io.i.create<MutableList<Number>>("List Int", null) {}
      io.i.create<MutableList<in Number>>("List in Int", null) {}
      io.o.create<Int>("Nothing?", 5)
      io.o.create<Number>("Int", 5)

   }

   fun testFxConfigs() {
      val c = object {
         val `vBoolean` = v(true)
         val `vBoolean_null` = vn(true)
         val `vxBoolean` = vx<Boolean>(true)
         val `vxBoolean_null` = vx<Boolean?>(true)
      }
      content.children.clear()
      content.lay += form(c.toConfigurableFx())
   }

   fun testEditors() {
      val c = object: ConfigurableBase<Any?>() {
         var `Boolean_simple` by c<Boolean>(true)
         var `Boolean_simple_null` by cn<Boolean>(null)
         val `Boolean_observe` by cv<Boolean>(true)
         val `Boolean_observe_null` by cvn<Boolean>(null)
         var `KeyCode_simple` by c<Key>(Key.A)
         var `KeyCode_simple_null` by cn<Key>(null)
         val `KeyCode_observe` by cv<Key>(Key.A)
         val `KeyCode_observe_null` by cvn<Key>(null)
         var `Int_simple` by c<Int>(0)
         var `Int_simple_null` by cn<Int>(null)
         val `Int_observe` by cv<Int>(0)
         val `Int_observe_null` by cvn<Int>(null)
         var `File_simple_null` by cn<File>(null)
         val `File_observe_null` by cvn<File>(null)
         var `Font_simple_null` by cn<Font>(null)
         val `Font_observe_null` by cvn<Font>(null)
         var `Color_simple_null` by cn<Color>(null)
         val `Color_observe_null` by cvn<Color>(null)
         val `List` by cList<Int>(1, 2, 3)
         val `List_null` by cList<Int?>(1, 2, null)
         val `CheckList_` by cCheckList(CheckList.nonNull(type<Boolean?>(), listOf(true, false, null), listOf(true, false, false)))
         val `CheckList_null_` by cCheckList(CheckList.nullable(type<Boolean?>(), listOf(true, false, null), listOf(true, false, null)))
      }
      content.children.clear()
      content.lay += form(c)
   }
}