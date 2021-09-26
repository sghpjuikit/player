package sp.it.pl.layout.controller.io

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.Transition
import javafx.beans.property.DoubleProperty
import javafx.collections.FXCollections.observableSet
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.DragEvent.DRAG_ENTERED
import javafx.scene.input.DragEvent.DRAG_EXITED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.input.TransferMode.COPY
import javafx.scene.input.TransferMode.LINK
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.withSign
import kotlin.properties.Delegates.observable
import sp.it.pl.layout.Component
import sp.it.pl.layout.ContainerSwitchUi
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetSource
import sp.it.pl.layout.controller.io.IOLayer.IOLinkBase.IOLinkConnection.BEZIER
import sp.it.pl.layout.controller.io.IOLayer.IOLinkBase.IOLinkConnection.ELECTRONIC
import sp.it.pl.layout.controller.io.IOLayer.IOLinkBase.IOLinkConnection.LINE
import sp.it.pl.layout.controller.io.IOLayer.IOLinkBase.IOLinkEffect.DOT
import sp.it.pl.layout.controller.io.IOLayer.IOLinkBase.IOLinkEffect.NONE
import sp.it.pl.layout.controller.io.IOLayer.IOLinkBase.IOLinkEffect.PATH
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.contains
import sp.it.pl.main.emScaled
import sp.it.pl.main.get
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.main.set
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.Util.pyth
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.enumConverter
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
import sp.it.util.access.v
import sp.it.util.access.visible
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.mapTo01
import sp.it.util.animation.Loop
import sp.it.util.animation.stopAndFinish
import sp.it.util.animation.then
import sp.it.util.async.executor.EventReducer
import sp.it.util.collections.map.Map2D
import sp.it.util.collections.map.Map2D.Key
import sp.it.util.collections.materialize
import sp.it.util.dev.markUsed
import sp.it.util.dev.failCase
import sp.it.util.functional.Util.forEachCartesianHalfNoSelf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.type.type
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.show
import sp.it.util.ui.size
import sp.it.util.ui.text
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.millis
import sp.it.util.units.uuid

private typealias Compute<T> = java.util.function.Function<Key<Put<*>, Put<*>>, T>

/**
 * Display for [sp.it.pl.layout.controller.io.XPut] of components, displaying their relations as am editable graph.
 */
class IOLayer(private val containerSwitchUi: ContainerSwitchUi): StackPane() {
   private val inputNodes = HashMap<Input<*>, XNode>()
   private val outputNodes = HashMap<Output<*>, XNode>()
   private val inoutputNodes = HashMap<InOutput<*>, InOutputNode>()
   private val links = Map2D<Put<*>, Put<*>, IOLink>()

   private val paneLinks = Pane()
   private val paneNodes = Pane()
   private val paneLabels = Pane()
   private val paneLabelsLayouter = Loop(Runnable {
      val labels = xNodes().mapNotNull { it.label.takeIf { it.isVisible() } }.toSet()
      val labelsSumFxy = labels.associateWith { 0 x 0 }.toMutableMap()
      fun Double.normalizeToClip(t: Double) = this.sign*abs(this).clip(0.0, t)/t

      // pull towards original location
      labels.forEach {
         val dist = pyth(it.x - it.text.layoutX, it.y - it.text.layoutY)
         val dir = atan2(it.y - it.text.layoutY, it.x - it.text.layoutX)
         val f = 4*dist.normalizeToClip(100.0).pow(2)
         labelsSumFxy[it]!!.x += f*cos(dir)
         labelsSumFxy[it]!!.y += f*sin(dir)
      }

      // pull away from each other
      forEachCartesianHalfNoSelf(labels) { l1, l2 ->
         val dist = (l1.text.layoutX + l1.text.layoutBounds.width x l1.text.layoutY + l1.text.layoutBounds.height) - (l2.text.layoutX + l1.text.layoutBounds.width x l2.text.layoutY + l2.text.layoutBounds.height)
         val fDistPadding = 5.emScaled.x2
         val fDist = (l1.text.layoutBounds.size + l2.text.layoutBounds.size)/2.0 + fDistPadding
         val fxy = dist.map(fDist) { a, b -> if (abs(a) - b<0) 4.0*((a - b).normalizeToClip(fDistPadding.x)).pow(2) else 0.0 }
         if (fxy.x!=0.0 && fxy.y!=0.0) {
            labelsSumFxy[l1]!! += fxy
            labelsSumFxy[l2]!! -= fxy
         }
      }

      // update
      labels.forEach {
         it.text.layoutX += labelsSumFxy[it]!!.x
         it.text.layoutY += labelsSumFxy[it]!!.y
         it.byX = it.x - it.text.layoutX
         it.byY = it.y - it.text.layoutY
      }
   })

   private val padding = 20.0
   private val tTranslate: DoubleProperty
   private val tScaleX: DoubleProperty
   private var animPos1 = 0.0
   private var animPos2 = 0.0
   private var animPos3 = 0.0

   private var edit: IOLinkEdit? = null
   private var editFrom: XNode? = null
   private var editTo: XNode? = null
   private var selected: XNode? = null

   private val isDisplaying = visible
   private val disposer = Disposer()

   init {
      interact(doLayout = true, noMouse = false, noPick = false)
      tTranslate = containerSwitchUi.translateProperty()
      tScaleX = containerSwitchUi.zoomProperty()
      tScaleX attach { requestLayout() } on disposer
      translateXProperty().bind(tTranslate.multiply(tScaleX))
      parentProperty().syncNonNullWhile { it.onEventUp(MOUSE_CLICKED) { selectNode(null) } } on disposer

      paneLinks.interact(doLayout = false, noMouse = false, noPick = false)
      paneNodes.interact(doLayout = false, noMouse = false, noPick = false)
      paneLabels.interact(doLayout = false, noMouse = true, noPick = false)
      children += listOf(paneLinks, paneNodes, paneLabels)

      isDisplaying attachTrue { paneLabelsLayouter.start() }
      isDisplaying attachFalse { paneLabelsLayouter.stop() }
      disposer += { paneLabelsLayouter.stop() }

      var aDir = true
      val av = anim(900.millis) {
         isVisible = it!=0.0
         isMouseTransparent = it!=1.0
         animPos1 = if (aDir) it else mapTo01(it, 0.0, 0.2)
         animPos2 = if (aDir) it else mapTo01(it, 0.25, 0.65)
         animPos3 = if (aDir) it else mapTo01(it, 0.8, 1.0)

         paneNodes.opacity = animPos1
         links.forEach { _, _, link ->
            link.showClip1.setScaleXY(animPos2)
            link.showClip2.setScaleXY(animPos2)
         }
         paneLabels.opacity = animPos3
      }.applyAt(0.0)
      val avReducer = EventReducer.toLast<Any>(100.0) {
         if (APP.ui.layoutMode.value)
            av.dur(900.millis).playOpen()
      }
      APP.ui.layoutMode attach {
         aDir = !it
         if (it) avReducer.push(null)
         else av.dur(300.millis).playClose()
      } on disposer

//      allInputs.onItemSync { addInput(it) } on disposer
      allInputs.onItemRemoved { remInput(it) } on disposer
//      allOutputs.onItemSync { addOutput(it) } on disposer
      allOutputs.onItemRemoved { remOutput(it) } on disposer
//      allInoutputs.onItemSync { addInOutput(it) } on disposer
      allInoutputs.onItemRemoved { remInOutput(it) } on disposer

      allLayers += this
   }

   fun dispose() {
      allLayers -= this
      children.clear()
      inputNodes.values.forEach { it.disposer() }
      outputNodes.values.forEach { it.disposer() }
      inoutputNodes.values.forEach { it.disposer() }
      links.values.forEach { it.disposer() }
      disposer()
   }

   private fun addInput(i: Input<*>) {
      inputNodes.computeIfAbsent(i) {
         val n = InputNode(i)
         i.getSources().forEach { o -> links.computeIfAbsent(Key(i, o), Compute { IOLink(i, o) }) }
         n
      }
      requestLayout()
   }

   private fun remInput(i: Input<*>) {
      inputNodes.remove(i)?.let { it.disposer() }
      links.removeIf { it.key1()==i || it.key2()==i }.forEach { it.disposer() }
   }

   private fun addOutput(o: Output<*>) {
      outputNodes.computeIfAbsent(o) { OutputNode(o) }
      requestLayout()
   }

   private fun remOutput(o: Output<*>) {
      outputNodes.remove(o)?.let { it.disposer() }
      links.removeIf { it.key1()==o || it.key2()==o }.forEach { it.disposer() }
   }

   private fun addInOutput(io: InOutput<*>) {
      inoutputNodes.computeIfAbsent(io) {
         val n = InOutputNode(io)
         inputNodes[io.i] = n
         outputNodes[io.o] = n
         io.i.getSources().forEach { o -> links.computeIfAbsent(Key(io.i, o), Compute { IOLink(io.i, o) }) }
         n
      }
      requestLayout()
   }

   private fun remInOutput(io: InOutput<*>) {
      inputNodes -= io.i
      outputNodes -= io.o
      inoutputNodes.remove(io)?.let { it.disposer() }
      links.removeIf { it.key1()==io.i || it.key1()==io.o || it.key2()==io.i || it.key2()==io.o }.forEach { it.disposer() }
   }

   private fun addConnection(i: Put<*>, o: Put<*>) {
      links.computeIfAbsent(Key(i, o)) { IOLink(i, o) }
      requestLayout()
   }

   private fun remConnection(i: Put<*>, o: Put<*>) {
      links.remove2D(i, o)?.let { it.disposer() }
   }

   private fun editBegin(eFrom: XNode?) {
      if (eFrom==null) return
      selectNode(null)

      val e = IOLinkEdit(eFrom)
      edit = e
      editFrom = eFrom

      // start effect: disable & visually differentiate bindable & unbindable nodes
      e.link.pseudoClassChanged("highlighted", true)
      eFrom.i.pseudoClassChanged("highlighted", true)
      outputNodes.forEach { (_, node) -> node.onEditActive(true, node.output===eFrom.output) }
      inputNodes.forEach { (_, node) -> node.onEditActive(true, node.input!!.isAssignable(eFrom.output!!)) }
      inoutputNodes.forEach { (_, node) -> node.onEditActive(true, node.output===eFrom.output || node.input!!.isAssignable(eFrom.output!!)) }
      links.forEach { _, link -> link.onEditActive(true) }
   }

   private fun editMove(m: MouseEvent) {
      val e = edit ?: return
      val eFrom = editFrom ?: return

      e.link.lay(eFrom.x, eFrom.y, m.x, m.y)

      val n = inputNodes.values.asSequence()
         .filter { pyth(it.x - m.x, it.y - m.y)<8 }
         .find { it.input!!.isAssignable(eFrom.output!!) }

      if (editTo!==n) {
         editTo?.i?.pseudoClassChanged("highlighted", false)
         editTo = n
         editTo?.i?.pseudoClassChanged("highlighted", true)
      }
   }

   private fun editEnd() {
      val e = edit ?: return
      val eFrom = editFrom!!
      val eTo = editTo
      edit = null
      editFrom = null
      editTo = null

      if (eTo!=null) {
         if (e.isValueOnly.value) {
            eTo.input!!.valueAny = eFrom.output!!.value
            dataArrived(eTo.x, eTo.y)
         } else {
            eTo.input!!.bindAny(eFrom.output!!)
            links.get(eTo.input, eFrom.output).dataSend()
         }
      }

      eTo?.select(false)
      e.link.disposer()

      // stop effect: disable & visually differentiate bindable nodes
      eFrom.i.pseudoClassChanged("highlighted", false)
      eTo?.i?.pseudoClassChanged("highlighted", false)
      outputNodes.forEach { (_, node) -> node.onEditActive(false, true) }
      inputNodes.forEach { (_, node) -> node.onEditActive(false, true) }
      links.forEach { _, link -> link.onEditActive(false) }
   }

   private fun selectNode(n: XNode?) {
      selected?.select(false)
      selected = n
      selected?.select(true)
   }

   private fun xNodes(): Sequence<XNode> = (inputNodes.asSequence() + outputNodes.asSequence() + inoutputNodes.asSequence()).map { it.value }

   override fun layoutChildren() {
      val headerOffset = containerSwitchUi.root.localToScene(0.0, 0.0).y
      val translationOffset = tTranslate.value

      xNodes().forEach { it.graphics.isVisible = false }

      containerSwitchUi.container.rootParent?.getAllWidgets().orEmpty().filter { it.controller!=null }.forEach { w ->
         val c = w.controller!!
         val ins = c.io.i.getInputs().mapNotNull { inputNodes[it] }
         val ons = c.io.o.getOutputs().mapNotNull { outputNodes[it] }

         val b = w.ui!!.root.let { it.localToScene(it.layoutBounds) }
         val baseX = b.minX/tScaleX.value.toDouble() - translationOffset
         val baseY = b.minY - headerOffset
         val ww = b.width/tScaleX.value.toDouble()
         val wh = b.height
         val ihx = wh/(ins.size + 1)
         val ohx = wh/(ons.size + 1)

         ins.forEachIndexed { i, n ->
            n.graphics.isVisible = true
            n.graphics.autosize()
            n.updatePosition(calcScaleX(baseX + padding), calcScaleY(baseY + ihx*(i + 1)))
         }
         ons.forEachIndexed { i, n ->
            n.graphics.isVisible = true
            n.graphics.autosize()
            n.updatePosition(calcScaleX(baseX + ww - padding), calcScaleY(baseY + ohx*(i + 1)))
         }
      }

      drawGraph()

      paneLabels.resizeRelocate(0.0, 0.0, width, height)
   }

   fun drawGraph() {
      links.forEach { input, output, link ->
         val i: XNode? = inputNodes[input] ?: outputNodes[input]
         val o: XNode? = inputNodes[output] ?: outputNodes[output]
         if (i!=null && o!=null) {
            link.isVisible = i.graphics.isVisible && o.graphics.isVisible
            if (link.isVisible) {
               when {
                  input is Input<*> && output is Input<*> -> link.layInputs(i.x, i.y, o.x, o.y)
                  input is Output<*> && output is Output<*> -> link.layOutputs(i.x, i.y, o.x, o.y)
                  else -> link.lay(i.x, i.y, o.x, o.y)
               }
            }
         }
      }
   }

   private fun calcScaleX(x: Double): Double = x*tScaleX.value.toDouble()

   private fun calcScaleY(y: Double): Double = y

   private abstract inner class XNode(xPut: XPut<*>, iconStyleclass: String) {
      val input: Input<*>?
      val output: Output<*>?
      val inoutput: InOutput<*>?
      val i = Icon()
      val graphics = HBox(0.0, i)
      val label = XLabel(iconStyleclass)
      var x by observable(0.0) { _, _, nv -> label.y = nv + 15 }
      var y by observable(0.0) { _, _, nv -> label.x = nv + 15 }
      var selected = false
      val disposer = Disposer()

      init {
         when (xPut) {
            is Input<*> -> {
               input = xPut
               output = null
               inoutput = null
            }
            is Output<*> -> {
               input = null
               output = xPut
               inoutput = null
            }
            is InOutput<*> -> {
               input = xPut.i
               output = xPut.o
               inoutput = xPut
            }
            else -> failCase(xPut)
         }

         paneNodes.children += graphics
         disposer += { paneNodes.children -= graphics }
         graphics.visibleProperty() sync { if (it) paneLabels.children += label.text else paneLabels.children -= label.text } on disposer
         disposer += { paneLabels.children -= label.text }

         i.styleclass(iconStyleclass)
         i.pseudoClassStates.onItemSyncWhile {
            if (it.pseudoClassName in propagatedPseudoClasses) {
               label.text.pseudoClassChanged("node-" + it.pseudoClassName, true)
               Subscription {
                  label.text.pseudoClassChanged("node-" + it.pseudoClassName, false)
               }
            } else {
               Subscription()
            }
         }
         i.onEventDown(MOUSE_CLICKED) {
            when (it.clickCount) {
               1 -> {
                  when (it.button) {
                     PRIMARY -> selectNode(this)
                     SECONDARY -> {
                        contextMenuInstance.setItemsFor(xPut)
                        contextMenuInstance.show(i, it)
                     }
                     else -> Unit
                  }
               }
               2 -> {
                  output?.value?.let {
                     APP.ui.actionPane.orBuild.show(it)
                  }
               }
            }
            it.consume()
         }

         val valuePut = if (xPut is Input<*>) input else output
         valuePut!!.sync { label.text.text = valuePut.xPutToStr() } on disposer
         valuePut.attach { if (isDisplaying.value) dataArrived(x, y) } on disposer
      }

      fun select(v: Boolean) {
         if (selected==v) return
         selected = v
         i.pseudoClassChanged("selected", v)
      }

      fun onEditActive(active: Boolean, canAccept: Boolean) {
         graphics.isDisable = active && !canAccept
      }

      fun updatePosition(toX: Double, toY: Double) {
         x = toX
         y = toY
         graphics.relocate(x - graphics.layoutBounds.width/2.0, y - graphics.layoutBounds.height/2.0)
         label.updatePosition()
      }

      protected fun installDragFrom() {
         i.onEventUp(DRAG_DETECTED, PRIMARY) {
            if (selected) i.startDragAndDrop(if (it.isShortcutDown) LINK else COPY)[Df.WIDGET_OUTPUT] = output!!
            else editBegin(this)
         }
      }

      protected fun installDragTo() {
         i.onEventUp(DRAG_ENTERED) { i.pseudoClassChanged("drag-over", true) }
         i.onEventUp(DRAG_EXITED) { i.pseudoClassChanged("drag-over", false) }
         i.installDrag(
            IconFA.CLIPBOARD, "",
            { if (it.transferMode==LINK) Df.WIDGET_OUTPUT in it.dragboard else true },
            {
               if (Df.WIDGET_OUTPUT in it.dragboard) {
                  val o = it.dragboard[Df.WIDGET_OUTPUT]
                  if (it.transferMode==LINK) {
                     if (input!!.isAssignable(o))
                        input.bindAny(o)
                  } else {
                     if (input!!.isAssignable(o.value))
                        input.valueAny = o.value
                  }
               } else {
                  val o = it.dragboard.getAny()
                  if (input!!.isAssignable(o))
                     input.valueAny = o
               }
            }
         )
      }

      inner class XLabel(iconStyleclass: String) {
         var x = 0.0
         var y = 0.0
         var byX = 0.0
         var byY = 0.0
         var text = text {
            styleClass += "$iconStyleclass-text"
            translateX = 20.0
            translateY = 20.0
         }

         fun isVisible() = text.parent!=null

         fun updatePosition() {
            x = this@XNode.x
            y = this@XNode.y
            text.layoutX = this@XNode.x - byX
            text.layoutY = this@XNode.y - byY
         }

      }

   }

   private inner class InputNode(xPut: Input<*>): XNode(xPut, "inode") {
      init {
         installDragTo()
      }
   }

   private inner class OutputNode(xPut: Output<*>): XNode(xPut, "onode") {
      init {
         installDragFrom()
      }
   }

   private inner class InOutputNode(xPut: InOutput<*>): XNode(xPut, "ionode") {
      init {
         installDragFrom()
         installDragTo()
      }
   }

   abstract class IOLinkBase: Path() {
      val dataConnection by sv(DATA_CONNECTION)
      val dataEffect by sv(DATA_EFFECT)

      override fun getCssMetaData() = classCssMetaData

      companion object: StyleableCompanion() {
         val DATA_CONNECTION by svMetaData<IOLinkBase, IOLinkConnection>("-fx-data-connection", enumConverter(), ELECTRONIC, IOLinkBase::dataConnection)
         val DATA_EFFECT by svMetaData<IOLinkBase, IOLinkEffect>("-fx-data-effect", enumConverter(), PATH, IOLinkBase::dataEffect)
      }

      enum class IOLinkConnection { LINE, BEZIER, ELECTRONIC }
      enum class IOLinkEffect { NONE, DOT, PATH }
   }

   private inner class IOLink(val input: Put<*>?, val output: Put<*>?): IOLinkBase() {
      var startX = 0.0
      var startY = 0.0
      var toX = 0.0
      var toY = 0.0
      var length = 0.0
      private val effect = Path()
      private val effectClip = Pane()
      val showClip1 = Circle()
      val showClip2 = Circle()
      private val showClip = Pane(showClip1, showClip2)
      private val linkGap = 20.0
      private val linkEndOffset = 8.0
      val disposer = Disposer()
      private val disposerAnimations = mutableListOf<Transition>()

      init {
         styleClass += "iolink"
         isMouseTransparent = false
         isPickOnBounds = false
         showClip1.setScaleXY(animPos2)
         showClip2.setScaleXY(animPos2)
         clip = showClip
         paneLinks.children += this
         disposer += { paneLinks.children -= this }

         effect.styleClass += "iolink-effect-link"
         effect.isMouseTransparent = true
         effect.clip = effectClip
         duplicateTo(effect)
         paneLinks.children += effect
         disposer += { paneLinks.children -= effect }
         dataConnection attach { this@IOLayer.requestLayout() }
         dataEffect attach { this@IOLayer.requestLayout() }

         disposer += { disposerAnimations.materialize().forEach { it.stopAndFinish() } }

         if (input!=null && output!=null) {
            output.attach { if (isDisplaying.value && edit?.link!=this) dataSend() } on disposer
         }
         hoverProperty() attach {
            val n1 = inputNodes[input] ?: outputNodes[input]
            val n2 = inputNodes[output] ?: outputNodes[output]
            n1?.i?.pseudoClassChanged("highlighted", it)
            n2?.i?.pseudoClassChanged("highlighted", it)
         }
         onEventDown(MOUSE_CLICKED, SECONDARY) {
            if (input is Input<*> && output is Output<*>)
               input.unbind(output)
         }
         onEventDown(DRAG_DETECTED, PRIMARY) {
            editBegin(outputNodes[output])
         }
      }


      fun onEditActive(active: Boolean) {
         isDisable = active
      }

      fun layInputs(inX: Double, inY: Double, outX: Double, outY: Double) {
         layInit(inX, inY, outX, outY)
         when (dataConnection.value!!) {
            ELECTRONIC -> {
               elements += MoveTo(inX + loX(1.0, 1.0), inY - loY(1.0, 1.0))
               elements += LineTo(inX + linkGap, inY - linkGap)
               elements += LineTo(outX + linkGap, outY + linkGap)
               elements += LineTo(outX + loX(1.0, 1.0), outY + loY(1.0, 1.0))
            }
            BEZIER, LINE -> {
               val dx = (outX - inX).sign
               val dy = (outY - inY).sign
               elements += MoveTo(inX + loX(dx, dy), inY + loY(dx, dy))
               elements += LineTo(outX - loX(dx, dy), outY - loY(dx, dy))
            }
         }
      }

      fun layOutputs(inX: Double, inY: Double, outX: Double, outY: Double) {
         layInit(inX, inY, outX, outY)
         when (dataConnection.value!!) {
            ELECTRONIC -> {
               elements += MoveTo(inX - loX(1.0, 1.0), inY + loY(1.0, 1.0))
               elements += LineTo(inX - linkGap, inY + linkGap)
               elements += LineTo(outX - linkGap, outY - linkGap)
               elements += LineTo(outX - loX(1.0, 1.0), outY - loY(1.0, 1.0))
            }
            BEZIER, LINE -> {
               val dx = (outX - inX).sign
               val dy = (outY - inY).sign
               elements += MoveTo(inX + loX(dx, dy), inY + loY(dx, dy))
               elements += LineTo(outX - loX(dx, dy), outY - loY(dx, dy))
            }
         }
      }

      @Suppress("LocalVariableName", "CanBeVal")
      fun lay(inX: Double, inY: Double, outX: Double, outY: Double) {
         layInit(inX, inY, outX, outY)
         val dx = (outX - inX).sign
         val dy = (outY - inY).sign

         when (dataConnection.value!!) {
            ELECTRONIC -> {
               val inX_ = inX + linkGap*dx
               val inY_ = inY + linkGap*dy
               elements += MoveTo(inX + loX(dx, dy), inY + loY(dx, dy))
               elements += LineTo(inX_, inY_)
               layTo(inX_, inY_, outX - linkGap*dx, outY - linkGap*dy)
               elements += LineTo(outX - loX(dx, dy), outY - loY(dx, dy))
            }
            BEZIER -> {
               val cx = dx*(1.0 max abs(outX-inX))*(abs(outY-inY).net { (it/500.0) min 1.0 })
               elements += MoveTo(inX + loX(dx, 0.0), inY + loY(dx, 0.0))
               elements += CubicCurveTo(inX + cx, inY, outX - cx, outY, outX - loX(dx, 0.0), outY - loY(dx, 0.0))
            }
            LINE -> {
               elements += MoveTo(inX + loX(dx, dy), inY + loY(dx, dy))
               elements += LineTo(outX - loX(dx, dy), outY - loY(dx, dy))
            }
         }
      }

      fun layInit(inX: Double, inY: Double, outX: Double, outY: Double) {
         startX = outX
         startY = outY
         toX = inX
         toY = inY
         length = pyth(inX - outX, inY - outY)
         showClip1.radius = length
         showClip1.centerX = inX
         showClip1.centerY = inY
         showClip2.radius = length
         showClip2.centerX = outX
         showClip2.centerY = outY
         elements.clear()
      }

      private fun layTo(inX: Double, inY: Double, outX: Double, outY: Double) {
         val dx = outX - inX
         val dy = outY - inY
         if (dx==0.0 || dy==0.0) {
            elements += LineTo(outX, outY)
         } else {
            val dXy = abs(dx) min abs(dy)
            val x = inX + dXy.withSign(dx)
            val y = inY + dXy.withSign(dy)
            elements += LineTo(x, y)
            layTo(x, y, outX, outY)
         }
      }

      private fun loX(x: Double, y: Double) = linkEndOffset*cos(atan2(y, x))

      private fun loY(x: Double, y: Double) = linkEndOffset*sin(atan2(y, x))

      fun dataSend() {
         val lengthNormalized = 30.0 max (length/4.0)
         val middleDur = 1000.0
         val speed = length/middleDur
         val edgeDur = lengthNormalized/2.0/speed

         when (dataEffect.value!!) {
            NONE -> Unit
            PATH -> {
               val eRunner = Circle(lengthNormalized/2.0).apply {
                  effectClip.children += this
                  setScaleXY(0.0)
                  centerX = startX
                  centerY = startY
               }
               val ea1 = anim(edgeDur.millis) { eRunner.setScaleXY(it) }.apply {
                  interpolator = Interpolator.LINEAR
                  delay = 0.millis
                  disposerAnimations += this
                  then {
                     disposerAnimations -= this
                  }
               }
               val ea2 = PathTransition(middleDur.millis, this, eRunner).apply {
                  rate = -1.0
                  delay = edgeDur.millis
                  interpolator = Interpolator.LINEAR
                  disposerAnimations += this
                  then {
                     disposerAnimations -= this
                  }
               }
               val ea3 = anim(edgeDur.millis) { eRunner.setScaleXY(1 - it) }.apply {
                  delay = (edgeDur+middleDur).millis
                  disposerAnimations += this
                  interpolator = Interpolator.LINEAR
                  then {
                     disposerAnimations -= this
                     effectClip.children -= eRunner
                  }
               }

               ea1.play()
               ea2.play()
               ea3.play()
            }
            DOT -> {
               val pRunner = Circle(3.0).apply {
                  styleClass += "iolink-effect-dot"
                  this@IOLayer.children += this
               }
               val a1 = PathTransition(1000.millis, this, pRunner).apply {
                  rate = -1.0
                  disposerAnimations += this
                  interpolator = Interpolator.LINEAR
                  then {
                     this@IOLayer.children -= pRunner
                     disposerAnimations -= this
                  }
               }
               a1.playFrom(a1.duration)
            }
         }
      }
   }

   private inner class IOLinkEdit(node: XNode) {
      val link = IOLink(node.input, node.output)
      val isValueOnly = v(false)

      init {
         link.styleClass += "iolink-edit"
         isValueOnly attach { link.pseudoClassChanged("value-only", it) } on disposer

         val editDrawer = EventHandler<MouseEvent> {
            isValueOnly.value = it.isShiftDown
            layToMouse(it)
         }
         val editCanceler = object: EventHandler<MouseEvent> {
            override fun handle(e: MouseEvent) {
               editEnd()
               isValueOnly.value = false
               this@IOLayer.removeEventFilter(MOUSE_CLICKED, this)
               this@IOLayer.removeEventFilter(MouseEvent.ANY, editDrawer)
            }
         }
         this@IOLayer.addEventFilter(MouseEvent.ANY, editDrawer)
         this@IOLayer.addEventFilter(MOUSE_RELEASED, editCanceler)
      }

      fun layToMouse(e: MouseEvent) = editMove(e)
   }

   companion object {
      val allLayers = observableSet<IOLayer>()!!
      val allLinks = Map2D<Put<*>, Put<*>, Any>()
      val allInputs = observableSet<Input<*>>()!!
      val allOutputs = observableSet<Output<*>>()!!
      val allInoutputs = observableSet<InOutput<*>>()!!
      val allInputsApp = observableSet<Input<*>>()!!
      val allOutputsApp = observableSet<Output<*>>()!!
      val allInoutputsApp = observableSet<InOutput<*>>()!!
      val generatingOutputRefs = observableSet<GeneratingOutputRef<*>>()!!
      private val contextMenuInstance by lazy { ValueContextMenu<XPut<*>>() }
      private val propagatedPseudoClasses = setOf("hover", "highlighted", "selected", "drag-over")

      fun allInputs(): Sequence<Input<*>> = allInputs.asSequence() + allInputsApp.asSequence() + allInoutputs().map { it.i }
      fun allOutputs(): Sequence<Output<*>> = allOutputs.asSequence() + allOutputsApp.asSequence() + allInoutputs().map { it.o }
      fun allInoutputs(): Sequence<InOutput<*>> = allInoutputs.asSequence() + allInoutputsApp.asSequence()
      fun allInputRefs(): Sequence<InputRef> = (allInputs.asSequence() + allInputsApp.asSequence()).map { it.toRef() } + allInoutputs().map { it.toInputRef() }
      fun allOutputRefs(): Sequence<OutputRef> = (allOutputs.asSequence() + allOutputsApp.asSequence()).map { it.toRef() } + allInoutputs().map { it.toOutputRef() }

      fun componentPutAdded(id: UUID, put: Put<*>) {
         APP.widgetManager.widgets.findAll(WidgetSource.OPEN).find { it.id == id }.ifNotNull { w ->
            val s = w.window?.scene ?: return
            val l = allLayers.find { it.scene === s } ?: return
            when (put) {
               is Input<*> -> {
                  allInputs += put
                  l.addInput(put)
               }
               is Output<*> -> {
                  allOutputs += put
                  l.addOutput(put)
               }
               is InOutput<*> -> {
                  allInoutputs += put
                  l.addInOutput(put)
               }
            }
         }
      }
      fun componentPutRemoved(id: UUID, put: Put<*>) {
         id.markUsed()
         when (put) {
            is Input<*> -> allInputs -= put
            is Output<*> -> allOutputs -= put
            is InOutput<*> -> allInoutputs -= put
         }
      }
      fun componentSceneChanged(c: Widget) {
         allInputs -= c.controller?.io?.i?.getInputs().orEmpty()
         allOutputs -= c.controller?.io?.o?.getOutputs().orEmpty()
         allInoutputs -= c.controller?.io?.io?.getInOutputs().orEmpty()

         val s = c.window?.scene ?: return
         val l = allLayers.find { it.scene === s } ?: return

         allInputs += c.controller?.io?.i?.getInputs().orEmpty()
                      c.controller?.io?.i?.getInputs().orEmpty().forEach { l.addInput(it) }
         allOutputs += c.controller?.io?.o?.getOutputs().orEmpty()
                       c.controller?.io?.o?.getOutputs().orEmpty().forEach { l.addOutput(it) }
         allInoutputs += c.controller?.io?.io?.getInOutputs().orEmpty()
                         // c.controller?.io?.io?.getInOutputs().orEmpty().forEach { l.addInOutput(it) } // undesirable
      }

      fun requestLayoutFor(c: Component) {
         val s = c.window?.scene ?: return
         val l = allLayers.find { it.scene === s } ?: return
         l.requestLayout()
      }

      private val currentTime = GeneratingOutputRef<LocalTime>(uuid("c86ed924-e2df-43be-99ba-4564ddc2660a"), "Current Time", type()) { ref ->
         object: GeneratingOutput<LocalTime>(ref, LocalTime.now()) {
            val generation = Loop(Runnable { i.value = LocalTime.now().net { LocalTime.of(it.hour, it.minute, it.second) } })
            init { appWide() }
            init { generation.start() }
            override fun dispose() = generation.stop()
         }
      }.appWide()

      private val currentDate = GeneratingOutputRef<LocalDate>(uuid("c86ed924-e2df-43be-99ba-4564ddc2660a"), "Current Date", type()) { ref ->
         object: GeneratingOutput<LocalDate>(ref, LocalDate.now()) {
            val generation = Loop(Runnable { i.value = LocalDate.now() })
            init { appWide() }
            init { generation.start() }
            override fun dispose() = generation.stop()
         }
      }.appWide()



      fun addLinkForAll(i: Put<*>, o: Put<*>) {
         allLinks.put(i, o, Any())
         allLayers.forEach { it.addConnection(i, o) }
      }

      fun remLinkForAll(i: Put<*>, o: Put<*>) {
         allLinks.remove2D(i, o)
         allLayers.forEach { it.remConnection(i, o) }

         allInoutputs().asSequence().filterIsInstance<GeneratingOutput<*>>().find { it.o === o }.ifNotNull {
            if (!it.o.isBound()) {
               allInoutputs -= it
               allInoutputsApp -= it
               it.dispose()
            }
         }
      }

      private fun <T> Put<T>.xPutToStr(): String = "$name : ${type.toUi()}\n${APP.instanceName[value]}"

      private fun Node.interact(doLayout: Boolean, noMouse: Boolean, noPick: Boolean) {
         isManaged = doLayout
         isMouseTransparent = noMouse
         isPickOnBounds = noPick
      }

      private fun Path.duplicateTo(path: Path) {
         elements.onItemAdded { path.elements += it }
         elements.onItemRemoved { path.elements.clear() }
      }

      private fun IOLayer.dataArrived(x: Double, y: Double) {
         children += Circle(5.0).apply {
            styleClass += "iolink-effect-receive"
            isManaged = false
            relocate(x - radius, y - radius)

            anim(250.millis) {
               setScaleXY(4*sqrt(it))
               opacity = 1 - it*it
            }.then {
               children -= this
            }.play()
         }
      }
   }

}

fun Input<*>.boundOutputs(): Sequence<Output<*>> = getSources().asSequence()

fun Output<*>.boundInputs(): Sequence<Input<*>> = IOLayer.allLinks.keys.asSequence()
   .mapNotNull { if (it.key1()==value) it.key2() else if (it.key2()==value) it.key1() else null }
   .filterIsInstance<Input<*>>().distinct()

fun <T> Input<T>.appWide() = apply { IOLayer.allInputsApp += this }

fun <T> Output<T>.appWide() = apply { IOLayer.allOutputsApp += this }

fun <T> InOutput<T>.appWide() = apply { IOLayer.allInoutputsApp += this }

fun <T> GeneratingOutputRef<T>.appWide() = apply { IOLayer.generatingOutputRefs += this }