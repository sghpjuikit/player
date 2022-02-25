package functionViewer

import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.math.RoundingMode.UP
import java.text.NumberFormat
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_LEFT
import javafx.scene.Cursor.CROSSHAIR
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import javafx.scene.paint.Color
import javafx.scene.paint.Color.AQUA
import javafx.scene.paint.CycleMethod.NO_CYCLE
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import kotlin.math.pow
import kotlin.math.roundToInt
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.ConfigEditor
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.cv
import sp.it.util.conf.getDelegateConfig
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.math.P
import sp.it.util.math.StrExF
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachFrom
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.text.keys
import sp.it.util.ui.alpha
import sp.it.util.ui.anchorPane
import sp.it.util.ui.hBox
import sp.it.util.ui.initClip
import sp.it.util.ui.initMouseDrag
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.ui.prefSize
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.version
import sp.it.util.units.year

private typealias Fun = (BigDecimal) -> BigDecimal
private typealias Num = BigDecimal
private typealias NumRange = ClosedRange<BigDecimal>

class FunctionViewer(widget: Widget): SimpleController(widget) {
   private val function by cv(StrExF("x")) attach { plotAnimated(it) }
   private var functionPlotted = function.value as Fun
   private val functionEditor = ConfigEditor.create(Config.forProperty<StrExF>("Function", function))
   private val xMin by cv(Num("-1.0")) attach { plot() }
   private val xMax by cv(Num("+1.0")) attach { plot() }
   private val yMin by cv(Num("-1.0")) attach { plot() }
   private val yMax by cv(Num("+1.0")) attach { plot() }
   private val plot = Plot()
   private val plotAnimation = anim(400.millis) { plot.animation.value = 1.0 - it*it*it*it }
   private var coordTxtUpdate: (ClosedRange<Num>) -> Unit = {}
   private var coordVisUpdate: (Boolean) -> Unit = {}

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.cursor = CROSSHAIR
      root.onEventDown(MOUSE_MOVED) {
         val size = root.size
         val posUi = P(it.x, it.y)
         val posRel = (posUi/size).invertY()
         val range = (xMax.value - xMin.value)..(yMax.value - yMin.value)
         val pos = (xMin.value..yMin.value) + range * posRel
         coordTxtUpdate(pos)
      }
      root.onEventDown(SCROLL) {
         val scale = 1.2.big
         val isInc = it.deltaY<0 || it.deltaX>0
         val rangeOld = (xMax.value - xMin.value)..(yMax.value - yMin.value)
         val rangeNew = if (isInc) rangeOld*scale else rangeOld/scale
         val rangeDiff = rangeNew - rangeOld
         val size = root.size
         val posUi = P(it.x, it.y)
         val posRelMin = (posUi/size).invertY()
         val posRelMax = P(1.0, 1.0) - posRelMin
         plot.ignorePlotting = true
         xMin.value = (xMin.value - rangeDiff.start*posRelMin.x)
         xMax.value = (xMax.value + rangeDiff.start*posRelMax.x)
         yMin.value = (yMin.value - rangeDiff.endInclusive*posRelMin.y)
         yMax.value = (yMax.value + rangeDiff.endInclusive*posRelMax.y)
         updatePrecision()
         plot.ignorePlotting = false
         plot()

         it.consume()
      }
      root.initMouseDrag(
         mutableListOf<Num>(),
         {
            it.data setTo listOf(xMin.value, xMax.value, yMin.value, yMax.value)
         },
         {
            val size = root.size
            val diffRel = it.diff/size
            val range = BigP(it.data[1] - it.data[0], it.data[3] - it.data[2])
            val diff = BigP(range.x*diffRel.x.big, range.y*diffRel.y.big)
            plot.ignorePlotting = true
            xMin.value = (it.data[0] - diff.x)
            xMax.value = (it.data[1] - diff.x)
            yMin.value = (it.data[2] + diff.y)
            yMax.value = (it.data[3] + diff.y)
            updatePrecision()
            plot.ignorePlotting = false
            plot()
         }
      )
      root.onHoverOrDrag { coordVisUpdate(it) } on onClose

      root.setMinPrefMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
      root.lay += plot
      root.lay += stackPane {
         isManaged = false
         root.widthProperty() attach { resizeRelocate(0.0, 0.0, root.width, root.height) }
         root.heightProperty() attach { resizeRelocate(0.0, 0.0, root.width, root.height) }

         lay += stackPane {

            lay(BOTTOM_RIGHT) += label {
               isMouseTransparent = true
               coordTxtUpdate = {
                  val precision = (xMin.value..xMax.value).precisionDigits()
                  val numberFormat = NumberFormat.getNumberInstance(APP.locale.value).apply {
                     minimumFractionDigits = precision
                     maximumFractionDigits = precision
                  }
                  text = "[${numberFormat.format(it.start.setScale(precision, UP))} ${numberFormat.format(it.endInclusive.setScale(precision, UP))}]"
               }
               coordVisUpdate = { isVisible = it }
            }
            lay(TOP_LEFT) += vBox(5) {
               isFillWidth = false

               lay += functionEditor.toHBox()
               lay += ConfigEditor.create(::xMin.getDelegateConfig()).toHBox()
               lay += ConfigEditor.create(::xMax.getDelegateConfig()).toHBox()
               lay += ConfigEditor.create(::yMin.getDelegateConfig()).toHBox()
               lay += ConfigEditor.create(::yMax.getDelegateConfig()).toHBox()
            }
         }
      }

      root.sync1IfInScene { plot() } on onClose
   }

   override fun focus() = functionEditor.focusEditor()

   fun plot(f: Fun = functionPlotted) {
      functionPlotted = f
      plot.plot(f)
   }

   fun plotAnimated(f: Fun = functionPlotted) = plotAnimation.playOpenDoClose { plot(f) }

   private fun updatePrecision() {
      val precision = (xMin.value..xMax.value).precisionDigits() + 2
      xMin.setValueOf { xMin.value.setScale(precision, UP) }
      xMax.setValueOf { xMax.value.setScale(precision, UP) }
      yMin.setValueOf { yMin.value.setScale(precision, UP) }
      yMax.setValueOf { yMax.value.setScale(precision, UP) }
   }

   inner class Plot: AnchorPane() {
      val animation = v(1.0)
      val coordRoot: Canvas
      val pathRoot: Canvas
      val pathGc: GraphicsContext
      val coordGc: GraphicsContext
      var ignorePlotting = false
      val isInvalidRange get() = xMin.value>=xMax.value || yMin.value>=yMax.value
      val gradient = LinearGradient(0.0, 1.0, 0.0, 0.0, true, NO_CYCLE, Stop(1.0, AQUA.alpha(0.6)), Stop(0.0, AQUA.alpha(0.2)))

      init {
         setMinSize(0.0, 0.0)
         setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
         setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
         pathRoot = Canvas()
         pathGc = pathRoot.graphicsContext2D
         coordRoot = Canvas()
         coordGc = coordRoot.graphicsContext2D
         isMouseTransparent = true

         layFullArea += anchorPane {
            setMinSize(0.0, 0.0)
            setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            layFullArea += coordRoot.apply {
               this.widthProperty() attachFrom this@anchorPane.widthProperty()
               this.heightProperty() attachFrom this@anchorPane.heightProperty()
            }
         }
         layFullArea += anchorPane {
            setMinSize(0.0, 0.0)
            setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            initClip()
            animation sync { opacity = it }

            layFullArea += pathRoot.apply {
               this.widthProperty() attachFrom this@anchorPane.widthProperty()
               this.heightProperty() attachFrom this@anchorPane.heightProperty()
            }
         }

         layoutBoundsProperty() sync { plot(functionPlotted) }
      }

      fun plot(function: Fun) {
         if (ignorePlotting) return
         if (isInvalidRange) return

         pathGc.clearRect(0.0, 0.0, pathRoot.width, pathRoot.height)
         coordGc.clearRect(0.0, 0.0, coordRoot.width, coordRoot.height)

         // draw function
         val precision = ((xMin.value..xMax.value).precisionDigits()+2) max 10
         val xMin = xMin.value.setScale(precision, UP)
         val xMax = xMax.value.setScale(precision, UP)
         val yMin = yMin.value.setScale(precision, UP)
         val yMax = yMax.value.setScale(precision, UP)
         fun mapX(x: Num): Double = ((x - xMin)/(xMax - xMin)).toDouble()*width
         fun mapY(y: Num): Double = (ONE - (y - yMin)/(yMax - yMin)).toDouble()*height
         val xIncMax = (xMax - xMin)/(1.0 max width).big.setScale(precision, UP)
//         val xIncDistMax = 4.big*sqrt(2.big*xIncMax.pow(2))
//         val xIncDistMin = xIncDistMax/4.0
         val xInc = xIncMax
         var x = xMin + xInc
         var previousValue: BigP? = null
         var path: Any? = null
         val pathXs = ArrayList<Double>(500)
         val pathYs = ArrayList<Double>(500)
         lateinit var moveTo: P

         pathXs += 0.0
         pathYs += pathRoot.height

         while (x<xMax) {
            try {
               val y = function(x)
//               println("$previousValue $x $y")
               val isAbove = y > yMax
               val isBelow = y < yMin
               val isOutside = isAbove || isBelow
               val wasOutside = previousValue?.net { it.y !in yMin..yMax } ?: true

               if (path==null) {
                  if (!isOutside) {
//                     TODO: implement look-around to fix clipping for steep functions like 1/x
//                     if (previousValue!=null) {
//                        val xIncDist = previousValue.distance(x, y)
//                        if (xIncDist>xIncDistMax) {
//                           x -= xInc
//                           xInc /= 2.0
//                           x += xInc
//                           continue
//                        }
//                        if (xIncDist<xIncDistMin) {
//                           x -= xInc
//                           xInc *= 2.0
//                           x += xInc
//                           continue
//                        }
//                     }

                     path = Any()
                     moveTo = P(mapX(previousValue?.x ?: x), mapY(previousValue?.y ?: y))
                  }
                  if (!wasOutside || !isOutside) {
                     pathGc.globalAlpha = 1.0
                     pathGc.stroke = Color.ORANGE
                     pathGc.lineWidth = 2.0
                     pathGc.fill = Color.TRANSPARENT
                     pathGc.setLineDashes()
                     pathGc.strokeLine(moveTo.x, moveTo.y, mapX(x), mapY(y.clip(yMin, yMax)))
                  }
               } else {
                  if (!wasOutside || !isOutside) {
                     pathGc.globalAlpha = 1.0
                     pathGc.stroke = Color.ORANGE
                     pathGc.lineWidth = 2.0
                     pathGc.fill = Color.TRANSPARENT
                     pathGc.setLineDashes()
                     pathGc.strokeLine(moveTo.x, moveTo.y, mapX(x), mapY(y.clip(yMin, yMax)))
                  }
               }

               if (isOutside)
                  path = null

               previousValue = BigP(x, y)
               moveTo = P(mapX(previousValue.x), mapY(previousValue.y))

               pathXs += moveTo.x
               pathYs += when { isAbove -> 0.0; isBelow -> pathRoot.height; else -> moveTo.y }
            } catch (e: Throwable) {
               if (previousValue!=null) {
                  moveTo = P(mapX(previousValue.x), mapY(previousValue.y))
                  pathXs += moveTo.x
                  pathYs += pathRoot.height
               }

               previousValue = null
               path = null
            }

            x += xInc
         }

         if (pathXs.size>3) {
            // add last point
            pathXs += moveTo.x; pathYs += pathRoot.height

            pathGc.globalAlpha = 0.1
            pathGc.fill = gradient
            pathGc.fillPolygon(pathXs.toDoubleArray(), pathYs.toDoubleArray(), pathXs.size)
         }

         // draw axes
         if (0.0.big in xMin..xMax) {
            coordGc.stroke = AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 2.0
            coordGc.setLineDashes(2.0)
            coordGc.strokeLine(mapX(0.0.big).precise, 0.0, mapX(0.0.big).precise, height.precise)
         }
         if (0.0.big in yMin..yMax) {
            coordGc.stroke = AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 2.0
            coordGc.setLineDashes(2.0)
            coordGc.strokeLine(0.0, mapY(0.0.big).precise, width.precise, mapY(0.0.big).precise)
         }

         (xMin..xMax).axes().forEach { axe ->
            coordGc.stroke = AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 1.0
            coordGc.setLineDashes(4.0)
            coordGc.strokeLine(mapX(axe).precise, 0.0, mapX(axe).precise, height.precise)
         }
         (yMin..yMax).axes().forEach { axe ->
            coordGc.stroke = AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 1.0
            coordGc.setLineDashes(4.0)
            coordGc.strokeLine(0.0, mapY(axe).precise, width.precise, mapY(axe).precise)
         }
      }

   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Function Viewer"
      override val description = "Plots mathematical functions"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(3, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY)
      override val summaryActions = listOf(
         ShortcutPane.Entry("Graph", "Zoom in/out", "Scroll"),
         ShortcutPane.Entry("Graph", "Shift axes", keys("LMB drag")),
      )

      fun ConfigEditor<*>.toHBox() = hBox(5, CENTER) {
         val n = buildNode()
         val l = buildLabel().apply {
            alignment = CENTER_RIGHT
            prefWidth = 100.0
            isPickOnBounds = false
            labelForWithClick setTo n
         }

         lay += l
         lay += n
      }

      data class BigP(val x: Num, val y: Num)

      val Int.big: Num get() = Num.valueOf(toLong())
      val Double.big: Num get() = Num.valueOf(this)
      val Double.precise: Double get() = roundToInt().toDouble()

      operator fun Num.times(v: Double) = this*v.big
      operator fun NumRange.times(v: P) = (start*v.x.big)..(endInclusive*v.y.big)
      operator fun NumRange.times(v: Num) = (start*v)..(endInclusive*v)
      operator fun NumRange.div(v: Num) = (start/v)..(endInclusive/v)
      operator fun NumRange.minus(v: NumRange) = (start-v.start)..(endInclusive-v.endInclusive)
      operator fun NumRange.plus(v: NumRange) = (start+v.start)..(endInclusive+v.endInclusive)

      fun NumRange.precisionDigits(): Int {
         val range = endInclusive-start
         return when {
            range<=ZERO -> 2
            range>ONE -> 0
            else -> range.traverse { it*TEN }.takeWhile { it<ONE }.count() - 1
         }.max(0) + 2
      }

      fun NumRange.axes(): Sequence<Num> {
         val range = (endInclusive-start).abs()
         return if (range==ZERO) {
            sequenceOf()
         } else {
            val digits = when {
               range>ONE -> range.traverse { it/TEN }.takeWhile { it>ONE }.count() - 1
               else -> -range.traverse { it*TEN }.takeWhile { it<ONE }.count()
            }
            val gap = 10.0.pow(digits).big
            val first = (start / gap).toInt()
            generateSequence(first) { it+1 }.map { gap*it.big }.filter { it>=start }.takeWhile { it<=endInclusive }
         }

      }

      fun P.invertY() = apply { y = 1 - y }
   }
}