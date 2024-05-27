package sp.it.pl.plugin.impl

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.Cursor.CROSSHAIR
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.round
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.plugin.impl.VoiceAssistantWidgetTimeline.HoverType.LABEL
import sp.it.pl.plugin.impl.VoiceAssistantWidgetTimeline.HoverType.TOOLTIP
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.dev.failIf
import sp.it.util.math.P
import sp.it.util.math.abs
import sp.it.util.math.clip
import sp.it.util.math.intersectsWith
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.text.pluralUnit
import sp.it.util.toLocalDateTime
import sp.it.util.ui.anchorPane
import sp.it.util.ui.canvas
import sp.it.util.ui.initMouseDrag
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.rectangle
import sp.it.util.ui.screenXy
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.ui.xy
import sp.it.util.units.em
import sp.it.util.units.formatToSmallestUnit
import sp.it.util.units.millis

class VoiceAssistantWidgetTimeline: StackPane() {

   /** Names groups of events */
   val lines = observableList<Line>()
   val viewSpanMin = vn<Instant>(null)
   val viewSpanMax = vn<Instant>(null)
   val view = v(View())
   val hoverType = v(TOOLTIP)

   private val canvas = canvas({ draw() }) {}
   private val gc = canvas.graphicsContext2D
   private val eventMinWidth = 1.0

   private val hoverL = label { }
   private val hoverR = rectangle { fill = Color.TRANSPARENT }
   private val spanMinL = label { }
   private val spanMaxL = label { }
   private val coordL = label { }
   private val linesL = mutableListOf<Triple<Label, Label, Label>>()
   private val linesLpane = anchorPane { }
   private val hoverT = appTooltip("").apply {
      showDelay = 0.millis
      hideDelay = 0.millis
      isWrapText = true
      maxWidth = 450.emScaled
      opacity = 0.0
      this@VoiceAssistantWidgetTimeline.install(this)
   }

   init {
      prefSize = 500 x 400
      cursor = CROSSHAIR

      lay += canvas.apply {
         isManaged = false
      }
      lay += anchorPane {
         isManaged = false
         isMouseTransparent = true
         lay += hoverL
         lay += hoverR
      }
      lay += linesLpane.apply {
         isManaged = false
         isMouseTransparent = true
      }
      lay(TOP_LEFT) += spanMinL
      lay(TOP_RIGHT) += spanMaxL
      lay(BOTTOM_RIGHT) += coordL

      // redraw
      view attach { draw() }
      lines.onChange { draw() }
      sceneProperty() attach { if (it!=null) draw() }
      // zoom
      onEventDown(SCROLL) { e ->
         if (e.deltaY > 0) view.setValueOf { it.zoom(2*e.x/width-1, 1*1.3).fitViewMinMax() }
         if (e.deltaY < 0) view.setValueOf { it.zoom(2*e.x/width-1, 1/1.3).fitViewMinMax() }
         drawHover(e.xy, e.screenXy)
      }
      // move
      initMouseDrag(view.value, { drag -> drag.data = view.value }) { drag ->
         view.value = drag.data.move(-(view.value.span.toNanos()/width*drag.diff.x).toLong()).fitViewMinMax()
      }
      // hover
      onEventDown(MOUSE_MOVED) {
         drawHover(it.xy, it.screenXy)
      }
      onEventDown(MOUSE_DRAGGED) {
         drawHover(it.xy, it.screenXy)
      }
      // coord
      onEventDown(MOUSE_MOVED) {
         coordL.text = (view.value.startMs+it.x/width*view.value.span.toMillis()).toTimeUiHover()
      }
      view attach {
         spanMinL.text = view.value.start.toUiHover()
         spanMaxL.text = view.value.end.toUiHover()
      }
   }

   override fun layoutChildren() {
      super.layoutChildren()
      for (child in children)
         if (!child.isManaged || child is AnchorPane)
            child.resizeRelocate(0.0, 0.0, width, height)
   }

   private fun draw() {
      if (scene==null) return
      if (height<=0.0) return
      if (lines.isEmpty()) return

      gc.lineWidth = 0.0
      gc.fill = Color.YELLOW
      gc.clearRect(0.0, 0.0, width, height)
      lines.forEachIndexed { i, es -> drawLine(i, es) }
      drawAxes()
   }

   private fun drawLine(i: Int, line: Line) {
      if (lines.isEmpty()) return
      if (lines.size != linesL.size) {
         linesL setTo lines.map { Triple(label(), label(), label()) }
         linesLpane.children setTo linesL.flatMap { listOf(it.first, it.second, it.third) }
      }

      val v = view.value
      val vMin = v.startMs
      val vMax = v.endMs
      val wx = v.span.toMillis().toDouble()/width
      val yBase = height/(lines.size+1)

      val h = 5.0
      var y = yBase + i*yBase
      val esGroups = line.events.groupBy { when { it.toMs<v.startMs -> -1; it.fromMs>v.endMs -> +1; else -> 0 } }

      gc.save()
      gc.globalAlpha = 0.3
      gc.stroke = hoverL.textFill
      gc.lineWidth = hoverR.strokeWidth
      gc.setLineDashes(3.0, 3.0)
      gc.strokeLine(0.0, y, width, y)
      gc.restore()


      for (e in esGroups.get(0).orEmpty()) {
         val x = (e.fromMs-v.startMs)/wx max 0.0
         val w = (e.toMs.coerceAtMost(vMax) - e.fromMs.coerceAtLeast(vMin))/wx
         gc.fillRect(x, y - h/2, w.coerceAtMost(width).coerceAtLeast(eventMinWidth), h)
      }

      linesL[i].first.text = "${line.name} (${"event".pluralUnit(line.events.size)})"
      linesL[i].first.layoutX = 1.em.emScaled
      linesL[i].first.layoutY = yBase + i*yBase-linesL[i].first.height/2 - 1.25.em.emScaled
      linesL[i].second.text = "< " + "event".pluralUnit(esGroups.get(-1).orEmpty().size)
      linesL[i].second.layoutX = 1.em.emScaled
      linesL[i].second.layoutY = yBase + i*yBase + 0.25.em.emScaled
      linesL[i].second.isVisible = esGroups.get(-1).orEmpty().isNotEmpty()
      linesL[i].third.text = "event".pluralUnit(esGroups.get(+1).orEmpty().size) + " >"
      linesL[i].third.layoutX = width - 1.em.emScaled - linesL[i].second.width
      linesL[i].third.layoutY = yBase + i*yBase + 0.25.em.emScaled
      linesL[i].third.isVisible = esGroups.get(+1).orEmpty().isNotEmpty()
   }

   private fun drawHover(xy: P, xyScreen: P) {
      if (lines.isEmpty()) return
      val yBase = height/(lines.size+1)
      val i = ((xy.y-yBase/2)/yBase).toInt().clip(0, lines.lastIndex)
      drawHoverLine(xyScreen, view.value.startMs + xy.x/width*view.value.span.toMillis(), i, lines[i].events)
   }

   private fun drawHoverLine(xyScreen: P, at: Double, i: Int, line: List<Event>) {
      val v = view.value
      val vMin = v.startMs
      val vMax = v.endMs
      val wx = v.span.toMillis().toDouble()/width
      val yBase = height/(lines.size+1)

      val e = findHovered(at, wx, line)
      if (e!=null) {
         if (hoverType.value==LABEL) {
            hoverL.text = "${e.fromMs.toTimeUiHover()}..${e.toMs.toTimeUiHover()}\n\n${e.text.take(40).replace("\n", " ")}"
            hoverL.layoutY = (yBase + i*yBase-hoverL.height/2 - 1.25.em.emScaled).clip(0.0, width)
            hoverL.layoutX = (e.fromMs-v.startMs)/wx
         } else
            hoverL.text = null

         hoverR.isVisible = true
         hoverR.opacity = 0.3
         hoverR.style = "-fx-stroke: linear-gradient(to bottom, transparent, -skin-def-font-color, transparent);"
         hoverR.strokeWidth = 1.0
         hoverR.x = (e.fromMs-v.startMs)/wx
         hoverR.y = i*yBase + yBase/2
         hoverR.width = (e.toMs.coerceAtMost(vMax)-e.fromMs.coerceAtLeast(vMin))/wx
         hoverR.height = yBase

         if (hoverType.value==TOOLTIP) {
            if (!hoverT.isShowing) hoverT.show(this, 0.0, 0.0)
            hoverT.xy = xyScreen + 1.em.emScaled.x2
            hoverT.text = "from: ${e.fromMs.toTimeUiHover()}\n  to: ${e.toMs.toTimeUiHover()}\n dur: ${e.durMs.millis.formatToSmallestUnit()}\n\n${e.text}"
            hoverT.opacity = 1.0
         } else {
            hoverT.opacity = 0.0
            if (hoverT.isShowing) hoverT.hide()
            hoverT.text = null
         }
      } else {
         hoverL.text = null
         hoverR.isVisible = false
         hoverT.opacity = 0.0
         if (hoverT.isShowing) hoverT.hide()
         hoverT.text = null
      }

   }

   private fun drawAxes() {
      val v = view.value
      val wx = v.span.toMillis().toDouble()/width

      gc.save()
      gc.globalAlpha = 0.2
      gc.stroke = hoverL.textFill
      gc.lineWidth = hoverR.strokeWidth
      gc.setLineDashes(3.0, 3.0)

      (v.startMs..v.endMs).axes().forEach { axe ->
         gc.strokeLine((axe-v.startMs)/wx, 0.0, (axe-v.startMs)/wx, height)
      }
      gc.restore()
   }

   private fun findHovered(at: Double, minMs: Double, line: List<Event>): Event? {
      var left = 0
      var right = line.lastIndex
      while (left<=right) {
         val mid = left + right
         val midE = line[mid]
         if (midE.fromMs-minMs/2<=at && at <= midE.toMs+minMs/2) return midE
         if (midE.fromMs<at) left = mid + 1
         else right = mid - 1
      }
      return null
   }

   private fun View.fitViewMinMax(): View {
      val min = viewSpanMin.value
      val max = viewSpanMax.value
      var v = this
      v = if (max!=null && min!=null && Duration.between(min, max)<v.span) View(Duration.between(min, max), v.end) else v
      v = if (max!=null && v.end>max) v.move(max.toEpochNs()-v.endNs.toLong()) else v
      v = if (min!=null && v.start<min) v.move(min.toEpochNs()-v.startNs.toLong()) else v
      return v
   }

   /** Event with ui text, start and end (supporting [Double.NEGATIVE_INFINITY], [Double.POSITIVE_INFINITY] for open interval) */
   data class Event(val text: String, val fromMs: Double, val toMs: Double, val durMs: Double = if (fromMs==NEGATIVE_INFINITY) POSITIVE_INFINITY else if (toMs==POSITIVE_INFINITY) Instant.now().toEpochMilli()-fromMs else toMs-fromMs)

   /** Series of [Event] with ui name */
   data class Line(val name: String, val events: List<Event>)

   /** View of the timeline. */
   data class View(
      val span: Duration = Duration.ofSeconds(20),
      val end: Instant = Instant.now().plusSeconds(10),
      val start: Instant = end.minus(span)
   ) {
      val startMs: Double = start.toEpochMilli().toDouble()
      val startNs: Double = start.toEpochNs().toDouble()
      val endMs: Double = end.toEpochMilli().toDouble()
      val endNs: Double = end.toEpochNs().toDouble()

      /** Zooms in or out by coeficient at <-1,+1> position */
      fun zoom(at: Double = 0.0, by: Double): View {
         failIf(at !in -1.0..+1.0) { "Out of range <-1,+1>" }
         val spanMinNs = 10000L
         val spanNewNs = if (by==0.0) spanMinNs else (span.toNanos().toDouble()/by).toLong() max spanMinNs
         val spanNew = Duration.ofNanos(spanNewNs)
         val spanDiff = spanNewNs/2 - span.toNanos()/2
         val adjustedEndNs = (endNs + (((-at + 1) * spanDiff).toLong())).toLong()
         return View(spanNew, Instant.ofEpochSecond(adjustedEndNs/1_000_000_000, adjustedEndNs%1_000_000_000))
      }

      /** Moves the view forward or backward by a given duration */
      fun move(byNs: Long): View =
         View(span, end.plusNanos(byNs), start.plusNanos(byNs))

      companion object {
         fun between(start: Instant, end: Instant) = View(Duration.ofMillis(end.toEpochMilli()-start.toEpochMilli()), end)
      }
   }

   enum class HoverType { LABEL, TOOLTIP }

   companion object {
      private fun ClosedFloatingPointRange<Double>.axes(): Sequence<Double> {
         val rangeMillis = (endInclusive - start).abs
         val thresholds = listOf(3600000.0, 600000.0, 60000.0, 10000.0, 1000.0, 100.0, 10.0, 1.0)
         val spacing = thresholds.firstOrNull { rangeMillis/it>1 } ?: 1000.0
         val first = round(start.toLong() / spacing)*spacing
         return generateSequence(first) { it + spacing }.takeWhile { it<endInclusive }
      }

      private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
      private fun Instant.toEpochNs() = epochSecond * 1_000_000_000L + nano
      private fun Instant.toUiHover() = formatter.format(toLocalDateTime())
      private fun Double.toTimeUiHover() =
         when (this) {
            POSITIVE_INFINITY -> "+∞"
            NEGATIVE_INFINITY -> "-∞"
            else -> Instant.ofEpochMilli(toLong()).toUiHover()
         }
   }
}