package tester

import java.io.File
import java.lang.Math.PI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javafx.animation.Animation.INDEFINITE
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.PathTransition.OrientationType
import javafx.animation.Transition
import javafx.geometry.Insets
import javafx.geometry.Insets.EMPTY
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.Group
import javafx.scene.control.Separator
import javafx.scene.control.Slider
import javafx.scene.effect.Blend
import javafx.scene.effect.Effect
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcTo
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle
import javafx.scene.shape.ClosePath
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import sp.it.pl.conf.Command
import sp.it.pl.conf.Command.DoNothing
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.DEVELOPMENT
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.FileFilters
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
import sp.it.util.conf.between
import sp.it.util.conf.but
import sp.it.util.conf.c
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cList
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.conf.toConfigurableFx
import sp.it.util.conf.uiOut
import sp.it.util.conf.valuesUnsealed
import sp.it.util.file.div
import sp.it.util.functional.asIs
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.flatMap
import sp.it.util.reactive.map
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncWhile
import sp.it.util.reactive.zip
import sp.it.util.text.nameUi
import sp.it.util.type.type
import sp.it.util.ui.flowPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.em
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year

@Suppress("RemoveExplicitTypeArguments", "RemoveRedundantBackticks", "RemoveExplicitTypeArguments", "RedundantLambdaArrow")
@ExperimentalController("For development")
class Tester(widget: Widget): SimpleController(widget) {
   val content = stackPane()
   val onContentChange = Disposer()

   init {
      root.prefSize = 800.emScaled x 500.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()

      root.lay += vBox(0.0, TOP_CENTER) {
         lay += flowPane(25.emScaled, 25.emScaled) {
            alignment = Pos.CENTER
            padding = Insets(10.emScaled, 10.emScaled, 20.emScaled, 10.emScaled)
            lay += Icon(IconOC.CODE).onClickDo { testInputs() }.withText("Test widget inputs")
            lay += Icon(IconOC.CODE).onClickDo { testFxConfigs() }.withText("Test Fx Configs")
            lay += Icon(IconOC.CODE).onClickDo { testValueObserving() }.withText("Test observing values")
            lay += Icon(IconOC.CODE).onClickDo { testEditors() }.withText("Test Config Editors")
            lay += Icon(IconOC.CODE).onClickDo { testInterpolators() }.withText("Animation Interpolators")
            lay += Icon(IconOC.CODE).onClickDo { testPathShapeTransitions() }.withText("Path/Shape Animations")
            lay += Icon(IconOC.CODE).onClickDo { testCssGradients() }.withText("Test CSS Gradients")
         }
         lay += Separator()
         lay += stackPane {
            padding = Insets(20.emScaled, 10.emScaled, 0.0, 10.emScaled)
            lay += content
         }
      }

      // Create test inputs/outputs
      io.i.create<Any?>("Black hole", null) {}
      io.i.create<Number>("Number", 5) {}
      io.i.create<Number?>("Number?", null) {}
      io.i.create<MutableList<out Number>>("List<out Number>", mutableListOf()) {}
      io.i.create<MutableList<Number>>("List<Number>?", mutableListOf()) {}
      io.i.create<MutableList<in Number>>("List<in Number>", mutableListOf()) {}
      io.o.create<Int>("Int", 5)
      io.o.create<Number?>("Number?", 5)
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
      val r = object: ConfigurableBase<Any?>() {
         val editable by cv(true)
      }
      val c = object: ConfigurableBase<Any?>() {
         var `c(Boolean)` by c<Boolean>(true)
         var `cn(Boolean)` by cn<Boolean>(null)
         val `cv(String)` by cv<String>("text")
         val `cvn(String)` by cvn<String>(null)
         val `cv(String) with autocomplete` by cv<String>("aaa").valuesUnsealed { listOf("a", "aa", "aaa") }
         val `cvn(String) with autocomplete` by cvn<String>(null).valuesUnsealed { listOf("a", "aa", "aaa", null) }
         val `cv(Pos)` by cv<Pos>(TOP_CENTER)
         val `cvn(Pos)` by cvn<Pos>(null)
         val `cv(Key)` by cv<Key>(Key.A)
         val `cvn(Key)` by cvn<Key>(null)
         var `c(Int)` by c<Int>(0)
         var `cn(Int)` by cn<Int>(null)
         val `cv(Int)` by cv<Int>(0)
         val `cvn(Int)` by cvn<Int>(null)
         val `cv(Int)|0-100` by cv<Int>(0).between(0, 100)
         val `cvn(Int)|0-100` by cvn<Int>(null).between(0, 100)
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
         var `cn(Insets)` by cn<Insets>(EMPTY)
         val `cvn(Insets)` by cvn<Insets>(null)
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
         val `cv(Effect)` by cv<Effect>(Blend())
         val `cvn(Effect)` by cvn<Effect>(null)
         val `cv(Command) with value builder` by cvn<Command>(DoNothing).but(Command.parser.toUiStringHelper())
         val `cvn(Command) with value builder` by cvn<Command>(null).but(Command.parser.toUiStringHelper())
         val `cv(FileFilter) with value builder` by cv(FileFilters.filterPrimary).but(FileFilters.parser.toUiStringHelper())
         val `cvn(FileFilter) with value builder` by cvn(null).but(FileFilters.parser.toUiStringHelper())
         val `cList(Int)` by cList<Int>(1, 2, 3)
         val `cList(Int?)` by cList<Int?>(1, 2, null)
         val `cCheckList(Boolean)` by cCheckList(CheckList.nonNull(type<Boolean>(), listOf("a", "b", "c"), listOf(true, false, false)))
         val `cCheckList(Boolean?)` by cCheckList(CheckList.nullable(type<Boolean?>(), listOf("a", "b", null), listOf(true, false, null)))
      }
      onContentChange()
      content.children setToOne vBox {
         spacing = 1.em.emScaled

         lay += form(r).apply {
            minPrefMaxHeight = 6.em.emScaled
         }
         lay += Separator().apply {
            minPrefMaxWidth = 10.em.emScaled
         }
         lay += form(c).apply {
            isEditable syncFrom r.editable
            editorOrder = compareByDeclaration
         }
      }
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
         "x^-4 (sqrt(sqrt(x)))" to { it -> sqrt(sqrt(it)) },
         "log2 N(20)" to { it -> log2(1.0 + (it*1024*1024))/20.0 },
         "log2 N(10)" to { it -> log2(1.0 + (it*1024))/10.0 },
         "log2 N(4)" to { it -> log2(1.0 + (it*16))/4.0 },
         "log2 N(2)" to { it -> log2(1.0 + (it*4))/2.0 },
         "exp2 N(20)" to { it -> 20.0.pow(10*(it - 1)) },
         "exp2 N(10)" to { it -> 10.0.pow(10*(it - 1)) },
         "exp2 N(4)" to { it -> 4.0.pow(10*(it - 1)) },
         "exp2 N(2)" to { it -> 2.0.pow(10*(it - 1)) },
         "sin" to { it -> sin(PI/2*it) },
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

   fun testPathShapeTransitions() {
      fun createEllipsePath(centerX: Double, centerY: Double, radiusX: Double, radiusY: Double, rotate: Double) = Path().apply {
         elements += MoveTo(centerX - radiusX, centerY - radiusY)
         elements += ArcTo().apply {
            this.x = centerX - radiusX + 1
            this.y = centerY - radiusY
            this.isSweepFlag = false
            this.isLargeArcFlag = true
            this.radiusX = radiusX
            this.radiusY = radiusY
            this.xAxisRotation = rotate
         }
         elements += ClosePath()
         stroke = Color.DODGERBLUE
         strokeDashArray.setAll(5.0, 5.0)
      }

      onContentChange()
      content.children setToOne scrollPane {
         content = vBox {
            lay += Group().apply {
               children += Rectangle(0.0, 0.0, 220.0, 220.0)
               children += Rectangle(0.0, 0.0, 10.0, 10.0).apply {
                  arcHeight = 0.0
                  arcWidth = 0.0
                  fill = Color.GREEN
               }
               children += createEllipsePath(210.0, 110.0, 100.0, 80.0, 0.0)

               PathTransition(8.seconds, children[2].asIs(), children[1].asIs()).apply {
                  orientation = OrientationType.ORTHOGONAL_TO_TANGENT
                  interpolator = Interpolator.LINEAR
                  cycleCount = INDEFINITE
                  isAutoReverse = false
                  play()
                  onClose += ::stop
               }
            }
            lay += stackPane {
               prefSize = 500.x2
               lay += Group().apply {
                  children += Rectangle().apply {
                     fill = Color.TRANSPARENT
                     width = 400.0
                     height = 400.0
                  }
                  children += Circle(100.0, Color.AQUA).apply {
                     styleClass += "xxx"
                     centerX = 200.0
                     centerY = 200.0
                     clip = Group().apply {
                        children += Rectangle().apply {
                           fill = Color.TRANSPARENT
                           width = 400.0
                           height = 400.0
                        }
                        children += Arc().apply {
                           styleClass += "yyy"
                           this.type = ArcType.ROUND
                           this.centerX = 200.0
                           this.centerY = 200.0
                           this.radiusX = 100.0
                           this.radiusY = 100.0
                           this.startAngle = 0.0

                           val a = anim(3.seconds) { length = 360.0*it }
                           a.delay(0.millis)
                           a.interpolator = Interpolator.LINEAR
                           a.cycleCount = Transition.INDEFINITE
                           a.playOpen()
                           onClose += a::stop
                        }
                     }
                  }
               }
            }
         }
      }
   }

   fun testCssGradients() {
      onContentChange()
      content.children setToOne scrollPane {
         content = vBox {
            lay += stackPane {
               styleClass += "test-gradient"
            }
         }
      }
   }

   fun testValueObserving() {
      onContentChange()

      val c = object: ConfigurableBase<Any?>() {
         val aa by cv(true).def(name = "Select 'a' if true else 'b + c'")
         val a by cv(1.0).between(1, 10)
         val b by cv(2.0).between(1, 10)
         val c by cv(3.0).between(1, 10)
         val consumer1 = v(0.0)
         val consumer2 = v(0.0)
      }

      c.consumer1 syncFrom c.aa.flatMap {
         when (it) {
            true -> c.a
            false -> c.b zip c.c map { (td, sd) -> sd + td }
         }
      }

      c.aa syncWhile { s ->
         when (s) {
            true -> c.consumer2 syncFrom c.a
            false -> c.b syncWhile { td -> c.c sync { sd -> c.consumer2.value = sd + td } }
         }
      }

      content.children setToOne vBox {
         lay += label("Complex value observation.\nThe below values should always be the same. ") {
            isWrapText = true
         }
         lay += label()
         lay += label {
            isWrapText = true
            textProperty() syncFrom c.consumer1.map { "  Map/flatMap based. Tests map(), flatMap(), zip()\n    Value: $it" }
         }
         lay += label()
         lay += label {
            isWrapText = true
            textProperty() syncFrom c.consumer2.map { "  Subscription based. Tests subscription nesting.\n    Value: $it" }
         }
         lay += form(c)
      }
   }

   companion object: WidgetCompanion {
      override val name = TESTER_NAME
      override val description = "Provides facilities & demos for testing and development"
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