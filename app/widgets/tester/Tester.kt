package tester

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javafx.animation.Animation.INDEFINITE
import javafx.animation.Interpolator
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.Separator
import javafx.scene.control.Slider
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import kotlin.math.sqrt
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
import sp.it.pl.ui.pane.ConfigPane.Companion.compareByDeclaration
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.action.ActionManager
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
import sp.it.util.text.nameUi
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
            padding = Insets(10.emScaled, 10.emScaled, 20.emScaled, 10.emScaled)
            lay += Icon(IconOC.CODE).onClickDo { testInputs() }.withText("Test widget inputs")
            lay += Icon(IconOC.CODE).onClickDo { testFxConfigs() }.withText("Test Fx Configs")
            lay += Icon(IconOC.CODE).onClickDo { testEditors() }.withText("Test Config Editors")
            lay += Icon(IconOC.CODE).onClickDo { testInterpolators() }.withText("Test Animations")
         }
         lay += Separator()
         lay += stackPane {
            padding = Insets(20.emScaled, 10.emScaled, 0.0, 10.emScaled)
            lay += content
         }
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

   fun testInputs() {
      val c = label {
         text = "Trigger widget layout mode (${ActionManager.keyManageLayout.nameUi}) to test this widget's inputs"
         isWrapText = true
         textAlignment = TextAlignment.CENTER
      }
      onContentChange()
      content.children setToOne c
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

   @Suppress("ObjectPropertyName", "NonAsciiCharacters", "DANGEROUS_CHARACTERS")
   fun testEditors() {
      val c = object: ConfigurableBase<Any?>() {
         var `c(Boolean)` by c<Boolean>(true)
         var `cn(Boolean)` by cn<Boolean>(null)
         val `cv(Boolean)` by cv<Boolean>(true)
         val `cvn(Boolean)` by cvn<Boolean>(null)
         var `c(Key)` by c<Key>(Key.A)
         var `cn(Key)` by cn<Key>(null)
         val `cv(Key)` by cv<Key>(Key.A)
         val `cvn(Key)` by cvn<Key>(null)
         var `c(Int)` by c<Int>(0)
         var `cn(Int)` by cn<Int>(null)
         val `cv(Int)` by cv<Int>(0)
         val `cvn(Int)` by cvn<Int>(null)
         var `c(File)` by c<File>(APP.location.spitplayer_exe)
         var `cn(File)` by cn<File>(null)
         val `cv(File)` by cv<File>(APP.location.spitplayer_exe)
         val `cvn(File)` by cvn<File>(null)
         var `c(File)·only(FILE)·uiOut()` by c<File>(APP.location.spitplayer_exe).only(FILE).uiOut()
         var `c(File)·only(FILE)` by c<File>(APP.location.spitplayer_exe).only(FILE)
         var `c(File)·only(DIRECTORY)` by c<File>(APP.location).only(DIRECTORY)
         var `c(File)·only(ANY)` by c<File>(APP.location).only(ANY)
         var `cn(Font)` by cn<Font>(null)
         val `cvn(Font)` by cvn<Font>(null)
         var `c(Color)` by c<Color>(Color.BLACK)
         var `cn(Color)` by cn<Color>(null)
         val `cv(Color)` by cv<Color>(Color.BLACK)
         val `cvn(Color)` by cvn<Color>(null)
         var `c(LocalTime)` by c<LocalTime>(LocalTime.now())
         var `cn(LocalTime)` by cn<LocalTime>(null)
         val `cv(LocalTime)` by cv<LocalTime>(LocalTime.now())
         val `cvn(LocalTime)` by cvn<LocalTime>(null)
         var `c(LocalDate)` by c<LocalDate>(LocalDate.now())
         var `cn(LocalDate)` by cn<LocalDate>(null)
         val `cv(LocalDate)` by cv<LocalDate>(LocalDate.now())
         val `cvn(LocalDate)` by cvn<LocalDate>(null)
         var `c(LocalDateTime)` by c<LocalDateTime>(LocalDateTime.now())
         var `cn(LocalDateTime)` by cn<LocalDateTime>(null)
         val `cv(LocalDateTime)` by cv<LocalDateTime>(LocalDateTime.now())
         val `cvn(LocalDateTime)` by cvn<LocalDateTime>(null)
         val `cList(Int)` by cList<Int>(1, 2, 3)
         val `cList(Int?)` by cList<Int?>(1, 2, null)
         val `cCheckList(Boolean)` by cCheckList(CheckList.nonNull(type<Boolean>(), listOf(true, false, false), listOf(true, false, false)))
         val `cCheckList(Boolean?)` by cCheckList(CheckList.nullable(type<Boolean?>(), listOf(true, false, null), listOf(true, false, null)))
      }
      onContentChange()
      content.children setToOne form(c).apply { editorOrder = compareByDeclaration }
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