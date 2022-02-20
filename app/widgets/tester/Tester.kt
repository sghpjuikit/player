package tester

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.lang.Math.PI
import java.lang.Math.random
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javafx.animation.Interpolator
import javafx.animation.Interpolator.LINEAR
import javafx.animation.PathTransition
import javafx.animation.PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT
import javafx.animation.Transition.INDEFINITE
import javafx.geometry.Insets
import javafx.geometry.Insets.EMPTY
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Side.RIGHT
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.control.Separator
import javafx.scene.control.Slider
import javafx.scene.effect.Blend
import javafx.scene.effect.Effect
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Color.AQUA
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.DODGERBLUE
import javafx.scene.paint.Color.GRAY
import javafx.scene.paint.Color.ORANGE
import javafx.scene.paint.Color.RED
import javafx.scene.paint.Color.TRANSPARENT
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
import sp.it.pl.layout.ExperimentalController
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.FileFilters
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.IconUN
import sp.it.pl.main.Key
import sp.it.pl.main.Widgets.TESTER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.LabelWithIcon
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ConfigPane.Companion.compareByDeclaration
import sp.it.pl.ui.pane.ConfigPane.Layout.MINI
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.action.ActionManager
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.interpolator.BackInterpolator
import sp.it.util.animation.interpolator.CircularInterpolator
import sp.it.util.animation.interpolator.CubicInterpolator
import sp.it.util.animation.interpolator.EasingMode.EASE_IN
import sp.it.util.animation.interpolator.ElasticInterpolator
import sp.it.util.animation.interpolator.ExponentialInterpolator
import sp.it.util.animation.interpolator.QuadraticInterpolator
import sp.it.util.animation.interpolator.QuarticInterpolator
import sp.it.util.animation.interpolator.QuinticInterpolator
import sp.it.util.animation.interpolator.SineInterpolator
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runIoParallel
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
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.conf.toConfigurableFx
import sp.it.util.conf.uiOut
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.flatMap
import sp.it.util.reactive.map
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncWhile
import sp.it.util.reactive.zip
import sp.it.util.text.nameUi
import sp.it.util.type.type
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.maxSize
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.minSize
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassAdd
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
   val groups = listOf(
      Group(IconOC.CODE, "Test widget inputs") { testInputs() },
      Group(IconOC.CODE, "Test Fx Configs") { testFxConfigs() },
      Group(IconOC.CODE, "Test observing values") { testValueObserving() },
      Group(IconOC.CODE, "Test Config Editors") { testEditors() },
      Group(IconOC.CODE, "Animation Interpolators") { testInterpolators() },
      Group(IconOC.CODE, "Path/ShapeAnimations") { testPathShapeTransitions() },
      Group(IconOC.CODE, "Test CSS Gradients") { testCssGradients() },
      Group(IconOC.CODE, "Test CSS Borders") { testCssBorders() },
      Group(IconOC.CODE, "Test Tasks") { testTasks() },
      Group(IconOC.CODE, "Test Mouse events") { testMouseEvents() }
   )
   val groupSelected by cv("").noUi()

   init {
      root.prefSize = 500.emScaled x 700.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()
      groupSelected.sync { s -> groups.forEach { it.select(it.name==s) } }


      root.prefSize = 400.emScaled x 400.emScaled
      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            padding = Insets(50.emScaled, 0.0, 50.emScaled, 0.0)
            minWidth = 220.emScaled
            lay += scrollPane {
               isFitToHeight = true
               isFitToWidth = true
               vbarPolicy = NEVER
               hbarPolicy = NEVER
               content = vBox(0.0, CENTER_LEFT) {
                  lay += groups.map { it.label }
               }
            }
         }
         lay += separator(VERTICAL) { maxHeight = 200.emScaled }
         lay(ALWAYS) += content
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
      val c = Icon(IconFA.PLAY)
         .onClickDo { APP.ui.toggleLayoutMode() }
         .withText(RIGHT, CENTER_LEFT, "Trigger layout mode (${ActionManager.keyManageLayout.nameUi}) to test this widget's inputs")
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
      content.children setToOne fittingScrollPane {
         content = form(c.toConfigurableFx())
      }
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
         val `cv(Byte)` by cv<Byte>(0)
         val `cv(UByte)` by cv<UByte>(0u)
         val `cv(Short)` by cv<Short>(0)
         val `cv(UShort)` by cv<UShort>(0u)
         var `c(Int)` by c<Int>(0)
         var `cn(Int)` by cn<Int>(null)
         val `cv(Int)` by cv<Int>(0)
         val `cvn(Int)` by cvn<Int>(null)
         val `cv(Int)|0-100` by cv<Int>(0).between(0, 100)
         val `cvn(Int)|0-100` by cvn<Int>(null).between(0, 100)
         val `cv(UInt)` by cv<UInt>(0u)
         val `cv(ULong)` by cv<ULong>(0uL)
         val `cv(Float)` by cv<Float>(0f)
         val `cv(Double)` by cv<Double>(0.0)
         val `cv(Double)|0-100` by cv<Double>(0.0).between(0.0, 100.0)
         val `cvn(Double)|0-100` by cvn<Double>(null).between(0.0, 100.0)
         val `cv(BigInteger)` by cv<BigInteger>(BigInteger.ZERO)
         val `cv(BigDecimal)` by cv<BigDecimal>(BigDecimal.ZERO)
         val `cv(BigInteger)|0-100` by cv<BigInteger>(BigInteger.ZERO).between(0.0, 100.0)
         val `cv(BigDecimal)|0-100` by cv<BigDecimal>(BigDecimal.ZERO).between(0.0, 100.0)
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
         var `c(Color)` by c<Color>(BLACK)
         var `cn(Color)` by cn<Color>(null)
         val `cv(Color)` by cv<Color>(BLACK)
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
      content.children setToOne vBox(1.em.emScaled) {
         lay += form(r).apply {
            editorUi.value = MINI
            minPrefMaxHeight = 6.em.emScaled
         }
         lay += Separator().apply {
            minPrefMaxWidth = 10.em.emScaled
         }
         lay += form(c).apply {
            editorUi.value = MINI
            isEditable syncFrom r.editable
            editorOrder = compareByDeclaration
         }
      }
   }

   fun testInterpolators() {
      fun Interpolator.toF(): (Double) -> Double = { this.interpolate(0.0, 1.0, it) }
      val interpolators = mapOf<String, (Double) -> Double>(
         "javaFx: LINEAR" to LINEAR.toF(),
         "javaFx: EASE_BOTH" to Interpolator.EASE_BOTH.toF(),
         "javaFx: EASE_IN" to Interpolator.EASE_IN.toF(),
         "javaFx: EASE_OUT" to Interpolator.EASE_OUT.toF(),
         "spit: sine" to SineInterpolator(EASE_IN).toF(),
         "spit: cubic" to CubicInterpolator(EASE_IN).toF(),
         "spit: quintic" to QuinticInterpolator(EASE_IN).toF(),
         "spit: circular" to CircularInterpolator(EASE_IN).toF(),
         "spit: quadratic" to QuadraticInterpolator(EASE_IN).toF(),
         "spit: quartic" to QuarticInterpolator(EASE_IN).toF(),
         "spit: exponential" to ExponentialInterpolator(EASE_IN).toF(),
         "spit: back" to BackInterpolator(EASE_IN).toF(),
         "spit: elastic" to ElasticInterpolator(EASE_IN).toF(),
         "math: x" to { it -> it },
         "math: x^2" to { it -> it*it },
         "math: x^3" to { it -> it*it*it },
         "math: x^4" to { it -> it*it*it*it },
         "math: x^-2 (sqrt(x))" to { it -> sqrt(it) },
         "math: x^-4 (sqrt(sqrt(x)))" to { it -> sqrt(sqrt(it)) },
         "math: log2 N(20)" to { it -> log2(1.0 + (it*1024*1024))/20.0 },
         "math: log2 N(10)" to { it -> log2(1.0 + (it*1024))/10.0 },
         "math: log2 N(4)" to { it -> log2(1.0 + (it*16))/4.0 },
         "math: log2 N(2)" to { it -> log2(1.0 + (it*4))/2.0 },
         "math: exp2 N(20)" to { it -> 20.0.pow(10*(it - 1)) },
         "math: exp2 N(10)" to { it -> 10.0.pow(10*(it - 1)) },
         "math: exp2 N(4)" to { it -> 4.0.pow(10*(it - 1)) },
         "math: exp2 N(2)" to { it -> 2.0.pow(10*(it - 1)) },
         "math: sin" to { it -> sin(PI/2*it) },
      )
      onContentChange()
      content.children setToOne fittingScrollPane {
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
         stroke = DODGERBLUE
         strokeDashArray.setAll(5.0, 5.0)
      }

      onContentChange()
      content.children setToOne fittingScrollPane {
         content = vBox(0.0, CENTER) {
            lay += stackPane {
               lay += Group().apply {
                  children += Rectangle(0.0, 0.0, 220.0, 220.0).apply {
                     fill = TRANSPARENT
                  }
                  children += Rectangle(0.0, 0.0, 10.0, 10.0).apply {
                     arcHeight = 0.0
                     arcWidth = 0.0
                     fill = AQUA
                  }
                  children += createEllipsePath(210.0, 110.0, 100.0, 80.0, 0.0)

                  PathTransition(8.seconds, children[2].asIs(), children[1].asIs()).apply {
                     orientation = ORTHOGONAL_TO_TANGENT
                     interpolator = LINEAR
                     cycleCount = INDEFINITE
                     isAutoReverse = false
                     play()
                     onContentChange += ::stop
                     onClose += ::stop
                  }
               }
            }
            lay += stackPane {
               prefSize = 500.x2
               lay += Group().apply {
                  children += Rectangle().apply {
                     fill = TRANSPARENT
                     width = 400.0
                     height = 400.0
                  }
                  children += Circle(100.0, AQUA).apply {
                     styleClass += "xxx"
                     centerX = 200.0
                     centerY = 200.0
                     clip = Group().apply {
                        children += Rectangle().apply {
                           fill = TRANSPARENT
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
                           a.interpolator = LINEAR
                           a.cycleCount = INDEFINITE
                           a.playOpen()
                           onContentChange += a::stop
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
      content.children setToOne fittingScrollPane {
         content = vBox(0.0, CENTER) {
            lay += stackPane {
               maxSize = 100.emScaled.x2
               minSize = 100.emScaled.x2
               prefSize = 100.emScaled.x2
               styleClass += "test-gradient"
            }
         }
      }
   }

   fun testCssBorders() {
      onContentChange()
      content.children setToOne fittingScrollPane {
         content = vBox(5.emScaled, CENTER) {
            styleClass += "test-buttons"

            lay += stackPane { styleClass += "test-button-1" }
            lay += stackPane { styleClass += "test-button-2" }
            lay += stackPane { styleClass += "test-button-3" }
            lay += stackPane { styleClass += "test-button-4" }
            lay += stackPane { styleClass += "test-button-5" }
            lay += stackPane { styleClass += "test-button-6" }
         }
      }
   }

   fun testTasks() {
      onContentChange()

      fun task(name: String, block: () -> Any?) = fut().thenWait(5.seconds).then { block() }.withAppProgress(name)

      content.children setToOne fittingScrollPane {
         content = vBox(0.0, CENTER_LEFT) {
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test success") {} }
               .withText(RIGHT, CENTER_LEFT, "Run long running task that succeeds")
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test failure (exception)") { fail { "Test error message" } } }
               .withText(RIGHT, CENTER_LEFT, "Run long running task that fails (exception)")
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test failure (custom error)") { Try.error("Test error value") } }
               .withText(RIGHT, CENTER_LEFT, "Run long running task that fails (custom error)")
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test failure (custom list with errors)") { runIoParallel(2, (1..100).toList()) { if (random()>0.5) it else fail { "error" } } } }
               .withText(RIGHT, CENTER_LEFT, "Run long running task that fails (custom error)")
         }
      }
   }

   fun testMouseEvents() {
      onContentChange()

      fun <T: Node> T.mt(): T = apply { isMouseTransparent = true }
      fun Label.bold(): Label = apply { styleclassAdd("text-weight-bold") }
      fun circle(radius: Double, fill: Color, block: Circle.() -> Unit = {}): Circle = Circle(radius, fill).apply(block)
      fun circleTop(): Circle = circle(40.emScaled, RED) { hoverProperty() sync { fill = if (it) ORANGE else RED } }
      fun labelTop(text: String): Label = label(text) { isWrapText = true; textAlignment = TextAlignment.CENTER; textFill = BLACK }.bold().mt()
      
      content.children setToOne fittingScrollPane {
         content = vBox(10.emScaled, CENTER_LEFT) {
            lay += vBox {
               lay += label("Test 1: Hover over black and red circle")
               lay += label("Test 2: Start drag in red circle and move outside black circle")
            }
            lay += label("Implemented using onHoverOrDrag():")
            lay += hBox(10.emScaled) {
               padding = Insets(0.0, 0.0, 0.0, 25.emScaled)
               lay += stackPane {
                  isPickOnBounds = false
                  val c = circle(50.emScaled, BLACK)
                  onHoverOrDrag { c.fill = if (it) GRAY else BLACK }
                  lay += c
                  lay += circleTop()
                  lay += labelTop("within")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     onHoverOrDrag { fill = if (it) GRAY else BLACK }
                  }
                  lay += circleTop()
                  lay += labelTop("above")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     onHoverOrDrag { fill = if (it) GRAY else BLACK }
                  }
                  lay += circleTop().mt()
                  lay += labelTop("above\n(no mouse)")
               }
            }
            lay += label("Implemented using MOUSE_ENTERED/MOUSE_EXITED events:")
            lay += hBox(10.emScaled) {
               padding = Insets(0.0, 0.0, 0.0, 25.emScaled)
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     onEventDown(MOUSE_EXITED) { fill = BLACK }
                     onEventDown(MOUSE_ENTERED) { fill = GRAY }
                  }
                  lay += circleTop()
                  lay += labelTop("above")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  val c = circle(50.emScaled, BLACK)
                  onEventDown(MOUSE_EXITED) { c.fill = BLACK }
                  onEventDown(MOUSE_ENTERED) { c.fill = GRAY }
                  lay += c
                  lay += circleTop()
                  lay += labelTop("within")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     onEventDown(MOUSE_EXITED) { fill = BLACK }
                     onEventDown(MOUSE_ENTERED) { fill = GRAY }
                  }
                  lay += circleTop().mt()
                  lay += labelTop("above\n(no mouse)")
               }
            }
            lay += label("Implemented using hoverProperty():")
            lay += hBox(10.emScaled) {
               padding = Insets(0.0, 0.0, 0.0, 25.emScaled)
               lay += stackPane {
                  isPickOnBounds = false
                  val c = circle(50.emScaled, BLACK)
                  hoverProperty() sync { c.fill = if (it) GRAY else BLACK }
                  lay += c
                  lay += circleTop()
                  lay += labelTop("within")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     hoverProperty() sync { fill = if (it) GRAY else BLACK }
                  }
                  lay += circleTop()
                  lay += labelTop("above")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     hoverProperty() sync { fill = if (it) GRAY else BLACK }
                  }
                  lay += circleTop().mt()
                  lay += labelTop("above\n(no mouse)")
               }
            }
         }
      }
   }

   fun testValueObserving() {
      onContentChange()

      val c = object: ConfigurableBase<Any?>() {
         val d by cv(true)
         val a by cv(1.0).between(1, 10)
         val b by cv(2.0).between(1, 10)
         val c by cv(3.0).between(1, 10)
         val consumer1 = v(0.0)
         val consumer2 = v(0.0)
      }

      c.consumer1 syncFrom c.d.flatMap {
         when (it) {
            true -> c.a
            false -> c.b zip c.c map { (td, sd) -> sd + td }
         }
      }

      c.d syncWhile { s ->
         when (s) {
            true -> c.consumer2 syncFrom c.a
            false -> c.b syncWhile { td -> c.c sync { sd -> c.consumer2.value = sd + td } }
         }
      }

      content.children setToOne fittingScrollPane {
         content = vBox(0.0, CENTER_LEFT) {
            lay += label("Observable chains.")
            lay += label("The below values `if (d) a else (b + c)` should be the same.")
            lay += label()
            lay += label("Map/flatMap based. Tests map(), flatMap(), zip().")
            lay += label { textProperty() syncFrom c.consumer1.map { "    Value: $it" } }
            lay += label()
            lay += label("Subscription based. Tests subscription nesting.")
            lay += label { textProperty() syncFrom c.consumer2.map { "    Value: $it" } }
            lay += label()
            lay += form(c).apply { editorUi.value = MINI }
         }
      }
   }

   inner class Group(glyph: GlyphIcons, val name: String, val block: () -> Unit) {
      val label = LabelWithIcon(glyph, name).apply {
         icon.onClickDo { groupSelected.value = name }
      }
      fun select(s: Boolean) {
         label.select(s)
         if (s) block()
      }
   }

   companion object: WidgetCompanion {
      override val name = TESTER_NAME
      override val description = "Provides facilities & demos for testing and development"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = listOf<ShortcutPane.Entry>()

      fun fittingScrollPane(block: ScrollPane.() -> Unit) = scrollPane { isFitToHeight = true; isFitToWidth = true; block() }
   }
}