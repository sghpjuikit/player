package voronoi

import java.util.Random
import java.util.stream.IntStream
import javafx.event.Event
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.shape.Rectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.streams.asSequence
import mu.KLogging
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder
import sp.it.pl.layout.ExperimentalController
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.VISUALISATION
import sp.it.pl.main.emScaled
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.pyth
import sp.it.util.access.V
import sp.it.util.animation.Loop
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.functional.runTry
import sp.it.util.math.clip
import sp.it.util.math.min
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.text.*
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year
import voronoi.Voronoi.CellGenerator.CIRCLES
import voronoi.Voronoi.Highlighting.BY_DISTANCE_ORDER
import voronoi.Voronoi.Highlighting.BY_DISTANCE_VALUE
import voronoi.Voronoi.Highlighting.NONE

@ExperimentalController("Only interesting as a demo.")
class Voronoi(widget: Widget): SimpleController(widget) {

   private val canvas = RenderNode()
   val pointCount by cv(200).def(name = "Points", info = "Number of generated points") sync { canvas.pointCount = it }
   val displayed by cv(CIRCLES).def(name = "Pattern", info = "Displayed structure") sync { canvas.displayedToBe = it }
   val highlighting by cv(BY_DISTANCE_VALUE).def(name = "Highlighting", info = "Type of highlighting algorithm") sync { canvas.highlighting = it }

   init {
      root.prefSize = 850.emScaled x 600.emScaled
      root.lay += canvas.color
      canvas.heightProperty() syncFrom root.heightProperty() on onClose
      canvas.widthProperty() syncFrom root.widthProperty() on onClose
      root.lay += canvas

      root.focusedProperty() sync { canvas.pause(!it) } on onClose
      root.onEventDown(MOUSE_CLICKED, PRIMARY) { canvas.pause(false) }
      root.onEventDown(Event.ANY) { if (it !is KeyEvent) it.consume() }

      root.sync1IfInScene { canvas.loop.start() } on onClose
      onClose += { canvas.loop.stop() }
   }

   private class RenderNode: Canvas() {
      val loop: Loop = Loop({ _ -> loop() })
      val gc = graphicsContext2D!!
      val color = Rectangle().apply { isVisible = false; style = "-fx-fill: -skin-def-font-color-hover;" }
      var cells: List<Cell> = listOf()
      var draggedCell: P? = null   // null if none
      var selectedCell: P? = null  // null if none
      var mousePos: P? = null      // null if outside
      var loopId: Long = 0
      private val inputOutputMap = HashMap<Coordinate, Cell>() // input -> polygons
      val running = V(true)
      private var displayedCurrent: CellGenerator? = null
      private var displayedPointCount: Int? = null
      var pointCount: Int = 40
      var displayedToBe: CellGenerator? = null
      var highlighting = BY_DISTANCE_ORDER

      init {
         onEventDown(MOUSE_PRESSED, PRIMARY) { draggedCell = selectedCell }
         onEventDown(MOUSE_RELEASED, PRIMARY) { draggedCell = null }
         onEventDown(MOUSE_DRAGGED, PRIMARY) {
            mousePos = P(it.x, it.y)
            draggedCell?.x = it.x
            draggedCell?.y = it.y
         }
         onEventDown(MOUSE_MOVED) { mousePos = P(it.x, it.y) }
         onEventDown(MOUSE_EXITED) { mousePos = null }
      }

      fun loop() {
         val w = width
         val h = height
         if (w<=0 || h<=0) return

         loopId++

         if (displayedCurrent!=displayedToBe || displayedPointCount!=pointCount) {
            displayedCurrent = displayedToBe
            displayedPointCount = pointCount
            displayedCurrent?.let {
               cells = it.generator(CellGeneratorSeed(w, h, pointCount)).toList()
            }
         }

         selectedCell = mousePos?.let { p -> cells.minByOrNull { it.distance(p) } }
         cells.forEach { it.moving?.invoke(w, h) }

         draw()
      }

      fun pause(v: Boolean) {
         if (running.value==v) return
         if (v) loop.stop() else loop.start()
      }

      fun draw() {
         inputOutputMap.clear()
         val c = color.fill
         val w = width
         val h = height
         val opacityMin = 0.1
         val opacityMax = 0.5
         val distMin = 0.0
         val distMax = 0.2*pyth(w, h)
         val distDiff = distMax - distMin
         val distances = when (highlighting) {
            NONE -> cells.associateWith { opacityMin }
            BY_DISTANCE_VALUE -> cells.associateWith {
               val dist = (mousePos?.distance(it.x, it.y) ?: distMax).clip(distMin, distMax)
               val distNormalized = (1 - (dist - distMin)/distDiff).clip(opacityMin, opacityMax)
               distNormalized
            }
            BY_DISTANCE_ORDER -> when (val mp = mousePos) {
               null -> cells.associateWith { opacityMin }
               else -> cells.sortedBy { mp.distance(it.x, it.y) }.withIndex().associate { (i, it) ->
                  val steps = 30 min cells.size
                  it to when {
                     (i>10) -> opacityMin
                     else -> {
                        val opacityStep = (opacityMax - opacityMin)/steps
                        val opacityI = (steps - i)
                        opacityI*opacityStep
                     }
                  }
               }
            }
         }

         gc.setEffect(null)
         gc.fill = c
         gc.stroke = c
         gc.clearRect(0.0, 0.0, w, h)
         gc.globalAlpha = 0.5

         val cords = cells.map {
            Coordinate(it.x, it.y).apply {
               inputOutputMap[this] = it
            }
         }
         val diagram = VoronoiDiagramBuilder().apply {
            setClipEnvelope(Envelope(0.0, w, 0.0, h))
            setSites(cords)
         }

         // draw cells
         gc.save()

         runTry {
            // the computation can fail under some circumstances, so defend against it with Try
            diagram.getDiagram(GeometryFactory()) as Geometry
         }.ifError {
            logger.warn(it) { "Computation of Voronoi diagram failed" }
         }.ifOk { g ->
            IntStream.range(0, g.numGeometries).asSequence()
               .map { g.getGeometryN(it) }
               .forEach { polygon ->

                  val cell = inputOutputMap[polygon.userData as Coordinate]!!
                  val cs = polygon.coordinates
                  val xs = DoubleArray(cs.size)
                  val ys = DoubleArray(cs.size)
                  for (j in cs.indices) {
                     xs[j] = cs[j].x
                     ys[j] = cs[j].y
                  }

                  val isSelected = selectedCell?.let { cell.x==it.x && cell.y==it.y } ?: false
                  val isDragged = draggedCell==null
                  if (isSelected) {
                     gc.globalAlpha = if (isDragged) 0.1 else 0.15
                     gc.fillPolygon(xs, ys, polygon.numPoints)
                  }

                  gc.globalAlpha = distances[cell] ?: opacityMin
                  strokePolygon(gc, polygon)
               }
         }
         gc.restore()

         // draw cell seeds
         gc.save()
         val r = 2.0
         val rd = 4.0
         cells.forEach { gc.fillOval(it.x - r, it.y - r, 2*r, 2*r) }
         selectedCell?.let { gc.fillOval(it.x - rd, it.y - rd, 2*rd, 2*rd) }
         gc.restore()
      }
   }

   data class CellGeneratorSeed(
      val width: Double,
      val height: Double,
      val count: Int
   )

   enum class Highlighting {
      NONE,
      BY_DISTANCE_VALUE,
      BY_DISTANCE_ORDER
   }

   enum class CellGenerator(val generator: (CellGeneratorSeed) -> Sequence<Cell>) {
      RANDOM({
         generateSequence { Cell.random(it.width, it.height, 0.5) }.take(it.count)
      }),
      CIRCLES({
         val wh = it.width min it.height
         val cells = ArrayList<Cell>()

         cells += generateSequence(0.0) { it + 2*PI/11 }.take(11)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = { _, _ ->
                        angle += 0.001
                        x = wh/2 + wh/20*cos(angle)
                        y = wh/2 + wh/20*sin(angle)
                        x += randOf(-1, 1)*randMN(0.0005, 0.00051)
                        y += randOf(-1, 1)*randMN(0.0005, 0.00051)
                     }
                  }
               }
            }
         cells += generateSequence(0.0) { it + 2*PI/3 }.take(3)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = { _, _ ->
                        angle -= 0.002
                        x = wh/2 + wh/10*cos(angle)
                        y = wh/2 + wh/10*sin(angle)
                        x += randOf(-1, 1)*randMN(0.0005, 0.00051)
                        y += randOf(-1, 1)*randMN(0.0005, 0.00051)
                     }
                  }
               }
            }

         cells += generateSequence(0.0) { a -> a + 2*PI/it.count }.take(it.count)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = { _, _ ->
                        angle -= 0.002
                        x = wh - wh/6 + wh/8*cos(angle)
                        y = wh/6 + wh/8*sin(angle)
                        x += randOf(-1, 1)*randMN(0.0005, 0.00051)
                        y += randOf(-1, 1)*randMN(0.0005, 0.00051)
                     }
                  }
               }
            }
         cells += generateSequence(0.0) { a -> a + 2*PI/it.count }.take(it.count)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = { _, _ ->
                        angle -= 0.002
                        x = wh/2 + wh/4*cos(angle)
                        y = wh/2 + wh/4*sin(angle)
                        x += randOf(-1, 1)*randMN(0.0005, 0.00051)
                        y += randOf(-1, 1)*randMN(0.0005, 0.00051)
                     }
                  }
               }
            }

         cells.forEach {
            it.moving = it.moving ?: { w, h ->
               val x = it.x + it.dx
               val y = it.y + it.dy
               when {
                  x<0 -> {
                     it.dx = -it.dx
                     it.x = -x
                  }
                  x>w -> {
                     it.dx = -it.dx
                     it.x = 2*w - x
                  }
                  else -> it.x = x
               }
               when {
                  y<0 -> {
                     it.dy = -it.dy
                     it.y = -y
                  }
                  y>h -> {
                     it.dy = -it.dy
                     it.y = 2*h - y
                  }
                  else -> it.y = y
               }
            }
         }

         cells.asSequence()
      }),
      HORIZONTAL_LINE({
         (0..it.count).asSequence().map { i ->
            Cell(it.width*0.1 + it.width*0.8/(it.count-1)*i, it.height/2.0)
         }
      }),
      SPIRAL({
         val periods = 3
         val radiusMax = it.width/2.0 min it.height/2.0
         (0..it.count).asSequence().map { i -> i.toDouble()/(it.count-1) }.map { i -> Cell(
            it.width/2.0  + i*radiusMax * cos(i*periods*2*PI),
            it.height/2.0 + i*radiusMax * sin(i*periods*2*PI)
         )}
      }),
      PHYLLOTAXIS({
         val theta = PI*(3.0 - sqrt(5.0))
         val cellWidth = 45.0
         val cellRadius = cellWidth/2.0

         (0..it.count).asSequence().map { i ->
            val iOffset = 0
            val index = (i + iOffset) % it.count
            val phyllotaxisX = cellRadius * sqrt(index.toDouble()) * cos(index * theta)
            val phyllotaxisY = cellRadius * sqrt(index.toDouble()) * sin(index * theta)

            Cell(
               it.width/2.0  + phyllotaxisX - cellRadius,
               it.height/2.0 + phyllotaxisY - cellRadius
            )
         }
      }),
      SINE({
         val amplitude = 0.3 * it.height /2.0
         val periods = 3

         (0..it.count).asSequence().map { i -> i.toDouble()/(it.count-1) }.map { i -> Cell(
            i*it.width,
            it.height/2.0 + amplitude*sin(i*periods*2*PI)
         )}
      });
   }

   open class P(var x: Double, var y: Double) {
      fun distance(p: P): Double = distance(p.x, p.y)
      fun distance(x: Double, y: Double): Double = sqrt((x - this.x)*(x - this.x) + (y - this.y)*(y - this.y))
   }

   class Line(var x1: Double, var y1: Double, var x2: Double, var y2: Double) {

      override fun equals(other: Any?): Boolean {
         if (this===other) return true
         if (other !is Line) return false
         return (dc(other.x1, x1)==0 && dc(other.x2, x2)==0 && dc(other.y1, y1)==0 && dc(other.y2, y2)==0) ||
            (dc(other.x1, x2)==0 && dc(other.x2, x1)==0 && dc(other.y1, y2)==0 && dc(other.y2, y1)==0)
      }

      override fun hashCode(): Int {
         var result: Int
         var temp: Long
         if (x1<x2) {
            temp = java.lang.Double.doubleToLongBits(x1)
            result = (temp xor temp.ushr(32)).toInt()
            temp = java.lang.Double.doubleToLongBits(y1)
            result = 31*result + (temp xor temp.ushr(32)).toInt()
            temp = java.lang.Double.doubleToLongBits(x2)
            result = 31*result + (temp xor temp.ushr(32)).toInt()
            temp = java.lang.Double.doubleToLongBits(y2)
            result = 31*result + (temp xor temp.ushr(32)).toInt()
         } else {
            temp = java.lang.Double.doubleToLongBits(x2)
            result = (temp xor temp.ushr(32)).toInt()
            temp = java.lang.Double.doubleToLongBits(y2)
            result = 31*result + (temp xor temp.ushr(32)).toInt()
            temp = java.lang.Double.doubleToLongBits(x1)
            result = 31*result + (temp xor temp.ushr(32)).toInt()
            temp = java.lang.Double.doubleToLongBits(y1)
            result = 31*result + (temp xor temp.ushr(32)).toInt()
         }
         return result
      }
   }

   open class Cell(x: Double, y: Double): P(x, y) {
      var dx = 0.0
      var dy = 0.0
      var moving: ((Double, Double) -> Unit)? = null

      fun moving(moving: (Double, Double) -> Unit): Cell {
         this.moving = moving
         return this
      }

      companion object {
         fun random(width: Double, height: Double, speed: Double): Cell {
            return Cell(rand0N(width), rand0N(height)).apply {
               dx = rand0N(speed) - speed/2
               dy = rand0N(speed) - speed/2
            }
         }
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Voronoi"
      override val description = "Playground to visualize and experiment with voronoi diagrams"
      override val descriptionLong = "$description.\nThe visualization is customizable through settings."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2016)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         Entry("Interact", "Highlight", "Move cursor"),
         Entry("Interact", "Move cell", "${PRIMARY.nameUi} Drag"),
      )
      override val tags = setOf(VISUALISATION)

      private var rand = Random()

      infix fun <T> MutableList<T>.with(elements: Sequence<T>) {
         this += elements
      }

      fun dc(x: Double, y: Double) = x.compareTo(y)

      fun randBoolean(): Boolean = rand.nextBoolean()

      fun rand0N(n: Double): Double = rand.nextDouble()*n

      fun randMN(m: Double, n: Double): Double = m + rand0N(n - m)

      fun <T> randOf(a: T, b: T): T = if (randBoolean()) a else b

      fun strokePolygon(gc: GraphicsContext, polygon: Geometry) {
         val cs = polygon.coordinates
         for (j in 0 until cs.size - 1)
            gc.strokeLine(cs[j].x, cs[j].y, cs[j + 1].x, cs[j + 1].y)
         gc.strokeLine(cs[0].x, cs[0].y, cs[cs.size - 1].x, cs[cs.size - 1].y)
      }
   }

}