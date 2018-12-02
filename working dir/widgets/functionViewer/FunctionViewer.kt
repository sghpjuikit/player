package functionViewer

import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.DEVELOPMENT
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.Config
import sp.it.pl.util.functional.net
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.graphics.setMinPrefMaxSize
import sp.it.pl.util.graphics.vBox
import sp.it.pl.util.math.P
import sp.it.pl.util.math.StrExF
import java.lang.Math.max

@Widget.Info(
        author = "Martin Polakovic",
        name = "Function Viewer",
        description = "Plots functions",
        version = "0.9",
        year = "2015",
        group = DEVELOPMENT
)
class FunctionViewer(widget: Widget<*>): SimpleController(widget) {
    private val axes = Axes(400.0, 300.0, -1.0, 1.0, 0.2, -1.0, 1.0, 0.2)
    private val plot = Plot(-1.0, 1.0, axes)
    private val function = v(StrExF.fromString("x").orThrow, this::plot)
    private var focusInput = {}

    init {
        this.setMinPrefMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
        this layFullArea vBox {
            spacing = 5.0
            padding = Insets(20.0)

            val c = ConfigField.create(Config.forProperty(StrExF::class.java, "Function", function))
            focusInput = c::focus
            lay() child StackPane(HBox(5.0, c.createLabel(), c.getNode()))
            lay(ALWAYS) child StackPane(plot)
        }
    }

    override fun requestFocus() {
        super.requestFocus()
        focusInput()
    }

    override fun refresh() = plot(function.value)

    fun plot(function: (Double) -> Double) = plot.plot(function)

    class Axes(width: Double, height: Double, xMin: Double, xMax: Double, xBy: Double, yMin: Double, yMax: Double, yBy: Double): Pane() {
        val xAxis: NumberAxis
        val yAxis: NumberAxis

        init {
            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)
            setPrefSize(width, height)
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)

            xAxis = NumberAxis(xMin, xMax, xBy).apply {
                side = Side.BOTTOM
                isMinorTickVisible = false
                prefWidth = width
                layoutY = (height/2)
            }

            yAxis = NumberAxis(yMin, yMax, yBy).apply {
                side = Side.LEFT
                isMinorTickVisible = false
                prefHeight = height
                layoutXProperty().bind(Bindings.subtract(width/2+1, widthProperty()))
            }

            children.setAll(xAxis, yAxis)
        }
    }

    class Plot(private val xMin: Double, private val xMax: Double, private val axes: Axes): Pane() {

        init {
            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)
            setPrefSize(axes.prefWidth, axes.prefHeight)
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)
        }

        fun plot(function: (Double) -> Double) {
            val paths = ArrayList<Path>()
            val inc = (xMax-xMin)/max(1.0, width) // inc by 1 px in 2D

            var x = xMin+inc
            var previousValue: P? = null
            var path: Path? = null
            while (x<xMax) {
                try {
                    val y = function(x)
                    val isOutside = y !in -1.0..1.0
                    val wasOutside = previousValue?.net { it.y !in -1.0..1.0 } ?: true
                    if (path==null) {
                        path = Path().apply {
                            stroke = Color.ORANGE
                            strokeWidth = 2.0
                            fill = Color.TRANSPARENT
                            clip = Rectangle(0.0, 0.0, axes.prefWidth, axes.prefHeight)
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

                x += inc
            }
            children.setAll(axes, *paths.toTypedArray())
        }

        private fun mapX(x: Double): Double {
            val tx = axes.prefWidth/2
            val sx = axes.prefWidth/(axes.xAxis.upperBound-axes.xAxis.lowerBound)
            return x*sx+tx
        }

        private fun mapY(y: Double): Double {
            val ty = axes.prefHeight/2
            val sy = axes.prefHeight/(axes.yAxis.upperBound-axes.yAxis.lowerBound)
            return -y*sy+ty
        }

    }
}