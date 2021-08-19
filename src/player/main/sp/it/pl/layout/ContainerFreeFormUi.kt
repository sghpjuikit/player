package sp.it.pl.layout

import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.AnchorPane
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
import sp.it.util.access.toggle
import sp.it.util.access.vAlways
import sp.it.util.async.runFX
import sp.it.util.functional.Util
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runnable
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.initClipToPadding
import sp.it.util.ui.layFullArea
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.x
import sp.it.util.units.millis

class ContainerFreeFormUi(c: ContainerFreeForm): ContainerUi<ContainerFreeForm>(c) {
   private val windows: MutableMap<Int, Window> = HashMap()
   private var isResizing = false
   private val isAnyWindowResizing = mutableListOf<Int>()
   private val content = AnchorPane()

   init {
      root.minSize = 0 x 0
      root.maxSize = Double.MAX_VALUE x Double.MAX_VALUE
      root.layFullArea += content


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
         { bestRec(it.x, it.y, null).absolute } // alternatively: e -> bestRec(e.getX(),e.getY(),DragUtilKt.get(e, Df.COMPONENT).getWindow())).absolute
      )

      content.widthProperty() attach {
         isResizing = true
         windows.forEach { (i, w) ->
            w.snappable.value = false
            val (wx, ww) = container.properties.net { it["${i}x"].asD() to it["${i}w"].asD() }
            if (wx!=null) w.x.value = wx*it.toDouble()
            if (wx!=null && ww!=null) w.w.value = (ww - wx)*it.toDouble()
            w.snappable.value = APP.ui.snapping.value
         }
         isResizing = false
      }
      content.heightProperty() attach {
         isResizing = true
         windows.forEach { (i, w) ->
            w.snappable.value = false
            val (wy, wh) = container.properties.net { it["${i}y"].asD() to it["${i}h"].asD() }
            if (wy!=null) w.y.value = wy*it.toDouble()
            if (wy!=null && wh!=null) w.h.value = (wh - wy)*it.toDouble()
            w.snappable.value = APP.ui.snapping.value
         }
         isResizing = false
      }
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
            c.ui?.asIf<ContainerUi<*>>()?.let { it.controls.ifSet { it.updateIcons() } }
         }
         is Widget -> {
            w.ui = null
               ?: run { if (w.ui is WidgetUi && (w.ui as WidgetUi?)!!.widget==c) w.ui else null }
                  ?: run {
                  w.ui?.dispose()
                  WidgetUi(container, i, c).apply {
                     val lb = Icon(IconMD.VIEW_DASHBOARD, 12.0, layoutButtonTooltipText, { w.autoLayout() })
                     controls.icons.children.add(1, lb)
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
      windows.remove(i).ifNotNull { w ->
         w.close()
         val c = container.children[i]
         if (c==null || "reloading=${c.id}" !in container.properties) {
            container.properties -= "${i}x"
            container.properties -= "${i}y"
            container.properties -= "${i}w"
            container.properties -= "${i}h"
         }
      }
   }

   private fun getOrBuildWindow(i: Int, c: Component?): Window {
      return windows.getOrPut(i) {
         val w = Window(i, content)

         w.open()
         w.resizeHalf()
         w.alignCenter()
         w.snappable.value = false
         w.root.onEventDown(MOUSE_CLICKED, PRIMARY) {}

         val (wx, ww) = container.properties.net { it["${i}x"].asD() to it["${i}w"].asD() }
         val (wy, wh) = container.properties.net { it["${i}y"].asD() to it["${i}h"].asD() }
         if (wx!=null) w.x.value = wx*content.width
         if (wy!=null) w.y.value = wy*content.height
         if (wx!=null && ww!=null) w.w.value = ww*content.width - wx*content.width
         if (wy!=null && wh!=null) w.h.value = wh*content.height - wy*content.height

         w.x attach { if (!isResizing) container.properties["${i}x"] = it.toDouble()/content.width }
         w.x attach { if (!isResizing) container.properties["${i}w"] = (it.toDouble() + w.w.value)/content.width }
         w.y attach { if (!isResizing) container.properties["${i}y"] = it.toDouble()/content.height }
         w.y attach { if (!isResizing) container.properties["${i}h"] = (it.toDouble() + w.h.value)/content.height }
         w.w attach { if (!isResizing) container.properties["${i}w"] = (w.x.value + it.toDouble())/content.width }
         w.h attach { if (!isResizing) container.properties["${i}h"] = (w.y.value + it.toDouble())/content.height }
         w.snapDistance syncFrom APP.ui.snapDistance on w.disposer
         w.snappable syncFrom APP.ui.snapping on w.disposer
         container.lockedUnder sync { w.resizable.value = !it } on w.disposer
         container.lockedUnder sync { w.movable.value = !it } on w.disposer
         container.showHeaders sync { w.updateHeader() } on w.disposer
         w.titleL.textProperty() syncFrom (if (c is Widget) c.customName else vAlways("")) on w.disposer
         w.resizing attach { if (it==Resize.NONE) runFX(100.millis) { isAnyWindowResizing -= i } else isAnyWindowResizing += i }

         // prevent layouter closing when resize/move
         w.resizing sync { suppressHidingFor(w.borders, it!=Resize.NONE) }
         w.moving sync { suppressHidingFor(w.borders, it) }

         // report component graphics changes
         w.root.parentProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }
         w.root.boundsInParentProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }

         w
      }
   }

   /** Optimal size/position strategy returning greatest empty square.  */
   private fun bestRec(x: Double, y: Double, newW: Window?): BestRec {
      val b = TupleM4(0.0, content.width, 0.0, content.height)
      for (w in windows.values) {
         if (w===newW) continue  // ignore self
         val wl = w.x.get() + w.w.get()
         if (wl<x && wl>b.a) b.a = wl
         val wr = w.x.get()
         if (wr>x && wr<b.b) b.b = wr
         val ht = w.y.get() + w.h.get()
         if (ht<y && ht>b.c) b.c = ht
         val hb = w.y.get()
         if (hb>y && hb<b.d) b.d = hb
      }
      b.a = 0.0
      b.b = content.width
      for (w in windows.values) {
         if (w===newW) continue  // ignore self
         val wl = w.x.get() + w.w.get()
         val wr = w.x.get()
         val ht = w.y.get() + w.h.get()
         val hb = w.y.get()
         val inTheWay = !(ht<y && ht<=b.c || hb>y && hb>=b.d)
         if (inTheWay) {
            if (wl<x && wl>b.a) b.a = wl
            if (wr>x && wr<b.b) b.b = wr
         }
      }
      return BestRec(
         TupleM4(b.a/content.width, b.c/content.height, (b.b - b.a)/content.width, (b.d - b.c)/content.height),
         BoundingBox(b.a, b.c, b.b - b.a, b.d - b.c)
      )
   }

   /** Initializes position & size for i-th window, ignoring self w if passed as param.  */
   private fun storeBestRec(i: Int, x: Double, y: Double) {
      val bestPos = bestRec(x, y, null).relative
      // add empty window at index
      // the method call eventually invokes load() method below, with
      // component/child == null (3rd case)
      container.properties["${i}x"] = bestPos.a
      container.properties["${i}y"] = bestPos.b
      container.properties["${i}w"] = bestPos.c + bestPos.a
      container.properties["${i}h"] = bestPos.d + bestPos.b
   }

   private fun addEmptyWindowAt(x: Double, y: Double): Int {
      val i = Util.findFirstEmptyKey(container.children, 1)
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
         root.styleClass += "free-form-container-window"
         offScreenFixOn.value = false
      }

      override fun close() {
         disposer()
         super.close()
      }

      fun autoLayout() {
         val p = bestRec(w.value + w.value/2, y.value + h.value/2, this).absolute
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
               Icon(ICON_CLOSE, -1.0, "Close this component", { container.removeChild(i) }).styleclass("header-icon")
            )
         } else {
            isHeaderVisible = false
            rightHeaderBox.children.clear()
         }
      }
   }

   companion object {
      private fun Any?.asD() = asIf<Double>()
      const val autoLayoutTooltipText = "Auto-layout\n\nResize components to maximize used space."
      const val layoutButtonTooltipText = "Maximize & align\n\n" +
         "Sets best size and position for the widget. Maximizes widget size " +
         "and tries to align it with other widgets so it does not cover other " +
         "widgets."
   }

}