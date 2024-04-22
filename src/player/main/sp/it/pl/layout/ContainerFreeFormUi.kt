package sp.it.pl.layout

import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Pos.BASELINE_CENTER
import javafx.geometry.Pos.BASELINE_LEFT
import javafx.geometry.Pos.BASELINE_RIGHT
import javafx.geometry.Pos.BOTTOM_CENTER
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.AnchorPane
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import sp.it.pl.core.NameUi
import sp.it.pl.layout.ContainerFreeFormUi.WindowPositionType.RELATIVE
import sp.it.pl.layout.Layouter.Companion.suppressHidingFor
import sp.it.pl.layout.WidgetUi.Companion.PSEUDOCLASS_DRAGGED
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Ui.ICON_CLOSE
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.Resize
import sp.it.pl.ui.objects.window.pane.PaneWindowControls
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.toggle
import sp.it.util.access.vAlways
import sp.it.util.async.runFX
import sp.it.util.collections.observableSet
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.functional.Util.findFirstEmptyKey
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.runnable
import sp.it.util.math.P
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.ui.initClipToPadding
import sp.it.util.ui.layFullArea
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.x
import sp.it.util.units.millis

class ContainerFreeFormUi(c: ContainerFreeForm): ContainerUi<ContainerFreeForm>(c) {
   private val windows: MutableMap<Int, Window> = HashMap()
   private var isResizing = false
   private val isAnyWindowMoving = observableSet<Int>()
   private val isAnyWindowResizing = observableSet<Int>()
   private val content = object: AnchorPane() {
//      override fun layoutChildren() {
//         super.layoutChildren()
//         isResizing = true
//         windows.forEach { (_, w) ->
//            w.snappable.value = false
//            w.updatePosition()
//            w.snappable.value = APP.ui.snapping.value
//         }
//         isResizing = false
//      }
   }

   init {
      root.minSize = 0 x 0
      root.maxSize = Double.MAX_VALUE x Double.MAX_VALUE
      root.styleClass += "container-freeform-ui"
      root.layFullArea += content

      content.initClipToPadding()
      isAnyWindowMoving.onChange { root.pseudoClassChanged(PC_MOVING, isAnyWindowMoving.isNotEmpty()) }
      isAnyWindowResizing.onChange { root.pseudoClassChanged(PC_RESIZING, isAnyWindowResizing.isNotEmpty()) }

      // add new widget on left click
      content.onEventDown(MOUSE_CLICKED, PRIMARY, false) { e ->
         if (e.isStillSincePress && !isContainerMode && (APP.ui.isLayoutMode || !container.lockedUnder.value) && isAnyWindowResizing.isEmpty()) {
            addEmptyWindowAt(e.x, e.y)
            e.consume()
         }
      }

      // drag
      content.onDragDone = EventHandler { content.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false) }
      content.installDrag(
         IconFA.EXCHANGE, { "Move component here" },
         { Df.COMPONENT in it.dragboard },
         { it.dragboard[Df.COMPONENT]===container },
         { it.dragboard[Df.COMPONENT].swapWith(container, addEmptyWindowAt(it.x, it.y)) },
         { bestRec(P(it.x, it.y), null).absolute }
      )

      content.widthProperty() attach {
         isResizing = true
         windows.forEach { (_, w) ->
            w.snappable.value = false
            w.updatePosition()
            w.snappable.value = APP.ui.snapping.value
         }
         isResizing = false
      }
      content.heightProperty() attach {
         isResizing = true
         windows.forEach { (_, w) ->
            w.snappable.value = false
            w.updatePosition()
            w.snappable.value = APP.ui.snapping.value
         }
         isResizing = false
      }
//      content.layoutBoundsProperty() attach {
//         isResizing = true
//         windows.forEach { (_, w) ->
//            w.snappable.value = false
//            w.updatePosition()
//            w.snappable.value = APP.ui.snapping.value
//         }
//         isResizing = false
//      }
   }

   override fun buildControls() = super.buildControls().apply {
      Icon(IconMD.VIEW_DASHBOARD, -1.0, autoLayoutTooltipText).onClickDo { autoLayoutAll() }.styleclass("header-icon").addExtraIcon()
      Icon(IconFA.HEADER, -1.0, "Show window headers.").onClickDo { container.showHeaders.toggle() }.styleclass("header-icon").addExtraIcon()
   }

   fun load() {
      container.children.forEach(this::load)
   }

   fun load(i: Int, c: Component?) {
      val w = getOrBuildWindow(i, c)
      val r = w.content
      val n: Node
      val a: AltState?
      when (c) {
         is Container<*> -> {
            w.ui?.dispose()
            w.ui = null
            n = c.load(r)
            a = c
            c.ui?.asIf<ContainerUi<*>>()?.let { it.controls.ifInitialized { it.updateIcons() } }
         }
         is Widget -> {
            w.ui = null
               ?: run { if (w.ui is WidgetUi && (w.ui as WidgetUi?)!!.widget==c) w.ui else null }
               ?: run {
                  w.ui?.dispose()
                  WidgetUi(container, i, c).apply {
                     val lb = Icon(IconMD.VIEW_DASHBOARD, 12.0, layoutButtonTooltipText, { w.autoLayout() })
                     controls.icons.children.add(0, lb)
                  }
               }
            n = w.ui!!.root
            a = w.ui
         }
         null -> {
            w.ui = null
               ?: run { w.ui?.takeIf { it is Layouter } }
               ?: run {
                  w.ui?.dispose()
                  Layouter(container, i, w.borders).apply {
                     onCancel = runnable<Any> { container.removeChild(i) }
                     root.onEventDown(MOUSE_CLICKED, SECONDARY) { hide() }
                  }
               }
            n = w.ui!!.root
            a = w.ui
         }
      }
      w.moveOnDragOf(w.root)
      w.setContent(n)
      if (c==null && a!=null) a.show()
   }

   fun closeWindow(i: Int) {
      isAnyWindowResizing -= i
      isAnyWindowMoving -= i
      windows.remove(i).ifNotNull { w ->
         w.close()
         val c = container.children[i]
         if (c==null || "reloading=${c.id}" !in container.properties) {
            container.properties -= "${i}x"
            container.properties -= "${i}y"
            container.properties -= "${i}w"
            container.properties -= "${i}h"
            container.properties -= "window-${i}"
         }
      }
   }

   private fun getWindow(i: Int?): Window? = if (i==null) null else windows[i]

   private fun getOrBuildWindow(i: Int, c: Component?): Window {
      return windows.getOrPut(i) {
         val w = Window(i, content)

         w.open()
         w.resizeHalf()
         w.alignCenter()
         w.snappable.value = false
         w.root.onEventDown(MOUSE_CLICKED, PRIMARY) {}

         container.properties["window-${i}"].asIs<WindowPosition?>().ifNotNull(w.position::setTo)
         container.properties["window-${i}"] = w.position
         w.updatePosition()
         w.x attach { w.positionUpdate() }
         w.y attach { w.positionUpdate() }
         w.w attach { w.positionUpdate() }
         w.h attach { w.positionUpdate() }
         w.resizing attach { if (it==Resize.NONE) w.positionUpdate() }
         w.snapDistance syncFrom APP.ui.snapDistance on w.disposer
         w.snappable syncFrom APP.ui.snapping on w.disposer
         container.lockedUnder zip APP.ui.layoutMode sync { (l1, l2) -> w.resizable.value = (!l1 || l2) } on w.disposer
         container.lockedUnder zip APP.ui.layoutMode sync { (l1, l2) -> w.movable.value = (!l1 || l2) } on w.disposer
         container.showHeaders sync { w.updateHeader() } on w.disposer
         w.titleL.textProperty() syncFrom (if (c is Widget) c.customName else vAlways("")) on w.disposer
         w.resizing attach { if (it==Resize.NONE) runFX(100.millis) { isAnyWindowResizing -= i } else isAnyWindowResizing += i }
         w.moving attach { if (it) isAnyWindowMoving += i else isAnyWindowMoving -= i }

         // prevent layouter closing when resize/move
         w.resizing sync { suppressHidingFor(w.borders, it!=Resize.NONE) }
         w.moving sync { suppressHidingFor(w.borders, it) }

         // report component graphics changes
         w.root.parentProperty() sync { IOLayer.requestLayoutFor(container) }
         w.root.boundsInParentProperty() sync { IOLayer.requestLayoutFor(container) }

         w
      }
   }

   /** Optimal size/position strategy returning greatest empty square. */
   private fun bestRec(at: P, newW: Window?): BestRec {
      if (windows.isEmpty())
         return BestRec(TupleM4(0.0, 0.0, 1.0, 1.0), BoundingBox(0.0, 0.0, content.width, content.height))

      // compute cells
      // cell is a unique rectangle in a grid defined by unique x and y axes of total area and all window edges
      data class I(val x: Int, val y: Int)
      data class Cell(val i: I, val lt: P, val c: P, val rb: P, var freeRecSize: I)
      val cells = hashMapOf<I, Cell>()
      val ws = windows.values - listOfNotNull(newW)
      val xs = (listOf(0.0) + ws.flatMap { listOf(it.x.value, it.x.value + it.w.value) } + listOf(content.width)).distinct().sorted().windowed(2, 1, false).map { it[0] to it[1] }
      val ys = (listOf(0.0) + ws.flatMap { listOf(it.y.value, it.y.value + it.h.value) } + listOf(content.height)).distinct().sorted().windowed(2, 1, false).map { it[0] to it[1] }
      xs.forEachIndexed { xi, x ->
         ys.forEachIndexed { yi, y ->
            val i = I(xi, yi)
            val lt = x.first x y.first
            val c = (x.first + x.second)/2 x (y.first + y.second)/2
            val rb = x.second x y.second
            cells[i] = Cell(i, lt, c, rb, I(1,1))
         }
      }

      // initialize empty/full state
      // cell is full if it is covered by any window
      cells.values.forEach {
         val full = windows.values.any { w -> w.x.value + w.w.value>it.lt.x && w.y.value + w.h.value>it.lt.y && w.x.value<it.rb.x && w.y.value<it.rb.y }
         it.freeRecSize = if (full) I(0, 0) else I(1, 1) // full cells have freeRecSize=0
      }

      // area is rectangle of cells defined by left top cell and size in cell count
      data class Area(val i: I, val size: I) {
         val ltCell by lazy { cells[I(i.x, i.y)]!! }
         val rbCell by lazy { cells[I(i.x+size.x, i.y+size.y)]!! }
         val width by lazy { rbCell.rb.x - ltCell.lt.x }
         val height by lazy { rbCell.rb.y - ltCell.lt.y }
         val area by lazy { width * height }
         fun contains(p: P) = ltCell.lt.x<=p.x && p.x<=rbCell.rb.x && ltCell.lt.y<=p.y && p.y<=rbCell.rb.y
         fun best() = BestRec(
            TupleM4(ltCell.lt.x/content.width, ltCell.lt.y/content.height, width/content.width, height/content.height),
            BoundingBox(ltCell.lt.x, ltCell.lt.y, width, height)
         )
         override fun toString() = "Area $i $size $width x $height"
      }

      // iterate cells from right to left, bottom to top
      var areaMax: Area? = null
      for (ix in xs.indices.reversed()) {
         for (iy in ys.indices.reversed()) {
            val i = I(ix, iy)
            val cell = cells[i]!!
            val cellRight = cells[I(ix+1, iy)]
            val cellBottom = cells[I(ix, iy+1)]
            if (cell.freeRecSize.x!=0) {
               // compute cell freeRecSize - how many cells to right and down are empty (this determines the direction of the algorithm)
               cell.freeRecSize = I(1 + (cellRight?.freeRecSize?.x ?: 0), 1 + (cellBottom?.freeRecSize?.y ?: 0))
               // compute the greatest area starting at current cell
               // at this point all cells to the right and down are updated
               // thus the largest are a for given row is minimum freeRecSize of any row above
               // thus single iteration is enough to check all N^2 areas starting at current cell
               var max: Int = cell.freeRecSize.x
               (iy..ys.lastIndex).forEach { maxY ->
                  max = max min (cells[I(ix, maxY)]?.freeRecSize?.x ?: 0)
                  val area = Area(i, I(max-1, maxY-iy))
                  if (area.contains(at) && (areaMax?.area ?: 0.0) < area.area) areaMax = area
               }
            }
         }
      }
      return if (areaMax==null) BestRec(TupleM4(0.0, 0.0, 1.0, 1.0), BoundingBox(0.0, 0.0, content.width, content.height))
      else areaMax!!.best()
   }

   /** Initializes position & size for i-th window, ignoring self w if passed as param.  */
   private fun storeBestRec(i: Int, x: Double, y: Double) {
      val (rel, abs) = bestRec(P(x, y), null)
      // add empty window at index
      // the method call eventually invokes load() method below, with
      // component/child == null (3rd case)
      container.properties["${i}x"] = rel.a
      container.properties["${i}y"] = rel.b
      container.properties["${i}w"] = rel.c + rel.a
      container.properties["${i}h"] = rel.d + rel.b
      container.properties["window-${i}"] = (container.properties["window-${i}"].asIs() ?: WindowPosition()).apply {
         minXAbs = abs.minX
         minXRel = rel.a
         maxXAbs = abs.maxX
         maxXRel = rel.c + rel.a
         minYAbs = abs.minY
         minYRel = rel.b
         maxYAbs = abs.maxY
         maxYRel = rel.d + rel.b
         wAbs = abs.width
         wRel = abs.width/content.width
         hAbs = abs.height
         hRel = abs.height/content.height
      }
   }

   private fun addEmptyWindowAt(x: Double, y: Double): Int {
      val i = findFirstEmptyKey(container.children, 1)
      storeBestRec(i, x, y)
      // add empty window at index (into viable area)
      // the method call eventually invokes load() method below, with
      // component/child == null (3rd case)
      container.addChild(i, null)
      return i
   }

   fun autoLayout(c: Component) {
      c.indexInParent().ifNotNull { windows[it]?.autoLayout() }
   }

   fun autoLayoutAll() {
      windows.forEach { (_, w) -> w.autoLayout() }
   }

   private data class BestRec(var relative: TupleM4, var absolute: Bounds)

   private data class TupleM4(var a: Double, var b: Double, var c: Double, var d: Double)

   private inner class Window(val i: Int, owner: AnchorPane?): PaneWindowControls(owner) {
      var ui: ComponentUi? = null
      var disposer = Disposer()

      init {
         root.styleClass += "container-free-form-window"
         offScreenFixOn.value = false
      }

      override fun close() {
         disposer()
         super.close()
      }

      val position = WindowPosition()
      val positionUpdate = Suppressor()
      fun positionUpdate() {
         if (!this@ContainerFreeFormUi.isResizing)
            positionUpdate.suppressing {
               updatePosition.suppressed {
                  position.minXAbs = x.value
                  position.minXRel = x.value/this@ContainerFreeFormUi.content.width
                  position.maxXAbs = x.value+w.value
                  position.maxXRel = (x.value+w.value)/this@ContainerFreeFormUi.content.width
                  position.minYAbs = y.value
                  position.minYRel = y.value/this@ContainerFreeFormUi.content.height
                  position.maxYAbs = y.value+h.value
                  position.maxYRel = (y.value+h.value)/this@ContainerFreeFormUi.content.height
                  position.wAbs = w.value
                  position.wRel = w.value/this@ContainerFreeFormUi.content.width
                  position.hAbs = h.value
                  position.hRel = h.value/this@ContainerFreeFormUi.content.height
               }
            }
      }

      val updatePosition = Suppressor()
      fun updatePosition() {
         val cnt = this@ContainerFreeFormUi.content
         if (cnt.width>0.0 && cnt.height>0.0)
            updatePosition.suppressing {
               positionUpdate.suppressed {

                  w.value = if (position.wType==RELATIVE) position.wRel*cnt.width else position.wAbs
                  h.value = if (position.hType==RELATIVE) position.hRel*cnt.height else position.hAbs
                  when (position.alignment) {
                     TOP_LEFT -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else position.minXRel*cnt.width
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else position.minYRel*cnt.height
                     }
                     TOP_CENTER -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else (cnt.width-w.value)/2.0
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else position.minYRel*cnt.height
                     }
                     TOP_RIGHT -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else position.maxXRel*cnt.width-w.value
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else position.minYRel*cnt.height
                     }
                     CENTER_LEFT -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else position.minXRel*cnt.width
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else (cnt.height-h.value)/2.0
                     }
                     CENTER -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else (cnt.width-w.value)/2.0
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else (cnt.height-h.value)/2.0
                     }
                     CENTER_RIGHT -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else position.maxXRel*cnt.width-w.value
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else (cnt.height-h.value)/2.0
                     }
                     BOTTOM_LEFT -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else position.minXRel*cnt.width
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else position.maxYRel*cnt.height-h.value
                     }
                     BOTTOM_CENTER -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else (cnt.width-w.value)/2.0
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else position.maxYRel*cnt.height-h.value
                     }
                     BOTTOM_RIGHT -> {
                        x.value = if (position.wType==RELATIVE) position.minXRel*cnt.width else position.maxXRel*cnt.width-w.value
                        y.value = if (position.hType==RELATIVE) position.minYRel*cnt.height else position.maxYRel*cnt.height-h.value
                     }
                     BASELINE_LEFT, BASELINE_CENTER, BASELINE_RIGHT, null -> {
                        x.value = (position.minXRel*cnt.width)
                        y.value = (position.minYRel*cnt.height)
                     }
                  }
               }
            }
      }


      fun autoLayout() {
         val p = bestRec(centre, this).absolute
         x.value = p.minX
         y.value = p.minY
         w.value = p.width
         h.value = p.height
      }

      fun updateHeader() {
         if (container.showHeaders.value) {
            isHeaderVisible = true
            rightHeaderBox.children += listOf(
               Icon(IconMD.VIEW_DASHBOARD, -1.0, autoLayoutTooltipText, { autoLayout() }).styleclass("header-icon"),
               Icon(IconFA.COGS, -1.0, "Settings\n\n" + "Displays widget properties.").onClickDo { showSettings(it) }.styleclass("header-icon"),
               Icon(ICON_CLOSE, -1.0, "Close this component", { container.removeChild(i) }).styleclass("header-icon")
            )
         } else {
            isHeaderVisible = false
            rightHeaderBox.children.clear()
         }
      }

      fun showSettings(it: Icon) {
         val key = "settingsWindow"
         val settings = object: ConfigurableBase<Any?>(), NameUi {
            override val nameUi = "Window $i"
//            val margin by cvn(position.margin).attach { position.margin = it; updatePosition() }
//               .def("Position margin", "Distance from edge, when alignment is set.")
            val alignment by cvn(position.alignment).attach { position.alignment = it; updatePosition() }
               .def("Alignment", "Affects edge relative to which the position will be calculated.")
            val wType by cv(position.wType).attach { position.wType = it; updatePosition() }
               .def("Width type", "Absolute width means no resizing when container width changes. Relative width means portion of the container width.")
            val hType by cv(position.hType).attach { position.hType = it; updatePosition() }
               .def("Height type", "Absolute width means no resizing when container width changes. Relative width means portion of the container width.")
         }
         it.properties[key]?.asIf<PopWindow>().ifNotNull { it.focus() }.ifNull {
            APP.windowManager.showSettings(settings, it).apply {
               it.properties[key] = this
               onHiding += { it.properties[key] = null }
            }
         }
      }
   }

   data class WindowPosition(
      var margin: Insets? = null,
      var alignment: Pos? = null,
      var minXAbs: Double = 100.0,
      var minXRel: Double = 1/3.0,
      var maxXAbs: Double = 200.0,
      var maxXRel: Double = 1/3.0,
      var minYAbs: Double = 100.0,
      var minYRel: Double = 1/3.0,
      var maxYAbs: Double = 200.0,
      var maxYRel: Double = 2/3.0,
      var wAbs: Double = 100.0,
      var wRel: Double = 1/3.0,
      var wType: WindowPositionType = RELATIVE,
      var hAbs: Double = 100.0,
      var hRel: Double = 1/3.0,
      var hType: WindowPositionType = RELATIVE,
   ) {
      fun setTo(wp: WindowPosition) {
         this::class.memberProperties
            .map { it.asIs<KMutableProperty1<WindowPosition, Any?>>() }
            .forEach { it.set(this, it.get(wp)) }
      }
   }
   enum class WindowPositionType {
      RELATIVE, ABSOLUTE
   }

   companion object {
      private fun Any?.asD() = asIf<Double>()
      const val autoLayoutTooltipText = "Auto-layout\n\nResize components to maximize used space."
      const val layoutButtonTooltipText = "Maximize & align\n\n" +
         "Sets best size and position for the widget. Maximizes widget size " +
         "and tries to align it with other widgets so it does not cover other " +
         "widgets."

      /** Styleclass. Applied on [root] as '.window'.  */
      const val STYLECLASS = "window-resizing"
      /** Pseudoclass active when this window is resized. Applied on [root].  */
      const val PC_RESIZING = "window-resizing"
      /** Pseudoclass active when this window is moved. Applied on [root].  */
      const val PC_MOVING = "window-moving"
   }

}