package tester

import javafx.animation.Animation.INDEFINITE
import javafx.animation.Interpolator
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.Slider
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.DEVELOPMENT
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.IconUN
import sp.it.pl.main.Key
import sp.it.pl.main.Widgets.TESTER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.setToOne
import sp.it.util.conf.CheckList
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.ANY
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.c
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cList
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.only
import sp.it.util.conf.toConfigurableFx
import sp.it.util.conf.uiOut
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.consumeScrolling
import sp.it.util.type.type
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year
import java.io.File
import kotlin.math.sqrt

@Suppress("RemoveExplicitTypeArguments", "RemoveRedundantBackticks", "RemoveExplicitTypeArguments", "RedundantLambdaArrow")
@ExperimentalController("For development")
class Tester(widget: Widget): SimpleController(widget) {
   val content = stackPane()
   val onContentChange = Disposer()

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.lay += vBox(0.0, TOP_CENTER) {
         lay += hBox(25.emScaled, TOP_CENTER) {
            padding = Insets(20.emScaled)
            lay += Icon(IconOC.CODE).onClickDo { testFxConfigs() }.withText("Test Fx Configs")
            lay += Icon(IconOC.CODE).onClickDo { testEditors() }.withText("Test Config Editors")
            lay += Icon(IconOC.CODE).onClickDo { testInterpolators() }.withText("Test Animations")
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

   override fun close() {
      onContentChange()
      content.children.clear()
   }

   fun testFxConfigs() {
      val c = object {
         val `vBoolean` = v(true)
         val `vBoolean_null` = vn(true)
         val `vxBoolean` = vx<Boolean>(true)
         val `vxBoolean_null` = vx<Boolean?>(true)
      }
      onContentChange()
      content.children setToOne form(c.toConfigurableFx())
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
         var `File_simple` by c<File>(APP.location.spitplayer_exe)
         var `File_simple_null` by cn<File>(null)
         var `File_observe` by c<File>(APP.location.spitplayer_exe)
         val `File_observe_null` by cvn<File>(null)
         var `File_observe_only_file_save` by c<File>(APP.location.spitplayer_exe).only(FILE).uiOut()
         var `File_observe_only_file` by c<File>(APP.location.spitplayer_exe).only(FILE)
         var `File_observe_only_dir` by c<File>(APP.location).only(DIRECTORY)
         var `File_observe_only_any` by c<File>(APP.location).only(ANY)
         var `Font_simple_null` by cn<Font>(null)
         val `Font_observe_null` by cvn<Font>(null)
         var `Color_simple_null` by cn<Color>(null)
         val `Color_observe_null` by cvn<Color>(null)
         val `List` by cList<Int>(1, 2, 3)
         val `List_null` by cList<Int?>(1, 2, null)
         val `CheckList_` by cCheckList(CheckList.nonNull(type<Boolean?>(), listOf(true, false, null), listOf(true, false, false)))
         val `CheckList_null_` by cCheckList(CheckList.nullable(type<Boolean?>(), listOf(true, false, null), listOf(true, false, null)))
      }
      onContentChange()
      content.children setToOne form(c)
   }

   fun testInterpolators() {
      val interpolators = mapOf<String, (Double) -> Double>(
         "LINEAR (JavaFx)" to { it -> Interpolator.LINEAR.interpolate(0.0, 1.0, it) },
         "EASE_BOTH (JavaFx)" to { it -> Interpolator.EASE_BOTH.interpolate(0.0, 1.0, it) },
         "EASE_IN (JavaFx)" to { it -> Interpolator.EASE_IN.interpolate(0.0, 1.0, it) },
         "EASE_OUT (JavaFx)" to { it -> Interpolator.EASE_OUT.interpolate(0.0, 1.0, it) },
         "x" to { it -> it },
         "x^2" to { it -> it*it },
         "x^3" to { it -> it*it*it },
         "x^4" to { it -> it*it*it*it },
         "x^-2 (sqrt(x))" to { it -> sqrt(it) },
         "x^-4 (sqrt(sqrt(x)))" to { it -> sqrt(sqrt(it)) }
      )
      onContentChange()
      content.children setToOne scrollPane {
         content = vBox {
            lay += interpolators.map { (name, interpolator) ->
               vBox {
                  padding = Insets(5.emScaled)
                  lay += label(name)
                  lay += hBox(15.emScaled, CENTER_RIGHT) {
                     lay += stackPane {
                        lay += Slider().apply {
                           prefWidth = 200.emScaled
                           min = 0.0
                           max = 1.0
                        }
                     }
                     lay += Icon(IconFA.STICKY_NOTE, 25.0)
                     lay += Icon(IconFA.STICKY_NOTE, 25.0)
                     lay += Icon(IconFA.STICKY_NOTE, 25.0)

                     anim(1.seconds) {
                        lookupChildAt<StackPane>(0).lookupChildAt<Slider>(0).value = it
                        lookupChildAt<Icon>(1).opacity = it
                        lookupChildAt<Icon>(2).rotate = 360*it
                        lookupChildAt<Icon>(3).setScaleXY(it)
                     }.apply {
                        intpl(interpolator)
                        delay = 1.seconds
                        cycleCount = INDEFINITE
                        isAutoReverse = true
                        onContentChange += ::stop
                        playOpen()
                     }
                  }
               }
            }
         }
      }
   }

   companion object: WidgetCompanion {
      override val name = TESTER_NAME
      override val description = "Provides facilities for testing and development"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf<ShortcutPane.Entry>()
      override val group = DEVELOPMENT
   }
}