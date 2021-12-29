package functionViewer

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
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.ConfigEditor
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.cv
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
import sp.it.util.ui.anchorPane
import sp.it.util.ui.hBox
import sp.it.util.ui.initClip
import sp.it.util.ui.initMouseDrag
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.prefSize
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.version
import sp.it.util.units.year

private typealias Fun = (Double) -> Double
private typealias Num = Double

class FunctionViewer(widget: Widget): SimpleController(widget) {
   private val function by cv(StrExF("x")) attach { plotAnimated(it) }
   private var functionPlotted = function.value as Fun
   private val functionEditor = ConfigEditor.create(Config.forProperty<StrExF>("Function", function))
   private val xMin by cv(-1.0) attach { plot() }
   private val xMax by cv(+1.0) attach { plot() }
   private val yMin by cv(-1.0) attach { plot() }
   private val yMax by cv(+1.0) attach { plot() }
   private val plot = Plot()
   private val plotAnimation = anim(700.millis) { plot.animation.value = 1.0 - it*it*it*it }
   private var updateCoord: (P) -> Unit = {}

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.cursor = CROSSHAIR
      root.onEventDown(MOUSE_MOVED) {
         val size = root.size
         val posUi = P(it.x, it.y)
         val posRel = (posUi/size).invertY()
         val range = P(xMax.value - xMin.value, yMax.value - yMin.value)
         val pos = P(xMin.value, yMin.value) + range*posRel
         updateCoord(pos)
      }
      root.onEventDown(SCROLL) {
         val scale = 1.2
         val isInc = it.deltaY<0 || it.deltaX>0
         val rangeOld = P(xMax.value - xMin.value, yMax.value - yMin.value)
         val rangeNew = if (isInc) rangeOld*scale else rangeOld/scale
         val rangeDiff = rangeNew - rangeOld
         val size = root.size
         val posUi = P(it.x, it.y)
         val posRelMin = (posUi/size).invertY()
         val posRelMax = P(1.0, 1.0) - posRelMin
         plot.ignorePlotting = true
         xMin.value -= rangeDiff.x*posRelMin.x
         xMax.value += rangeDiff.x*posRelMax.x
         yMin.value -= rangeDiff.y*posRelMin.y
         yMax.value += rangeDiff.y*posRelMax.y
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
            val diffRel = (it.diff/size) //.invertY()
            val range = P(it.data[1] - it.data[0], it.data[3] - it.data[2])
            val diff = diffRel*range
            plot.ignorePlotting = true
            xMin.value = it.data[0] - diff.x
            xMax.value = it.data[1] - diff.x
            yMin.value = it.data[2] + diff.y
            yMax.value = it.data[3] + diff.y
            plot.ignorePlotting = false
            plot()
         }
      )

      root.setMinPrefMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
      root.lay += plot
      root.lay += stackPane {
         isManaged = false
         root.widthProperty() attach { resizeRelocate(0.0, 0.0, root.width, root.height) }
         root.heightProperty() attach { resizeRelocate(0.0, 0.0, root.width, root.height) }

         lay += stackPane {

            lay(BOTTOM_RIGHT) += label {
               isMouseTransparent = true
               updateCoord = { text = "[${it.x} ${it.y}]" }
            }
            lay(TOP_LEFT) += vBox(5) {
               isFillWidth = false

               lay += functionEditor.toHBox()
               lay += xMin.createEditor("xMin").toHBox()
               lay += xMax.createEditor("xMax").toHBox()
               lay += yMin.createEditor("yMin").toHBox()
               lay += yMax.createEditor("yMax").toHBox()
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

   inner class Plot: AnchorPane() {
      val animation = v(1.0)
      val coordRoot: Canvas
      val pathRoot: Canvas
      val pathGc: GraphicsContext
      val coordGc: GraphicsContext
      val displayOutside = false // true can make scaleY animations prettier
      var ignorePlotting = false

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

         pathGc.clearRect(0.0, 0.0, pathRoot.width, pathRoot.height)
         coordGc.clearRect(0.0, 0.0, coordRoot.width, coordRoot.height)

         // draw function
         val xIncMax = (xMax.value - xMin.value)/(1.0 max width)
         val xIncDistMax = 4*sqrt(2*xIncMax.pow(2))
         val xIncDistMin = xIncDistMax/4.0
         var xInc = xIncMax
         var x = xMin.value + xInc
         var previousValue: P? = null
         var path: Any? = null
         lateinit var moveTo: P
         while (x<xMax.value) {
            try {
               val y = function(x)
//               println("$previousValue $x $y")
               val isOutside = !displayOutside && y !in yMin.value..yMax.value
               val wasOutside = !displayOutside && previousValue?.net { it.y !in yMin.value..yMax.value } ?: true

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
                     pathGc.strokeLine(moveTo.x, moveTo.y, mapX(x), mapY(y.clip(yMin.value, yMax.value)))
                  }
               } else {
                  if (!wasOutside || !isOutside) {
                     pathGc.globalAlpha = 1.0
                     pathGc.stroke = Color.ORANGE
                     pathGc.lineWidth = 2.0
                     pathGc.fill = Color.TRANSPARENT
                     pathGc.setLineDashes()
                     pathGc.strokeLine(moveTo.x, moveTo.y, mapX(x), mapY(y.clip(yMin.value, yMax.value)))
                  }
               }

               if (isOutside)
                  path = null

               previousValue = P(x, y)
               moveTo = P(mapX(previousValue.x), mapY(previousValue.y))

            } catch (e: ArithmeticException) {
               previousValue = null
               path = null
            }

            x += xInc
         }

         // draw axes
         if (0.0 in xMin.value..xMax.value) {
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 2.0
            coordGc.setLineDashes(2.0)
            coordGc.strokeLine(mapX(0.0).precise, 0.0, mapX(0.0).precise, height.precise)
         }
         if (0.0 in yMin.value..yMax.value) {
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 2.0
            coordGc.setLineDashes(2.0)
            coordGc.strokeLine(0.0, mapY(0.0).precise, width.precise, mapY(0.0).precise)
         }

         (xMin.value..xMax.value).axes().forEach { axe ->
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 1.0
            coordGc.setLineDashes(4.0)
            coordGc.strokeLine(mapX(axe).precise, 0.0, mapX(axe).precise, height.precise)
         }
         (yMin.value..yMax.value).axes().forEach { axe ->
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 1.0
            coordGc.setLineDashes(4.0)
            coordGc.strokeLine(0.0, mapY(axe).precise, width.precise, mapY(axe).precise)
         }
      }

      private fun mapX(x: Num) = (x - xMin.value)/(xMax.value - xMin.value)*width

      private fun mapY(y: Num) = (1 - (y - yMin.value)/(yMax.value - yMin.value))*height

   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Function Viewer"
      override val description = "Plots mathematical functions"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(2, 0, 0)
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

      fun V<Double>.createEditor(name: String) = ConfigEditor.create(Config.forProperty<Num>(name, this))

      val Double.precise: Double get() = roundToInt().toDouble()

      fun ClosedFloatingPointRange<Double>.axes(): Sequence<Double> {
         val axeRange = endInclusive-start
         return when {
            axeRange==0.0 -> sequenceOf()
            else -> {
               val precisionDigits = when {
                  axeRange>0.0 -> axeRange.traverse { it/10.0 }.takeWhile { it>1.0 }.count().toDouble() - 1.0
                  else -> -axeRange.traverse { it*10.0 }.takeWhile { it<1.0 }.count().toDouble() - 1.0
               }
               val axeGap = 10.0.pow(precisionDigits)
               val axeFirst = floor(start / axeGap).toInt()
               generateSequence(axeFirst) { it+1 }.map { it*axeGap }.filter { it>=start }.takeWhile { it<=endInclusive }
            }
         }

      }

      fun P.invertY() = apply { y = 1 - y }
   }
}