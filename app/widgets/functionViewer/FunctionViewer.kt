package functionViewer

import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Cursor
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.DEVELOPMENT
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.util.access.V
import sp.it.pl.util.access.initAttach
import sp.it.pl.util.access.v
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.conf.Config
import sp.it.pl.util.functional.net
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.anchorPane
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.initClip
import sp.it.pl.util.graphics.initMouseDrag
import sp.it.pl.util.graphics.label
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.graphics.minPrefMaxHeight
import sp.it.pl.util.graphics.minPrefMaxWidth
import sp.it.pl.util.graphics.setMinPrefMaxSize
import sp.it.pl.util.graphics.vBox
import sp.it.pl.util.math.P
import sp.it.pl.util.math.StrExF
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.sync
import java.lang.Math.max
import kotlin.math.roundToInt

typealias Fun = (Double) -> Double
typealias Num = Double
typealias VNum = V<Double>

@Widget.Info(
        author = "Martin Polakovic",
        name = "Function Viewer",
        description = "Plots functions",
        version = "1.0",
        year = "2015",
        group = DEVELOPMENT
)
class FunctionViewer(widget: Widget): SimpleController(widget) {
    private val function = v(StrExF.fromString("x").orThrow).initAttach { plotAnimated(it) }
    private val functionConfigField = ConfigField.create(Config.forProperty(StrExF::class.java, "Function", function))
    private val xMin: VNum = v(-1.0).initAttach { plot() }
    private val xMax: VNum = v(1.0).initAttach { plot() }
    private val yMin: VNum = v(-1.0).initAttach { plot() }
    private val yMax: VNum = v(1.0).initAttach { plot() }
    private val plot = Plot()
    private val plotAnimation = Anim.anim(700.millis) { plot.animation.value = 1.0-it*it*it*it }
    private var updateCoord: (P) -> Unit = {}

    init {
        cursor = Cursor.CROSSHAIR
        onEventDown(MOUSE_MOVED) {
            val size = P(width, height)
            val posUi = P(it.x, it.y)
            val posRel = (posUi/size).invertY()
            val range = P(xMax.value-xMin.value, yMax.value-yMin.value)
            val pos = P(xMin.value, yMin.value)+range*posRel
            updateCoord(pos)
        }
        onEventDown(SCROLL) {
            val scale = 1.2
            val isInc = it.deltaY<0 || it.deltaX>0
            val rangeOld = P(xMax.value-xMin.value, yMax.value-yMin.value)
            val rangeNew = if (isInc) rangeOld*scale else rangeOld/scale
            val rangeDiff = rangeNew-rangeOld
            val size = P(width, height)
            val posUi = P(it.x, it.y)
            val posRelMin = (posUi/size).invertY()
            val posRelMax = P(1.0, 1.0)-posRelMin
            plot.ignorePlotting = true
            xMin.value -= rangeDiff.x*posRelMin.x
            xMax.value += rangeDiff.x*posRelMax.x
            yMin.value -= rangeDiff.y*posRelMin.y
            yMax.value += rangeDiff.y*posRelMax.y
            plot.ignorePlotting = false
            plot()

            it.consume()
        }
        initMouseDrag(
                mutableListOf<Num>(),
                {
                    it.data setTo listOf(xMin.value, xMax.value, yMin.value, yMax.value)
                },
                {
                    val size = P(width, height)
                    val diffRel = (it.diff/size) //.invertY()
                    val range = P(it.data[1]-it.data[0], it.data[3]-it.data[2])
                    val diff = diffRel*range
                    plot.ignorePlotting = true
                    xMin.value = it.data[0]-diff.x
                    xMax.value = it.data[1]-diff.x
                    yMin.value = it.data[2]+diff.y
                    yMax.value = it.data[3]+diff.y
                    plot.ignorePlotting = false
                    plot()
                }
        )

        setMinPrefMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
        layFullArea += plot
        lay(null, 10, 10, null) += label {
            isMouseTransparent = true
            updateCoord = { text = "[${it.x} ${it.y}]" }
        }
        lay(0, null, null, 0) += vBox(5) {
            isFillWidth = false

            lay += functionConfigField.toHBox()
            lay += xMin.toConfigField("xMin").toHBox()
            lay += xMax.toConfigField("xMax").toHBox()
            lay += yMin.toConfigField("yMin").toHBox()
            lay += yMax.toConfigField("yMax").toHBox()
        }
    }

    override fun refresh() = plotAnimated()

    override fun focus() = functionConfigField.focus()

    fun plot(f: Fun = function.value) = plot.plot(f)

    fun plotAnimated(f: Fun = function.value) = plotAnimation.playOpenDoClose { plot(f) }

    inner class Plot: Pane() {
        val animation = v(1.0)
        val coordRoot: AnchorPane
        val pathRoot: AnchorPane
        val displayOutside = false // true can make scaleY animations prettier
        var ignorePlotting = false

        init {
            setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
            pathRoot = anchorPane {
                minPrefMaxWidth = USE_COMPUTED_SIZE
                minPrefMaxHeight = USE_COMPUTED_SIZE
                initClip()
                animation sync { opacity = it }
            }
            coordRoot = anchorPane {
                minPrefMaxWidth = USE_COMPUTED_SIZE
                minPrefMaxHeight = USE_COMPUTED_SIZE
            }
            isMouseTransparent = true

            lay += coordRoot
            lay += pathRoot

            layoutBoundsProperty() sync { this.plot(function.value) }
        }

        fun plot(function: Fun) {
            if (ignorePlotting) return

            // draw function
            val paths = ArrayList<Path>()
            val xInc = (xMax.value-xMin.value)/max(1.0, width)
            var x = xMin.value+xInc
            var previousValue: P? = null
            var path: Path? = null
            while (x<xMax.value) {
                try {
                    val y = function(x)
                    val isOutside = !displayOutside && y !in yMin.value..yMax.value
                    val wasOutside = !displayOutside && previousValue?.net { it.y !in yMin.value..yMax.value } ?: true
                    if (path==null) {
                        path = Path().apply {
                            stroke = Color.ORANGE
                            strokeWidth = 2.0
                            fill = Color.TRANSPARENT
                            elements += MoveTo(mapX(previousValue?.x ?: x), mapY(previousValue?.y ?: y))
                        }
                        paths += path
                    } else {
                        if (!(wasOutside && isOutside))
                            path.elements += LineTo(mapX(x), mapY(y))
                    }

                    previousValue = previousValue?.apply { this.x = x; this.y = y; } ?: P(x, y)
                    if (isOutside) path = null
                } catch (e: ArithmeticException) {
                    previousValue = null
                    path = null
                }

                x += xInc
            }
            pathRoot.children setTo paths.asSequence()

            coordRoot.children.clear()

            // draw axes
            if (0.0 in xMin.value..xMax.value) {
                coordRoot.children += Line(mapX(0.0).precise, 0.0, mapX(0.0).precise, height.precise).apply {
                    stroke = Color.AQUA
                    opacity = 0.4
                    strokeWidth = 1.0
                    strokeDashArray += 2.0
                }
            }
            if (0.0 in yMin.value..yMax.value) {
                coordRoot.children += Line(0.0, mapY(0.0).precise, width.precise, mapY(0.0).precise).apply {
                    stroke = Color.AQUA
                    opacity = 0.4
                    strokeWidth = 1.0
                    strokeDashArray += 2.0
                }
            }

            @Suppress("UNUSED_VARIABLE") val unit = 10 pow ((xMax.value-xMin.value).digits())
            @Suppress("UNUSED_VARIABLE") val range = P(xMax.value-xMin.value, yMax.value-yMin.value)
        }

        private fun mapX(x: Num) = (x-xMin.value)/(xMax.value-xMin.value)*width

        private fun mapY(y: Num) = (1-(y-yMin.value)/(yMax.value-yMin.value))*height

    }

    companion object {

        fun ConfigField<*>.toHBox() = hBox(5, CENTER) {
            lay += createLabel().apply { alignment = CENTER_RIGHT; prefWidth = 100.0 }
            lay += getNode()
        }

        fun VNum.toConfigField(name: String) = ConfigField.create(Config.forProperty(Num::class.java, name, this))!!

        val Double.precise: Double get() = roundToInt().toDouble()

        fun P.invertY() = apply { y = 1-y }

        infix fun Int.pow(power: Int) = Math.pow(this.toDouble(), power.toDouble())

        fun Num.digits(): Int {
            when {
                this<0.0 -> {
                    var x = this
                    var digits = 0
                    while (x<0) {
                        x += 10
                        digits++
                    }
                    return -digits
                }
                this>0.0 -> {
                    var x = this
                    var digits = 0
                    while (x>0) {
                        x /= 10
                        digits++
                    }
                    return digits
                }
                else -> return 0
            }

        }
    }
}