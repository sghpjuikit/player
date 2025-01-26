package tester

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.lang.Math.random
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import javafx.animation.Interpolator.LINEAR
import javafx.animation.PathTransition
import javafx.animation.PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT
import javafx.animation.Transition.INDEFINITE
import javafx.geometry.Insets
import javafx.geometry.Insets.EMPTY
import javafx.geometry.Orientation
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Side
import javafx.geometry.Side.RIGHT
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.control.Slider
import javafx.scene.effect.Blend
import javafx.scene.effect.Effect
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.Priority.ALWAYS
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.javafx.awaitPulse
import sp.it.pl.conf.Command
import sp.it.pl.conf.Command.DoNothing
import sp.it.pl.layout.ContainerBi
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.emptyWidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppProgress
import sp.it.pl.main.Double01
import sp.it.pl.main.FileFilter
import sp.it.pl.main.FileFilters
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.IconUN
import sp.it.pl.main.Key
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.Widgets.TESTER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.errorNotify
import sp.it.pl.main.listBox
import sp.it.pl.main.listBoxRow
import sp.it.pl.main.reportFor
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.item_node.ConfigEditor
import sp.it.pl.ui.item_node.ValueToggleButtonGroupCE
import sp.it.pl.ui.objects.ClipboardViewer
import sp.it.pl.ui.objects.KeyboardEventViewer
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ConfigPane.Companion.compareByDeclaration
import sp.it.pl.ui.pane.ConfigPane.Layout.MINI
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.Util
import sp.it.util.Util.pyth
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.action.ActionManager
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Interpolators.Companion.easeBoth
import sp.it.util.animation.Anim.Interpolators.Companion.easeIn
import sp.it.util.animation.Anim.Interpolators.Companion.easeOut
import sp.it.util.animation.Anim.Interpolators.Companion.fxDiscrete
import sp.it.util.animation.Anim.Interpolators.Companion.fxEaseBoth
import sp.it.util.animation.Anim.Interpolators.Companion.fxEaseIn
import sp.it.util.animation.Anim.Interpolators.Companion.fxEaseOut
import sp.it.util.animation.Anim.Interpolators.Companion.fxLinear
import sp.it.util.animation.Anim.Interpolators.Companion.geomBack
import sp.it.util.animation.Anim.Interpolators.Companion.geomCircular
import sp.it.util.animation.Anim.Interpolators.Companion.geomElastic
import sp.it.util.animation.Anim.Interpolators.Companion.geomExponential
import sp.it.util.animation.Anim.Interpolators.Companion.geomRev
import sp.it.util.animation.Anim.Interpolators.Companion.geomSine
import sp.it.util.animation.Anim.Interpolators.Companion.inv
import sp.it.util.animation.Anim.Interpolators.Companion.mathSine
import sp.it.util.animation.Anim.Interpolators.Companion.math_exp2_N10
import sp.it.util.animation.Anim.Interpolators.Companion.math_exp2_N2
import sp.it.util.animation.Anim.Interpolators.Companion.math_exp2_N20
import sp.it.util.animation.Anim.Interpolators.Companion.math_exp2_N4
import sp.it.util.animation.Anim.Interpolators.Companion.math_log2_N10
import sp.it.util.animation.Anim.Interpolators.Companion.math_log2_N2
import sp.it.util.animation.Anim.Interpolators.Companion.math_log2_N20
import sp.it.util.animation.Anim.Interpolators.Companion.math_log2_N4
import sp.it.util.animation.Anim.Interpolators.Companion.math_x
import sp.it.util.animation.Anim.Interpolators.Companion.math_xp2
import sp.it.util.animation.Anim.Interpolators.Companion.math_xp3
import sp.it.util.animation.Anim.Interpolators.Companion.math_xp4
import sp.it.util.animation.Anim.Interpolators.Companion.math_xs2
import sp.it.util.animation.Anim.Interpolators.Companion.math_xs4
import sp.it.util.animation.Anim.Interpolators.Companion.rev
import sp.it.util.animation.Anim.Interpolators.Companion.sym
import sp.it.util.animation.Anim.Interpolators.Companion.toF
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.async.runIoParallel
import sp.it.util.collections.setToOne
import sp.it.util.conf.CheckList
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.ANY
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.Constraint.ValueSealedToggle
import sp.it.util.conf.PropertyConfig
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
import sp.it.util.conf.uiRadio
import sp.it.util.conf.uiToggle
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.dev.failCase
import sp.it.util.dev.printIt
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.math.abs
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.flatMap
import sp.it.util.reactive.into
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncWhile
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.reactive.zip3
import sp.it.util.text.nameUi
import sp.it.util.type.type
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.maxSize
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.minSize
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.ui.onHoverOrInDrag
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassAdd
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.em
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year
import sp.it.util.animation.Anim.Companion.mapTo01
import sp.it.util.collections.ObservableListRO
import sp.it.util.conf.def
import sp.it.util.conf.values
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.ui.center
import sp.it.util.ui.centre
import sp.it.util.ui.size

@Suppress("RemoveExplicitTypeArguments", "RemoveRedundantBackticks", "RemoveExplicitTypeArguments")
class Tester(widget: Widget): SimpleController(widget) {
   val content = stackPane()
   val onContentChange = Disposer()
   val groups = listOf(
      Group(IconOC.CODE, "Test widget inputs") { testInputs() },
      Group(IconOC.CODE, "Test Fx Configs") { testFxConfigs() },
      Group(IconOC.CODE, "Test errors") { testErrors() },
      Group(IconOC.CODE, "Test observing values") { testValueObserving() },
      Group(IconOC.CODE, "Test Containers") { testContainers() },
      Group(IconOC.CODE, "Test Config Editors") { testEditors() },
      Group(IconOC.CODE, "Animation Interpolators") { testInterpolators() },
      Group(IconOC.CODE, "Path/ShapeAnimations") { testPathShapeTransitions() },
      Group(IconOC.CODE, "Test CSS Gradients") { testCssGradients() },
      Group(IconOC.CODE, "Test CSS Borders") { testCssBorders() },
      Group(IconOC.CODE, "Test Tasks") { testTasks() },
      Group(IconOC.CODE, "Test Mouse events") { testMouseEvents() },
      Group(IconOC.CODE, "Test Keyboard events") { testKeyEvents() },
      Group(IconOC.CODE, "Test Clipboard") { testClipboard() }
   )
   val groupSelected by cv(groups.first().name).noUi()

   init {
      root.prefSize = 500.emScaled x 700.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()

      root.prefSize = 800.emScaled x 400.emScaled
      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            padding = Insets(50.emScaled, 0.0, 50.emScaled, 0.0)
            minWidth = 220.emScaled
            lay += scrollPane {
               isFitToHeight = true
               isFitToWidth = true
               vbarPolicy = NEVER
               hbarPolicy = NEVER
               content = listBox {
                  lay += groups.map { it.row }
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

      groupSelected sync { s -> groups.forEach { it.select(it.name==s) } }
   }

   override fun close() {
      onContentChange()
      content.children.clear()
   }

   override fun focus() {
      if (!root.isFocusWithin)
         groups.forEach { it.select(it.name==groupSelected.value) }
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
         content = form(c.toConfigurableFx().apply {getConfigs().map { it.nameUi }.joinToString { it }.printIt() })
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
         val `cv(Boolean) - toggle` by cv<Boolean>(true).uiToggle()
         val `cvn(Boolean) - toggle` by cvn<Boolean>(null).uiToggle()
         val `cv(Boolean) - radio` by cv<Boolean>(true).uiRadio()
         val `cvn(Boolean) - radio` by cvn<Boolean>(null).uiRadio()
         val `cv(String)` by cv<String>("text")
         val `cvn(String)` by cvn<String>(null)
         val `cv(String) with autocomplete` by cv<String>("aaa").valuesUnsealed { listOf("a", "aa", "aaa") }
         val `cvn(String) with autocomplete` by cvn<String>(null).valuesUnsealed { listOf("a", "aa", "aaa", null) }
         val `cv(Side)` by cv<Side>(Side.LEFT)
         val `cvn(Side)` by cvn<Side>(null)
         val `cv(Side) - toggle` by cv<Side>(Side.LEFT).uiToggle()
         val `cvn(Side) - toggle` by cvn<Side>(null).uiToggle()
         val `cv(Side) - radio` by cv<Side>(Side.LEFT).uiRadio()
         val `cvn(Side) - radio` by cvn<Side>(null).uiRadio()
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
         var `cn(Icon)` by cn<GlyphIcons>(null)
         val `cvn(Icon)` by cvn<GlyphIcons>(null)
         val `cvn(TextAlignment)` by cvn<TextAlignment>(null)
         val `cvn(Orientation)` by cvn<Orientation>(null)
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
         var `c(ZonedDateTime)` by c<ZonedDateTime>(ZonedDateTime.now())
         var `cn(ZonedDateTime)` by cn<ZonedDateTime>(null)
         val `cv(ZonedDateTime)` by cv<ZonedDateTime>(ZonedDateTime.now())
         val `cvn(ZonedDateTime)` by cvn<ZonedDateTime>(null)
         val `cv(Effect)` by cv<Effect>(Blend())
         val `cvn(Effect)` by cvn<Effect>(null)
         val `cv(Command) with value builder` by cv<Command>(DoNothing).but(Command.parser.toUiStringHelper())
         val `cvn(Command) with value builder` by cvn<Command>(null).but(Command.parser.toUiStringHelper())
         val `cv(FileFilter) with value builder` by cv<FileFilter>(FileFilters.filterPrimary).but(FileFilters.parser.toUiStringHelper())
         val `cvn(FileFilter) with value builder` by cvn<FileFilter>(null).but(FileFilters.parser.toUiStringHelper())
         val `cList(Int)` by cList<Int>(1, 2, 3)
         val `cList(Int?)` by cList<Int?>(1, 2, null)
         val `cCheckList(Boolean)` by cCheckList(CheckList.nonNull(type<String>(), listOf("a", "b", "c"), listOf(true, false, false)))
            .def(name = "cCheckList(Boolean)", info = "Checklist with 2-valued selection")
         val `cCheckList(Boolean?)` by cCheckList(CheckList.nullable(type<String?>(), listOf("a", "b", null), listOf(true, false, null)))
            .def(name = "cCheckList(Boolean?)", info = "Checklist with 3-valued selection")
         val `cv_sealed_observable` by cv("a").values(`cCheckList(Boolean)`.selectedObservable(true))
            .def(name = "cv_sealed_observable", info = "The list of possible values is fixed at the time of creation to the source `cCheckList(Boolean)`")
         val `cv_sealed_observable_lambda` by cv("a").values { `cCheckList(Boolean)`.selected(true) }
            .def(name = "cv_sealed_observable_lambda", info = "The list of possible values is fixed at the time of creation to the source `cCheckList(Boolean)`")
         val `cv_unsealed_observable` by cv("a").valuesUnsealed(`cCheckList(Boolean)`.selectedObservable(true))
            .def(name = "cv_unsealed_observable", info = "The list of suggested values changes depending on the source `cCheckList(Boolean)`")
         val `cv_unsealed_observable_lambda` by cv("a").valuesUnsealed { `cCheckList(Boolean)`.selected(true) }
            .def(name = "cv_unsealed_observable_lambda", info = "The list of suggested values changes depending on the source `cCheckList(Boolean)`")
      }
      onContentChange()
      content.children setToOne vBox(1.em.emScaled) {
         lay += stackPane {
            styleClass += "h2p"
            lay += form(r).apply {
               editorUi.value = MINI
            }
         }
         lay += stackPane {
            lay += separator { minPrefMaxWidth = 10.em.emScaled }
         }
         lay += form(c).apply {
            editorUi.value = MINI
            isEditable syncFrom r.editable
            editorOrder = compareByDeclaration
         }
      }
   }

   fun testInterpolators() {
      val easing = v("None")
      val reverse = v(false)
      val inverse = v(false)
      val symmetric = v(false)
      val interpolators = mapOf<String, (Double) -> Double>(
         "javaFx: DISCRETE" to fxDiscrete.toF(),
         "javaFx: LINEAR" to fxLinear.toF(),
         "javaFx: EASE_IN" to fxEaseIn.toF(),
         "javaFx: EASE_OUT" to fxEaseOut.toF(),
         "javaFx: EASE_BOTH" to fxEaseBoth.toF(),
         "geom: reverse" to geomRev,
         "geom: sine" to geomSine,
         "geom: circular" to geomCircular,
         "geom: exponential" to geomExponential,
         "geom: back" to geomBack(),
         "geom: elastic" to geomElastic(),
         "math: x" to math_x,
         "math: x⁺²" to math_xp2,
         "math: x⁺³" to math_xp3,
         "math: x⁺⁴" to math_xp4,
         "math: x⁻²" to math_xs2,
         "math: x⁻⁴" to math_xs4,
         "math: log₂(20)" to math_log2_N20,
         "math: log₂(10)" to math_log2_N10,
         "math: log₂(4)" to math_log2_N4,
         "math: log₂(2)" to math_log2_N2,
         "math: exp₂(20)" to math_exp2_N20,
         "math: exp₂(10)" to math_exp2_N10,
         "math: exp₂(4)" to math_exp2_N4,
         "math: exp₂(2)" to math_exp2_N2,
         "math: sine" to mathSine,
      )
      onContentChange()
      content.children setToOne vBox(1.em.emScaled) {
         lay += vBox(null, CENTER) {
            styleClass += "h2p"
            lay += ConfigEditor.create(PropertyConfig(type<Boolean>(), "Reverse", ConfigDef(), setOf(), reverse, reverse.value, ""))
               .net { c -> hBox(null, CENTER) { lay += c.buildLabel(); lay += c.editor } }
            lay += ConfigEditor.create(PropertyConfig(type<Boolean>(), "Inverse", ConfigDef(), setOf(), inverse, inverse.value, ""))
               .net { c -> hBox(null, CENTER) { lay += c.buildLabel(); lay += c.editor } }
            lay += ConfigEditor.create(PropertyConfig(type<Boolean>(), "Symmetric", ConfigDef(), setOf(), symmetric, symmetric.value, ""))
               .net { c -> hBox(null, CENTER) { lay += c.buildLabel(); lay += c.editor } }
            lay += ValueToggleButtonGroupCE(PropertyConfig(type<String>(), "Ease", ConfigDef(), setOf(ValueSealedToggle), easing, easing.value, ""), listOf("None", "In", "Out", "Both"), {}).run {
               editor.alignment = CENTER
               editor
            }
         }
         lay += stackPane {
            lay += separator { minPrefMaxWidth = 10.em.emScaled }
         }
         lay += fittingScrollPane {
            content = vBox {
               lay += interpolators.map { (name, interpolator) ->
                  vBox {
                     padding = Insets(5.emScaled)
                     lay += label(name)
                     lay += hBox(15.emScaled, CENTER_RIGHT) {
                        lay(ALWAYS) += Slider().apply {
                           min = 0.0
                           max = 1.0
                        }
                        lay += Icon(IconFA.STICKY_NOTE, 25.0).apply { isFocusTraversable = false; isMouseTransparent = true }
                        lay += Icon(IconFA.STICKY_NOTE, 25.0).apply { isFocusTraversable = false; isMouseTransparent = true }
                        lay += Icon(IconFA.STICKY_NOTE, 25.0).apply { isFocusTraversable = false; isMouseTransparent = true }

                        anim(1.seconds) {
                           lookupChildAt<Slider>(0).value = it
                           lookupChildAt<Icon>(1).opacity = it
                           lookupChildAt<Icon>(2).setScaleXY(it)
                           lookupChildAt<Icon>(3).rotate = 180*it
                        }.apply {
                           reverse zip inverse zip2 symmetric zip3 easing sync { (r, i, s, e) ->
                              stop()
                              intpl(
                                 interpolator
                                    .net { if (r) it.rev() else it }
                                    .net { if (i) it.inv() else it }
                                    .net { if (s) it.sym() else it }
                                    .net { when (e) { "None" -> it; "In" -> it.easeIn(); "Out" -> it.easeOut(); "Both" -> it.easeBoth(); else -> failCase(e) } }
                              )
                              cycleCount = INDEFINITE
                              isAutoReverse = true
                              onContentChange += ::stop
                              playOpen()
                           } on onContentChange
                        }
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
         content = vBox(5.emScaled, CENTER) {

            lay += textArea {
               anim(5.seconds) {
                  val a = it * 2 * PI
                  val s = 100 + 100*((1-it).abs min it.abs)
                  val f = (cos(a) x sin(a)) * s
                  val t = (cos(a + PI) x sin(a + PI)) * s
                  style = "-fx-border-color: linear-gradient(from ${(width/2+f.x).toInt()}px ${height/2+f.y.toInt()}px to ${width/2+t.x.toInt()}px ${height/2+t.y.toInt()}px, transparent 25%, -fx-focus-color 50%, transparent 75%); -fx-border-width:2;"
               }.apply {
                  interpolator = LINEAR
                  cycleCount = INDEFINITE
                  onContentChange += ::stop
                  onClose += ::stop
               }.playOpen()
            }
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

      fun task(name: String, block: () -> Any?) = runSuspendingFx { delay(5000); block() }.withAppProgress(name)
      fun taskUiP(name: String, block: () -> Any?) = runSuspendingFx { AppProgress.start(name).reportFor { t -> repeat(60*5) { t.reportProgress(it/299.0); awaitPulse() }; block() } }
      fun taskIoP(name: String, block: () -> Flow<Double01>) = runSuspendingFx { AppProgress.start(name).reportFor { t -> block().conflate().collect { t.reportProgress(it); awaitPulse() } } }

      content.children setToOne fittingScrollPane {
         content = vBox(0.0, CENTER_LEFT) {
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test success") {} }
               .withText(RIGHT, CENTER_LEFT, "Run long running task that succeeds")
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test failure (exception)") { fail { "Test error message" } } }
               .withText(RIGHT, CENTER_LEFT, "Run task that fails with exception")
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test failure (custom error)") { Try.error("Test error value") } }
               .withText(RIGHT, CENTER_LEFT, "Run task that fails with custom error")
            lay += Icon(IconFA.PLAY)
               .onClickDo { task("Test failure (custom list with errors)") { runIoParallel(2, (1..100).toList()) { if (random()>0.5) it else fail { "error" } } } }
               .withText(RIGHT, CENTER_LEFT, "Run task with parallel subtasks that fail randomly with custom error")
            lay += Icon(IconFA.PLAY)
               .onClickDo { taskUiP("Test progress (on UI thread)") { } }
               .withText(RIGHT, CENTER_LEFT, "Run task with progress")
            lay += Icon(IconFA.PLAY)
               .onClickDo { taskIoP("Test progress (on BGR thread)") { (1..10000000).asFlow().map { it/10000000.0 }.flowOn(VT) } }
               .withText(RIGHT, CENTER_LEFT, "Run task with progress")
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
            lay += label("Implemented using onHoverOrInDrag():")
            lay += hBox(10.emScaled) {
               padding = Insets(0.0, 0.0, 0.0, 25.emScaled)
               lay += stackPane {
                  isPickOnBounds = false
                  val c = circle(50.emScaled, BLACK)
                  onHoverOrInDrag { c.fill = if (it) GRAY else BLACK }
                  lay += c
                  lay += circleTop()
                  lay += labelTop("within")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     onHoverOrInDrag { fill = if (it) GRAY else BLACK }
                  }
                  lay += circleTop()
                  lay += labelTop("above")
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += circle(50.emScaled, BLACK) {
                     onHoverOrInDrag { fill = if (it) GRAY else BLACK }
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
                  lay += circleTop()
                  lay += labelTop("above")
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

   fun testKeyEvents() {
      onContentChange()
      content.children setToOne KeyboardEventViewer()
      content.children[0].requestFocus()
   }

   fun testClipboard() {
      onContentChange()
      content.children setToOne fittingScrollPane {
         content = ClipboardViewer()
         content.requestFocus()
      }
   }

   fun testContainers() {
      suspend fun testControlContainer() = ContainerBi(Orientation.HORIZONTAL).apply {
         children += 1 to ContainerBi(VERTICAL).apply {
            children += 1 to emptyWidgetFactory.create()
            children += 2 to emptyWidgetFactory.create()
         }
         children += 2 to ContainerBi(VERTICAL).apply {
            children += 1 to ContainerBi(Orientation.HORIZONTAL).apply {
               children += 1 to emptyWidgetFactory.create()
               children += 2 to emptyWidgetFactory.create()
            }
            children += 2 to ContainerBi(Orientation.HORIZONTAL).apply {
               children += 1 to emptyWidgetFactory.create()
               children += 2 to emptyWidgetFactory.create()
            }
         }
      }
      onContentChange()
      content.children setToOne Icon(IconFA.PLAY).run {
         onClickDo {
            runSuspendingFx {
               APP.windowManager.showWindow(testControlContainer())
            }
         }
         withText(RIGHT, CENTER_LEFT, "Show new window with nested containers")
      }
   }

   fun testErrors() {
      onContentChange()

      content.children setToOne fittingScrollPane {
         content = vBox(0.0, CENTER_LEFT) {
            lay += Icon(IconFA.PLAY)
               .onClickDo { RuntimeException("Error generated by user from $name widget").errorNotify { AppError(it.message.orEmpty(), "Reason: ${it.stacktraceAsString}") } }
               .withText(RIGHT, CENTER_LEFT, "Generate application error event")
            lay += Icon(IconFA.PLAY)
               .onClickDo { APP.actionStream("Event generated by user from $name widget") }
               .withText(RIGHT, CENTER_LEFT, "Generate application event")
            lay += label()
            lay += Icon(IconFA.PLAY)
               .onClickDo { APP.actionsLog.showDetailForLastError() }
               .withText(RIGHT, CENTER_LEFT, "Open Action log")
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
         val consumer3 = v(0.0)
         val consumer4 = v(0.0)
      }

      c.consumer1 syncFrom (c.d flatMap {
         when (it) {
            true -> c.a
            false -> (c.b zip c.c).map { (td, sd) -> sd + td }
         }
      })

      c.consumer2 syncFrom (c.d into {
         when (it) {
            true -> c.a
            false -> c.b zip c.c map { (td, sd) -> sd + td }
         }
      })

      c.consumer3 syncFrom c.d.flatMap {
         when (it) {
            true -> c.a
            false -> c.b zip c.c map { (td, sd) -> sd + td }
         }
      }

      c.d syncWhile {
         when (it) {
            true -> c.consumer4 syncFrom c.a
            false -> c.b syncWhile { td -> c.c sync { sd -> c.consumer4.value = sd + td } }
         }
      }

      content.children setToOne fittingScrollPane {
         content = vBox(12.emScaled, CENTER_LEFT) {
            lay += label("Observable chains.")
            lay += label("The below values `if (d) a else (b + c)` should be the same.")
            lay += label()
            lay += label("`flatMap` (JavaFX) based. Tests `map()` (JavaFX), `flatMap()` (JavaFX), `zip()`.")
            lay += label { textProperty() syncFrom c.consumer1.map { "    Value: $it" } }
            lay += label()
            lay += label("`flatMap` based. Tests `map()`, `flatMap()`, `zip()`.")
            lay += label { textProperty() syncFrom c.consumer2.map { "    Value: $it" } }
            lay += label()
            lay += label("`into` based. Tests `map()`, `flatMap()`, `zip()`.")
            lay += label { textProperty() syncFrom c.consumer3.map { "    Value: $it" } }
            lay += label()
            lay += label("`Subscription` based. Tests `Subscription` nesting.")
            lay += label { textProperty() syncFrom c.consumer4.map { "    Value: $it" } }
            lay += label()
            lay += form(c).apply { editorUi.value = MINI }
         }
      }
   }

   inner class Group(glyph: GlyphIcons, val name: String, val block: () -> Unit) {
      val row = listBoxRow(glyph, name) {
         icon.onClickDo { groupSelected.value = name }
      }
      fun select(s: Boolean) {
         row.select(s)
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