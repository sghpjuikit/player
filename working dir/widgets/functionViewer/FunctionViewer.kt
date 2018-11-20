package functionViewer

import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.FillRule
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.PathElement
import javafx.scene.shape.Rectangle
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.DEVELOPMENT
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.Config
import sp.it.pl.util.graphics.setAnchors
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
    private val axes = Axes(400, 300, -1.0, 1.0, 0.2, -1.0, 1.0, 0.2)
    private val plot = Plot(-1.0, 1.0, axes)
    private val function = v(StrExF.fromString("x").orThrow, this::plot)

    init {
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE)

        val c = ConfigField.create(Config.forProperty(StrExF::class.java, "Function", function))
        val la = StackPane(HBox(5.0, c.createLabel(), c.getNode()))
        val lb = StackPane(plot)
        this.children += VBox(5.0, la, lb).apply {
            setAnchors(0.0)
            padding = Insets(20.0)
            VBox.setVgrow(lb, ALWAYS)
        }

        refresh()
    }

    fun plot(function: (Double) -> Double) {
        plot.plot(function)
    }

    override fun refresh() {
        plot.plot(function.value)
    }

    class Axes(width: Int, height: Int, xMin: Double, xMax: Double, xBy: Double, yMin: Double, yMax: Double, yBy: Double): Pane() {
        val xAxis: NumberAxis
        val yAxis: NumberAxis

        init {
            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)
            setPrefSize(width.toDouble(), height.toDouble())
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)

            xAxis = NumberAxis(xMin, xMax, xBy).apply {
                side = Side.BOTTOM
                isMinorTickVisible = false
                prefWidth = width.toDouble()
                layoutY = (height/2).toDouble()
            }

            yAxis = NumberAxis(yMin, yMax, yBy).apply {
                side = Side.LEFT
                isMinorTickVisible = false
                prefHeight = height.toDouble()
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
            val path = Path().apply {
                stroke = Color.ORANGE
                strokeWidth = 2.0
                fill = Color.TRANSPARENT
                clip = Rectangle(0.0, 0.0, axes.prefWidth, axes.prefHeight)
            }
            val inc = (xMax-xMin)/max(1.0, width) // inc by 1 px in 2D

            val circles = ArrayList<Circle>()
            var dir: Xxx? = null
            var pe: PathElement? = null
            var x = xMin+inc
            while (x<xMax)
                try {
                    val y = function(x)

                    val direction = when {
                        dir==null -> Xxx(null, null, y)
                        else -> {
                            val diff = dir.y-y
                            when {
                                diff>0 -> Xxx(dir.currentDirection, Direction.DOWN, y)
                                diff<0 -> Xxx(dir.currentDirection, Direction.UP, y)
                                else -> Xxx(dir.currentDirection, Direction.SAME, y)
                            }
                        }
                    }
                    dir = direction
                    if (direction.previousDirection isInverse direction.currentDirection) {
                        val localExtremeX = x
                        val localExtremeY = function.estimateLocalExtreme(localExtremeX) ?: y
                        println("y is $localExtremeY")
                        val isOutside = localExtremeY !in -1.0..1.0
                        println("isOutside = ${isOutside}")
                        if (!isOutside) {
                            circles.add(
                                    Circle(mapX(localExtremeX)-1, mapY(localExtremeY), 5.0, Color.ORANGE)
                            )
                        }
                    }

                    pe = if (pe==null) MoveTo(mapX(x), mapY(y)) else LineTo(mapX(x), mapY(y))
                    path.elements.add(pe)
                    x += inc
                } catch (e: ArithmeticException) {
                    // Function is not continuous in point x
                    x += inc
                }

            children.setAll(axes, path, *circles.toTypedArray())
        }

        private fun ((Double) -> Double).estimateLocalExtreme(x: Double): Double? {
            return try {
                this(x)
            } catch (e: ArithmeticException) {
                null
            }
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

        enum class Direction {
            SAME, UP, DOWN
        }

        infix fun Direction?.isInverse(d: Direction?) = this!=null && d!=null && ((this==Direction.DOWN && d==Direction.UP) || (this==Direction.UP && d==Direction.DOWN))

        class Xxx(val previousDirection: Direction?, val currentDirection: Direction?, val y: Double)
    }
}