package functionViewer

import java.math.BigDecimal
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
import sp.it.pl.main.toUi
import sp.it.pl.ui.itemnode.ConfigEditor
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.between
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

private typealias Fun = (Double) -> Double
private typealias Num = Double
private typealias NumRange = ClosedFloatingPointRange<Double>
private typealias BigNum = BigDecimal
private typealias BigNumRange = ClosedRange<BigDecimal>

class FunctionViewer(widget: Widget): SimpleController(widget) {
   private val function by cv(StrExF("x")) attach { plotAnimated(it) }
   private var functionPlotted = function.value as Fun
   private val functionEditor = ConfigEditor.create(Config.forProperty<StrExF>("Function", function))
   private val xMin by cv(BigDecimal("-1.0")).between(BIG_NUM_MIN, BIG_NUM_MAX) attach { plot() }
   private val xMax by cv(BigDecimal("+1.0")).between(BIG_NUM_MIN, BIG_NUM_MAX) attach { plot() }
   private val yMin by cv(BigDecimal("-1.0")).between(BIG_NUM_MIN, BIG_NUM_MAX) attach { plot() }
   private val yMax by cv(BigDecimal("+1.0")).between(BIG_NUM_MIN, BIG_NUM_MAX) attach { plot() }
   private val plot = Plot()
   private val plotAnimation = anim(700.millis) { plot.animation.value = 1.0 - it*it*it*it }
   private var coordTxtUpdate: (ClosedRange<BigDecimal>) -> Unit = {}
   private var coordVisUpdate: (Boolean) -> Unit = {}

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.cursor = CROSSHAIR
      root.onEventDown(MOUSE_MOVED) {
         val size = root.size
         val posUi = P(it.x, it.y)
         val posRel = (posUi/size).invertY()
         val range = (xMax.value - xMin.value)..(yMax.value - yMin.value)
         val pos = (xMin.value..yMin.value) + range*posRel
         coordTxtUpdate(pos)
      }
      root.onEventDown(SCROLL) {
         val scale = 1.2.toBigDecimal()
         val isInc = it.deltaY<0 || it.deltaX>0
         val rangeOld = (xMax.value - xMin.value)..(yMax.value - yMin.value)
         val rangeNew = if (isInc) rangeOld*scale else rangeOld/scale
         val rangeDiff = rangeNew - rangeOld
         val size = root.size
         val posUi = P(it.x, it.y)
         val posRelMin = (posUi/size).invertY()
         val posRelMax = P(1.0, 1.0) - posRelMin
         plot.ignorePlotting = true
         xMin.value = (xMin.value - rangeDiff.start*posRelMin.x).clipToNum()
         xMax.value = (xMax.value + rangeDiff.start*posRelMax.x).clipToNum()
         yMin.value = (yMin.value - rangeDiff.endInclusive*posRelMin.y).clipToNum()
         yMax.value = (yMax.value + rangeDiff.endInclusive*posRelMax.y).clipToNum()
         plot.ignorePlotting = false
         plot()

         it.consume()
      }
      root.initMouseDrag(
         mutableListOf<Num>(),
         {
            it.data setTo listOf(xMin.valueNum, xMax.valueNum, yMin.valueNum, yMax.valueNum)
         },
         {
            val size = root.size
            val diffRel = (it.diff/size)
            val range = P(it.data[1] - it.data[0], it.data[3] - it.data[2])
            val diff = diffRel*range
            plot.ignorePlotting = true
            xMin.value = (it.data[0] - diff.x).clipToNum().toBigDecimal()
            xMax.value = (it.data[1] - diff.x).clipToNum().toBigDecimal()
            yMin.value = (it.data[2] + diff.y).clipToNum().toBigDecimal()
            yMax.value = (it.data[3] + diff.y).clipToNum().toBigDecimal()
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
               coordTxtUpdate = { text = "[${it.start.toUi()} ${it.endInclusive.toUi()}]" }
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

   inner class Plot: AnchorPane() {
      val animation = v(1.0)
      val coordRoot: Canvas
      val pathRoot: Canvas
      val pathGc: GraphicsContext
      val coordGc: GraphicsContext
      val displayOutside = false // true can make scaleY animations prettier
      var ignorePlotting = false
      val isInvalidRange get() = xMin.valueNum>=xMax.valueNum || yMin.valueNum>=yMax.valueNum

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

         val xMinNum = xMin.valueNum
         val xMaxNum = xMax.valueNum
         val yMinNum = yMin.valueNum
         val yMaxNum = yMax.valueNum

         pathGc.clearRect(0.0, 0.0, pathRoot.width, pathRoot.height)
         coordGc.clearRect(0.0, 0.0, coordRoot.width, coordRoot.height)

         // draw function
         fun mapX(x: Num) = (x - xMinNum)/(xMaxNum - xMinNum)*width
         fun mapY(y: Num) = (1 - (y - yMinNum)/(yMaxNum - yMinNum))*height
         val xIncMax = (xMaxNum - xMinNum)/(1.0 max width)
         val xIncDistMax = 4*sqrt(2*xIncMax.pow(2))
         val xIncDistMin = xIncDistMax/4.0
         var xInc = xIncMax
         var x = xMinNum + xInc
         var previousValue: P? = null
         var path: Any? = null
         lateinit var moveTo: P
         while (x<xMaxNum) {
            try {
               val y = function(x)
//               println("$previousValue $x $y")
               val isOutside = !displayOutside && y !in yMinNum..yMaxNum
               val wasOutside = !displayOutside && previousValue?.net { it.y !in yMinNum..yMaxNum } ?: true

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
                     pathGc.strokeLine(moveTo.x, moveTo.y, mapX(x), mapY(y.clip(yMinNum, yMaxNum)))
                  }
               } else {
                  if (!wasOutside || !isOutside) {
                     pathGc.globalAlpha = 1.0
                     pathGc.stroke = Color.ORANGE
                     pathGc.lineWidth = 2.0
                     pathGc.fill = Color.TRANSPARENT
                     pathGc.setLineDashes()
                     pathGc.strokeLine(moveTo.x, moveTo.y, mapX(x), mapY(y.clip(yMinNum, yMaxNum)))
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
         if (0.0 in xMinNum..xMaxNum) {
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 2.0
            coordGc.setLineDashes(2.0)
            coordGc.strokeLine(mapX(0.0).precise, 0.0, mapX(0.0).precise, height.precise)
         }
         if (0.0 in yMinNum..yMaxNum) {
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 2.0
            coordGc.setLineDashes(2.0)
            coordGc.strokeLine(0.0, mapY(0.0).precise, width.precise, mapY(0.0).precise)
         }

         (xMinNum..xMaxNum).axes().forEach { axe ->
            coordGc.stroke = Color.AQUA
            coordGc.globalAlpha = 0.4
            coordGc.lineWidth = 1.0
            coordGc.setLineDashes(4.0)
            coordGc.strokeLine(mapX(axe).precise, 0.0, mapX(axe).precise, height.precise)
         }
         (yMinNum..yMaxNum).axes().forEach { axe ->
            coordGc.stroke = Color.AQUA
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

      val V<BigNum>.valueNum: Double get() = value.clipToNum().toDouble().clipToNum()

      val Double.precise: Double get() = roundToInt().toDouble()

      const val NUM_MIN = -Double.MAX_VALUE
      const val NUM_MAX = +Double.MAX_VALUE
      val BIG_NUM_MIN = NUM_MIN.toBigDecimal()
      val BIG_NUM_MAX = NUM_MAX.toBigDecimal()

      fun Num.clipToNum() = clip(NUM_MIN, NUM_MAX)
      
      fun BigNum.clipToNum() = clip(BIG_NUM_MIN, BIG_NUM_MAX)
      
      operator fun BigNum.times(v: Double) = this*v.toBigDecimal()
      
      operator fun BigNumRange.times(v: P) = start*v.x.toBigDecimal()..endInclusive*v.y.toBigDecimal()
      operator fun BigNumRange.times(v: BigNum) = start*v..endInclusive*v
      operator fun BigNumRange.div(v: BigNum) = start/v..endInclusive/v
      operator fun BigNumRange.minus(v: BigNumRange) = start-v.start..endInclusive-v.endInclusive
      operator fun BigNumRange.plus(v: BigNumRange) = start+v.start..endInclusive+v.endInclusive
      
      fun NumRange.axes(): Sequence<Double> {
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