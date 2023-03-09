package voronoi

import java.lang.Math.random
import javafx.event.Event
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode.F5
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.shape.Rectangle
import javafx.stage.Window
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.properties.Delegates.observable
import mu.KLogging
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.VISUALISATION
import sp.it.pl.main.emScaled
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.pyth
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.animation.Loop
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.functional.runTry
import sp.it.util.math.clip
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.into
import sp.it.util.reactive.intoLater
import sp.it.util.reactive.notNull
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.text.*
import sp.it.util.ui.canvas
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year
import voronoi.Voronoi.CellGenerator.CIRCLES
import voronoi.Voronoi.Highlighting.BY_DISTANCE_ORDER
import voronoi.Voronoi.Highlighting.BY_DISTANCE_VALUE
import voronoi.Voronoi.Highlighting.NONE

class Voronoi(widget: Widget): SimpleController(widget) {

   private val canvas = RenderNode(onClose)
   val pointCount by cv(200).def(name = "Points", info = "Number of generated points") sync { canvas.pointCount = it }
   val displayed by cv(CIRCLES).def(name = "Pattern", info = "Displayed structure") sync { canvas.displayedToBe = it }
   val highlighting by cv(BY_DISTANCE_VALUE).def(name = "Highlighting", info = "Type of highlighting algorithm") sync { canvas.highlighting = it }
   val animate by cv(true).def(name = "Animate", info = "Animate view (if view supports it)")
   val pauseOnFocus by cv(true).def(name = "Animation pause on no focus", info = "Pause animating when window loses focus")

   init {
      root.prefSize = 850.emScaled x 600.emScaled
      root.lay += canvas.color
      root.lay += canvas.canvas

      val windowFocused = root.sceneProperty().into(Scene::windowProperty).into(Window::focusedProperty).notNull(false).intoLater()
      var windowFocusedOnPress = false

      canvas.animate syncFrom animate
      canvas.hasFocus syncFrom (windowFocused zip pauseOnFocus).map { (a,b) -> a || !b }
      root.isFocusTraversable = true
      root.onEventUp(MOUSE_PRESSED, PRIMARY, false) { windowFocusedOnPress = windowFocused.value }
      root.onEventDown(MOUSE_CLICKED, PRIMARY) { if (it.isStillSincePress && (!pauseOnFocus.value || windowFocusedOnPress)) animate.toggle() }
      root.onEventDown(KEY_RELEASED, SPACE) { animate.toggle() }
      root.onEventDown(KEY_RELEASED, F5) { canvas.needsRefresh=true; canvas.loopDirty() }
      root.onEventDown(Event.ANY) { if (it !is KeyEvent) it.consume() }
   }

   private class RenderNode(onClose: Disposer) {
      val loop: Loop = Loop({ _ -> loop() })
      val canvas = canvas({})
      val gc = canvas.graphicsContext2D!!
      val color = Rectangle().apply { isVisible = false; style = "-fx-fill: -skin-def-font-color-hover;" }
      var cells: List<Cell> = listOf()
      var draggedCell: P? = null   // null if none
      var selectedCell: P? = null  // null if none
      var mousePos: P? = null      // null if outside
      var loopId: Long = 0
      private val inputOutputMap = HashMap<Coordinate, Cell>() // input -> polygons
      private var displayedCurrent: CellGenerator? = null
      private var displayedPointCount: Int? = null
      var pointCount: Int  by observable(40) { _,_,_ -> loopDirty() }
      var displayedToBe: CellGenerator? by observable(null) { _,_,_ -> loopDirty() }
      var highlighting = BY_DISTANCE_ORDER
      val animate = v(true)
      val hasFocus = v(false)
      var needsRefresh = false

      init {
         // start
         canvas.sync1IfInScene { animate zip hasFocus sync { (a, b) -> animate(a && b) } } on onClose
         // stop
         onClose += { loop.stop() }

         canvas.onEventDown(MOUSE_PRESSED, PRIMARY, false) {
            draggedCell = selectedCell
            drawDirty()
         }
         canvas.onEventDown(MOUSE_RELEASED, PRIMARY, false) {
            draggedCell = null
            drawDirty()
         }
         canvas.onEventDown(MOUSE_DRAGGED, PRIMARY, false) {
            mousePos = P(it.x, it.y)
            draggedCell?.x = it.x
            draggedCell?.y = it.y
            drawDirty()
         }
         canvas.onEventDown(MOUSE_MOVED) {
            mousePos = P(it.x, it.y)
            drawDirty()
         }
         canvas.onEventDown(MOUSE_EXITED) {
            mousePos = null
            drawDirty()
         }
      }

      fun loopDirty() {
         if (!loop.isRunning()) loop()
      }

      fun loop() {
         val w = canvas.width
         val h = canvas.height
         if (w<=0 || h<=0) return

         loopId++
         val seedMove = CellMoveSeed(w, h, w min h, canvas)
         val seedGen = CellGeneratorSeed(w, h, pointCount)

         if (displayedCurrent!=displayedToBe || displayedPointCount!=pointCount || needsRefresh) {
            needsRefresh = false
            displayedCurrent = displayedToBe
            displayedPointCount = pointCount
            displayedCurrent?.let {
               cells = it.generator(seedGen).toList()
            }
         }
         cells.forEach { if (it!==draggedCell) it.moving?.invoke(seedMove) }

         draw()
      }

      fun animate(v: Boolean) {
         if (!v && !loop.isRunning()) loopDirty()
         if (v) loop.start() else loop.stop()
      }

      fun drawDirty() {
         if (!loop.isRunning()) draw()
      }

      fun draw() {
         selectedCell = mousePos?.let { p -> cells.minByOrNull { it.distance(p) } }
         inputOutputMap.clear()
         val c = color.fill
         val w = canvas.width
         val h = canvas.height
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
            (0 until g.numGeometries).forEach { i ->
               val polygon = g.getGeometryN(i)
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

         gc.save()
         gc.globalAlpha = 0.25
         gc.strokeRect(0.0, 0.0, w, h)
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

   data class CellGeneratorSeed(val width: Double, val height: Double, val count: Int)
   data class CellMoveSeed(val w: Double, val h: Double, val wh: Double, val canvas: Canvas)

   enum class Highlighting {
      NONE,
      BY_DISTANCE_VALUE,
      BY_DISTANCE_ORDER
   }

   enum class CellGenerator(val generator: (CellGeneratorSeed) -> Sequence<Cell>) {
      RANDOM({
         generateSequence { Cell.random(it.width, it.height, 0.5) }.take(it.count)
            .onEach {
               var dx = rand0N(.4)-.2
               var dy = rand0N(.4)-.2
               it.moving = {
                  if (it.x !in 0.0..canvas.width) dx = -dx
                  if (it.y !in 0.0..canvas.height) dy = -dy
                  it.x += dx
                  it.y += dy
               }
            }
      }),
      CIRCLES({
         val cells = ArrayList<Cell>()

         cells += generateSequence(0.0) { it + 2*PI/11 }.take(11)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = {
                        angle += 0.001
                        x = wh/2 + wh/20*cos(angle)
                        y = wh/2 + wh/20*sin(angle)
                     }
                  }
               }
            }
         cells += generateSequence(0.0) { it + 2*PI/3 }.take(3)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = {
                        angle -= 0.002
                        x = wh/2 + wh/10*cos(angle)
                        y = wh/2 + wh/10*sin(angle)
                     }
                  }
               }
            }

         cells += generateSequence(0.0) { a -> a + 2*PI/it.count }.take(it.count)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = {
                        angle -= 0.002
                        x = wh - wh/6 + wh/8*cos(angle)
                        y = wh/6 + wh/8*sin(angle)
                     }
                  }
               }
            }
         cells += object: Cell(0.0, 0.0) {

               init {
                  moving = {
                     x = wh - wh/6
                     y = wh/6
                  }
               }
         }
         cells += generateSequence(0.0) { a -> a + 2*PI/it.count }.take(it.count)
            .map { a ->
               object: Cell(0.0, 0.0) {
                  var angle = a

                  init {
                     moving = {
                        angle -= 0.002
                        x = wh/2 + wh/4*cos(angle)
                        y = wh/2 + wh/4*sin(angle)
                     }
                  }
               }
            }

         cells.forEach {
            it.moving = it.moving ?: {
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

   open class Cell(x: Double, y: Double): P(x, y) {
      var dx = 0.0
      var dy = 0.0
      var moving: (CellMoveSeed.() -> Unit)? = null

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
         Entry("View", "Refresh", keys(F5)),
         Entry("View", "Toggle animation", keys(SPACE)),
         Entry("View", "Toggle animation", PRIMARY.nameUi),
      )
      override val tags = setOf(VISUALISATION)

      fun rand0N(n: Double): Double = random()*n

      fun strokePolygon(gc: GraphicsContext, polygon: Geometry) {
         val cs = polygon.coordinates
         for (j in 0 until cs.size - 1)
            gc.strokeLine(cs[j].x, cs[j].y, cs[j + 1].x, cs[j + 1].y)
         gc.strokeLine(cs[0].x, cs[0].y, cs[cs.size - 1].x, cs[cs.size - 1].y)
      }
   }

}