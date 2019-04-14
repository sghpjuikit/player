package sp.it.pl.layout.area

import javafx.animation.PathTransition
import javafx.beans.property.DoubleProperty
import javafx.collections.FXCollections.observableSet
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.input.DragEvent.DRAG_ENTERED
import javafx.scene.input.DragEvent.DRAG_EXITED
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import sp.it.pl.gui.objects.Text
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.container.switchcontainer.SwitchPane
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.controller.io.Put
import sp.it.pl.layout.widget.controller.io.XPut
import sp.it.pl.main.*
import sp.it.util.Util.pyth
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.mapTo01
import sp.it.util.async.executor.EventReducer
import sp.it.util.collections.map.Map2D
import sp.it.util.collections.map.Map2D.Key
import sp.it.util.dev.failCase
import sp.it.util.functional.Util.min
import sp.it.util.functional.asIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.onItemSync
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.text.plural
import sp.it.util.type.Util.getRawType
import sp.it.util.type.isSuperclassOf
import sp.it.util.ui.maxSize
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.x
import sp.it.util.units.millis
import java.lang.Math.abs
import java.lang.Math.max
import java.lang.Math.random
import java.lang.Math.signum
import java.lang.Math.sqrt
import java.lang.reflect.ParameterizedType
import java.util.ArrayList
import java.util.HashMap
import java.util.stream.Stream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

private typealias Compute<T> = java.util.function.Function<Key<Put<*>, Put<*>>, T>

/**
 * Display for [sp.it.pl.layout.widget.controller.io.XPut] of components, displaying their relations as am editable graph.
 */
class IOLayer(private val switchpane: SwitchPane): StackPane() {
    private val connections = Map2D<Put<*>, Put<*>, IOLine>()
    private val inputnodes: MutableMap<Input<*>, XNode<*, *>> = HashMap()
    private val outputnodes: MutableMap<Output<*>, XNode<*, *>> = HashMap()
    private val inoutputnodes: MutableMap<InOutput<*>, InOutputNode> = HashMap()

    private val padding = 15.0
    private val tTranslate: DoubleProperty
    private val tScaleX: DoubleProperty
    private val tScaleY: DoubleProperty
    private var anim1Opacity = 0.0
    private var anim2Opacity = 0.0
    private var anim3Opacity = 0.0

    private var edit: EditIOLine? = null
    private var selected: XNode<*, *>? = null
    private val disposer = Disposer()

    private var editFrom: XNode<*, *>? = null
    private var editTo: XNode<*, *>? = null

    fun addController(c: Controller) {
        c.io.i.getInputs().forEach { addInput(it) }
        c.io.o.getOutputs().forEach { addOutput(it) }
    }

    fun remController(c: Controller) {
        c.io.i.getInputs().forEach { remInput(it) }
        c.io.o.getOutputs().forEach { remOutput(it) }
    }

    private fun addInput(i: Input<*>) {
        allInputs += i
        inputnodes.computeIfAbsent(i) {
            val iNode = InputNode(i)
            i.getSources().forEach { o -> connections.computeIfAbsent(Key(i, o), Compute { IOLine(i, o) }) }
            children += iNode.graphics
            iNode
        }
    }

    private fun remInput(i: Input<*>) {
        allInputs -= i
        i.getSources().forEach { o -> removeChild(connections.remove2D(Key(i, o))) }
        removeChild(inputnodes.remove(i))
    }

    private fun addOutput(o: Output<*>) {
        allOutputs += o
        outputnodes.computeIfAbsent(o) {
            val on = OutputNode(o)
            children += on.graphics
            on
        }
    }

    private fun remOutput(o: Output<*>) {
        allOutputs -= o
        removeChild(outputnodes.remove(o))
        removeChildren(connections.removeIfKey2(o))
    }

    private fun addInOutput(io: InOutput<*>) {
        allInoutputs += io
        inoutputnodes.computeIfAbsent(io) {
            val ion = InOutputNode(io)
            inputnodes[io.i] = ion
            outputnodes[io.o] = ion
            io.i.getSources().forEach { o -> connections.computeIfAbsent(Key(io.i, o), Compute { IOLine(io.i, o) }) }
            children += ion.graphics
            ion
        }
    }

    private fun remInOutput(io: InOutput<*>) {
        allInoutputs -= io
        io.i.getSources().forEach { o -> removeChild(connections.remove2D(Key(io.i, o))) }
        removeChild(inoutputnodes.remove(io))
        removeChildren(connections.removeIfKey2(io.o))
        inputnodes -= io.i
        outputnodes -= io.o
    }

    private fun addConnection(i: Put<*>, o: Put<*>) {
        connections.computeIfAbsent(Key(i, o)) { IOLine(i, o) }
        drawGraph()
    }

    private fun remConnection(i: Put<*>, o: Put<*>) {
        connections.remove2D(i, o)?.let { it.disposer() }
        drawGraph()
    }

    init {
        isMouseTransparent = false
        isPickOnBounds = false
        tTranslate = switchpane.translateProperty()
        tScaleX = switchpane.zoomProperty()
        tScaleY = switchpane.zoomProperty()
        tScaleX attach { layoutChildren() } on disposer
        translateXProperty().bind(tTranslate.multiply(tScaleX))
        parentProperty().syncNonNullWhile { it.onEventUp(MOUSE_CLICKED) { selectNode(null) } } on disposer

        val av = anim(900.millis) {
            opacity = if (it==0.0) 0.0 else 1.0

            anim1Opacity = mapTo01(it, 0.0, 0.2)
            anim2Opacity = mapTo01(it, 0.25, 0.65)
            anim3Opacity = mapTo01(it, 0.8, 1.0)
            inputnodes.values.forEach { it.i.opacity = anim1Opacity }
            outputnodes.values.forEach { it.i.opacity = anim1Opacity }
            inoutputnodes.values.forEach { it.i.opacity = anim1Opacity }
            connections.forEach { _, _, line ->
                line.showClip1.setScaleXY(anim2Opacity)
                line.showClip2.setScaleXY(anim2Opacity)
            }
            inputnodes.values.forEach { it.t.opacity = anim3Opacity }
            outputnodes.values.forEach { it.t.opacity = anim3Opacity }
            inoutputnodes.values.forEach { it.t.opacity = anim3Opacity }
        }.applyAt(0.0)
        val avReducer = EventReducer.toLast<Any>(100.0) {
            if (APP.ui.layoutMode.value)
                av.playOpen()
        }
        APP.ui.layoutMode attach {
            if (it) avReducer.push(null)
            else av.playClose()
        } on disposer

        allInputs.onItemSync { addInput(it) } on disposer
        allInputs.onItemRemoved { remInput(it) } on disposer
        allOutputs.onItemSync { addOutput(it) } on disposer
        allOutputs.onItemRemoved { remOutput(it) } on disposer
        allInoutputs.onItemSync { addInOutput(it) } on disposer
        allInoutputs.onItemRemoved { remInOutput(it) } on disposer

        allLayers += this
        disposer += { allLayers -= this }
    }

    fun dispose() {
        children.clear()
        inputnodes.values.forEach { it.disposer() }
        outputnodes.values.forEach { it.disposer() }
        inoutputnodes.values.forEach { it.disposer() }
        connections.values.forEach { it.disposer() }
        disposer()
        allLayers -= this
    }

    private fun removeChild(n: Node?) {
        if (n!=null) children -= n
    }

    private fun removeChild(n: XNode<*, *>?) {
        if (n!=null) children -= n.graphics
    }

    private fun removeChildren(ns: Stream<out Node>) {
        ns.forEach { removeChild(it) }
    }

    private fun editBegin(n: XNode<*, *>?) {
        if (n==null) return

        editFrom = n
        edit = EditIOLine(n)

        // start effect: disable & visually differentiate bindable & unbindable nodes
        outputnodes.forEach { (_, node) -> node.onEditActive(true, false) }
        inputnodes.forEach { (_, node) -> node.onEditActive(true, node.input!!.isAssignable(editFrom!!.output!!)) }
        connections.forEach { _, line -> line.onEditActive(true) }
    }

    private fun editMove(e: MouseEvent) {
        if (edit==null || editFrom==null) return

        edit!!.line.lay(editFrom!!.cx, editFrom!!.cy, e.x, e.y)

        val n = inputnodes.values.asSequence()
                .filter { pyth(it.cx-e.x, it.cy-e.y)<8 }
                .find { it.input!!.isAssignable(editFrom!!.output!!) }

        if (editTo!==n) {
            editTo?.select(false)
            editTo = n
            editTo?.select(true)
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
                dataArrived(eTo.cx, eTo.cy)
            } else {
                eTo.input!!.bindAny(eFrom.output!!)
                connections.get(eTo.input, eFrom.output).dataSend()
            }
        }

        eTo?.select(false)
        e.line.disposer()

        // stop effect: disable & visually differentiate bindable nodes
        outputnodes.forEach { (_, node) -> node.onEditActive(false, true) }
        inputnodes.forEach { (_, node) -> node.onEditActive(false, true) }
        connections.forEach { _, line -> line.onEditActive(false) }
    }

    private fun selectNode(n: XNode<*, *>?) {
        selected?.select(false)
        selected = n
        selected?.select(true)
    }

    override fun layoutChildren() {
        val widgetIos = ArrayList<InOutput<*>>()
        val headerOffset = switchpane.root.localToScene(0.0, 0.0).y
        val translationOffset = tTranslate.get()

        inputnodes.values.forEach { it.graphics.isVisible = false }
        outputnodes.values.forEach { it.graphics.isVisible = false }
        inoutputnodes.values.forEach { it.graphics.isVisible = false }

        switchpane.container.rootParent.allWidgets.filter { it?.controller!=null }.forEach { w ->
            val c = w.controller
            val `is` = c.io.i.getInputsMixed().mapNotNull {
                when (it) {
                    is Input<*> -> inputnodes[it]
                    is InOutput<*> -> inoutputnodes[it]
                    else -> null
                }
            }
            val os = c.io.o.getOutputsMixed().mapNotNull {
                when (it) {
                    is Output<*> -> outputnodes[it]
                    is InOutput<*> -> inoutputnodes[it]
                    else -> null
                }
            }

            widgetIos += c.io.io.getInOutputs()

            val wr = w.areaTemp.root
            val b = wr.localToScene(wr.boundsInLocal)
            val baseX = b.minX/tScaleX.doubleValue()-translationOffset
            val baseY = b.minY-headerOffset
            val ww = b.width/tScaleX.doubleValue()
            val wh = b.height
            val ihx = wh/(`is`.size+1)
            val ohx = wh/(os.size+1)

            `is`.forEachIndexed { i, n ->
                n.graphics.isVisible = true
                n.graphics.autosize() // necessary before asking for size
                val is2 = n.i.layoutBounds.width/2.0
                n.cx = calcScaleX(baseX+padding)
                n.cy = calcScaleY(baseY+ihx*(i+1))
                n.graphics.relocate(n.cx-is2, n.cy-n.graphics.height/2)
            }
            os.forEachIndexed { i, n ->
                n.graphics.isVisible = true
                n.graphics.autosize() // necessary before asking for size
                val is2 = n.i.layoutBounds.width/2.0
                n.cx = calcScaleX(baseX+ww-padding-2*is2)
                n.cy = calcScaleY(baseY+ohx*(i+1))
                n.graphics.relocate(n.cx-n.graphics.layoutBounds.width+is2, n.cy-n.graphics.layoutBounds.height/2)
            }
        }

        val ioMinWidthX = 200.0
        val ioGapX = 10.0
        var ioOffsetX = 0.0
        val ioOffsetYShift = -10.0
        var ioOffsetY = height-150.0
        val ios = inoutputnodes.values.asSequence().filter { it.inoutput !in widgetIos }.sortedBy { it.inoutput!!.o.id.ownerId }.toList()
        for (n in ios) {
            n.graphics.isVisible = true
            n.graphics.autosize() // necessary before asking for size
            val is2 = n.i.layoutBounds.width/2.0
            n.cx = ioOffsetX
            n.cy = ioOffsetY
            n.graphics.relocate(n.cx-is2, n.cy-is2)
            ioOffsetX += max(n.graphics.layoutBounds.width, ioMinWidthX)+ioGapX
            ioOffsetY += ioOffsetYShift
        }

        drawGraph()
    }

    fun drawGraph() {
        connections.forEach { input, output, line ->
            val i: XNode<*, *>? = inputnodes[input] ?: outputnodes[input]
            val o: XNode<*, *>? = inputnodes[output] ?: outputnodes[output]
            if (i!=null && o!=null) {
                line.isVisible = i.graphics.isVisible && o.graphics.isVisible
                if (line.isVisible) {
                    when {
                        input is Input<*> && output is Input<*> -> line.layInputs(i.cx, i.cy, o.cx, o.cy)
                        input is Output<*> && output is Output<*> -> line.layOutputs(i.cx, i.cy, o.cx, o.cy)
                        else -> line.lay(i.cx, i.cy, o.cx, o.cy)
                    }
                }
            }
        }
    }

    private fun calcScaleX(x: Double): Double = x*tScaleX.doubleValue()

    private fun calcScaleY(y: Double): Double {
        val middle = height/2
        return middle+tScaleY.doubleValue()*(y-middle)
    }

    private abstract inner class XNode<X: XPut<*>, P: Pane>(xput: X) {
        val input: Input<*>?
        val output: Output<*>?
        val inoutput: InOutput<*>?
        lateinit var graphics: P
        var t = Text()
        var i = Icon()
        var cy = 80+random()*20
        var cx = 80+random()*20
        var selected = false
        val disposer = Disposer()

        val sceneXY: Point2D
            get() = Point2D(i.layoutBounds.minX+i.layoutBounds.width/2, i.layoutBounds.minY+i.layoutBounds.height/2)

        init {
            when (xput) {
                is Input<*> -> {
                    input = xput
                    output = null
                    inoutput = null
                }
                is Output<*> -> {
                    input = null
                    output = xput
                    inoutput = null
                }
                is InOutput<*> -> {
                    input = xput.i
                    output = xput.o
                    inoutput = xput
                }
                else -> failCase(xput)
            }

            i.opacity = anim1Opacity
            i.onEventDown(MOUSE_CLICKED) {
                when {
                    it.clickCount==1 -> selectNode(if (it.button==SECONDARY) null else this)
                    it.clickCount==2 && output?.value!=null -> APP.actionPane.show(output.value)
                }
                it.consume()
            }

            t.opacity = anim2Opacity
            val a = anim(250.millis) {
                t.opacity = it
                t.setScaleXY(0.8+0.2*it)
            }
            val valuePut = if (xput is Input<*>) input else output
            valuePut!!.sync { a.playCloseDoOpen { t.text = valuePut.xPutToStr() } } on disposer
        }

        fun select(v: Boolean) {
            if (selected==v) return
            selected = v
            i.pseudoClassStateChanged(pcXNodeSelected, v)
        }

        fun onEditActive(active: Boolean, canAccept: Boolean) {
            graphics.isDisable = active && !canAccept
        }

    }

    private inner class InputNode(xPut: Input<*>): XNode<Input<*>, HBox>(xPut) {

        init {
            graphics = HBox(8.0, i, t).apply {
                maxSize = 80 x 120
                alignment = Pos.CENTER_LEFT
                isMouseTransparent = false
            }
            i.styleclass(INODE_STYLECLASS)

            i.onEventUp(DRAG_ENTERED) { i.pseudoClassStateChanged(pcXNodeDragOver, true) }
            i.onEventUp(DRAG_EXITED) { i.pseudoClassStateChanged(pcXNodeDragOver, false) }
            installDrag(
                    i, IconFA.CLIPBOARD, "",
                    { true },
                    {
                        if (Df.WIDGET_OUTPUT in it.dragboard) {
                            val o = it.dragboard[Df.WIDGET_OUTPUT]
                            if (input!!.isAssignable(o))
                                input.bindAny(o)
                            drawGraph()
                        } else {
                            val o = it.dragboard.getAny()
                            if (input!!.isAssignable(o))
                                input.valueAny = o
                        }
                    }
            )

            t.text = input!!.xPutToStr()
        }

    }

    private inner class OutputNode(xPut: Output<*>): XNode<Output<*>, HBox>(xPut) {

        init {
            graphics = HBox(8.0, t, i).apply {
                maxSize = 80 x 120
                alignment = Pos.CENTER_RIGHT
                isMouseTransparent = false
            }
            i.styleclass(ONODE_STYLECLASS)

            i.onEventUp(DRAG_DETECTED) {
                if (selected) i.startDragAndDrop(TransferMode.LINK)[Df.WIDGET_OUTPUT] = output!!
                else editBegin(this)
                it.consume()
            }
        }

    }

    private inner class InOutputNode(xPut: InOutput<*>): XNode<InOutput<*>, VBox>(xPut) {

        init {
            graphics = VBox(8.0, i, t).apply {
                maxSize = 80 x 120
                alignment = Pos.CENTER_LEFT
                isMouseTransparent = false
            }
            i.styleclass(IONODE_STYLECLASS)

            i.onEventUp(DRAG_ENTERED) { i.pseudoClassStateChanged(pcXNodeDragOver, true) }
            i.onEventUp(DRAG_EXITED) { i.pseudoClassStateChanged(pcXNodeDragOver, false) }
            i.onEventUp(DRAG_DETECTED) {
                if (selected) i.startDragAndDrop(TransferMode.LINK)[Df.WIDGET_OUTPUT] = output!!
                else editBegin(this)
                it.consume()
            }
            installDrag(
                    i, IconFA.CLIPBOARD, "",
                    { e -> Df.WIDGET_OUTPUT in e.dragboard },
                    { e ->
                        val o = e.dragboard[Df.WIDGET_OUTPUT]
                        if (o!==output) {
                            input!!.bindAny(o)
                            drawGraph()
                        }
                    }
            )
        }

    }

    private inner class IOLine(val input: Put<*>?, val output: Put<*>?): Path() {
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
        private var notFinished = false
        private var notFinishedX = 0.0
        private var notFinishedY = 0.0
        private val lineGap = 20.0
        val disposer = Disposer()

        init {
            styleClass += IOLINE_STYLECLASS
            isMouseTransparent = false
            isPickOnBounds = false
            showClip1.setScaleXY(anim3Opacity)
            showClip2.setScaleXY(anim3Opacity)
            clip = showClip
            this@IOLayer.children += this
            disposer += { this@IOLayer.children -= this }

            effect.styleClass += "ioline-effect-line"
            effect.isMouseTransparent = true
            effect.clip = effectClip
            duplicateTo(effect)
            this@IOLayer.children += effect
            disposer += { this@IOLayer.children -= effect }

            if (edit?.line!=this && input!=null && output!=null) {
                output.sync { dataSend() } on disposer
            }
            onEventDown(MOUSE_CLICKED, SECONDARY) {
                if (input is Input<*> && output is Output<*>)
                    input.unbind(output)
            }
            onEventDown(DRAG_DETECTED) {
                editBegin(outputnodes[output])
                it.consume()
            }
        }

        fun onEditActive(active: Boolean) {
            isDisable = active
        }

        fun layInputs(inX: Double, inY: Double, outX: Double, outY: Double) {
            layInit(inX, inY, outX, outY)
            elements += LineTo(inX+lineGap, inY+lineGap)
            elements += LineTo(outX+lineGap, outY-lineGap)
            elements += LineTo(outX, outY)
        }

        fun layOutputs(inX: Double, inY: Double, outX: Double, outY: Double) {
            layInit(inX, inY, outX, outY)
            elements += LineTo(inX-lineGap, inY+lineGap)
            elements += LineTo(outX-lineGap, outY-lineGap)
            elements += LineTo(outX, outY)
        }

        @Suppress("LocalVariableName", "CanBeVal")
        fun lay(inX: Double, inY: Double, outX: Double, outY: Double) {
            layInit(inX, inY, outX, outY)
            var inX_ = inX
            var inY_ = inY
            var outX_ = outX
            var outY_ = outY

            val dx = outX_-inX_
            val dy = outY_-inY_
            if (dx>0) {
                // enhance start
                inY_ += lineGap*signum(dy)
                elements += LineTo(inX_, inY_)
                // enhance end
                notFinished = true
                notFinishedX = outX_
                notFinishedY = outY_
                outX_ -= lineGap*signum(dx)
                outY_ -= lineGap*signum(dy)
            }
            layTo(inX_, inY_, outX_, outY_)
        }

        fun layInit(inX: Double, inY: Double, outX: Double, outY: Double) {
            startX = outX
            startY = outY
            toX = inX
            toY = inY
            length = pyth(inX-outX, inY-outY)
            showClip1.radius = length
            showClip1.centerX = inX
            showClip1.centerY = inY
            showClip2.radius = length
            showClip2.centerX = outX
            showClip2.centerY = outY
            elements.clear()
            elements += MoveTo(inX, inY)
        }

        private fun layTo(inX: Double, inY: Double, outX: Double, outY: Double) {
            val dx = outX-inX
            val dy = outY-inY
            if (dx==0.0 || dy==0.0) {
                elements += LineTo(outX, outY)
                if (notFinished) {
                    notFinished = false
                    elements += LineTo(notFinishedX, notFinishedY)
                }
            } else {
                val dXy = min(abs(dx), abs(dy))
                val x = inX+signum(dx)*dXy
                val y = inY+signum(dy)*dXy
                elements += LineTo(x, y)
                layTo(x, y, outX, outY)
            }
        }

        fun dataSend() {
            val lengthNormalized = max(1.0, length/100.0)

            val pRunner = Circle(3.0).apply {
                styleClass += IOLINE_RUNNER_STYLECLASS
                this@IOLayer.children += this
            }
            val eRunner = Circle(15*lengthNormalized).apply {
                effectClip.children += this
                setScaleXY(0.0)
                centerX = startX
                centerY = startY
            }
            val a1 = PathTransition(1500.millis, this, pRunner).apply {
                rate = -1.0
                setOnFinished { this@IOLayer.children -= pRunner }
            }
            val ea1 = anim(300.millis) { eRunner.setScaleXY(sqrt(it)) }.delay(0.millis)
            val ea2 = PathTransition(1500.millis, this, eRunner).apply {
                rate = -1.0
                delay = 150.millis
                setOnFinished { dataArrived(toX, toY) }
            }
            val ea3 = anim(300.millis) { eRunner.setScaleXY(1-sqrt(it)) }.delay(1500.millis).then { effectClip.children -= eRunner }

            a1.playFrom(a1.duration)
            ea1.play()
            ea2.playFrom(ea2.duration)
            ea3.play()
        }

    }

    private inner class EditIOLine(node: XNode<*, *>)  {
        val line = IOLine(node.input, node.output)
        val isValueOnly = v(false)

        init {
            line.styleClass += "ioline-edit"
            isValueOnly attach { line.pseudoClassStateChanged(pseudoclass("value-only"), it) } on disposer

            val editDrawer = EventHandler<MouseEvent> {
                isValueOnly.value = it.isShiftDown
                layToMouse(it);
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
        const val INODE_STYLECLASS = "inode"
        const val ONODE_STYLECLASS = "onode"
        const val IONODE_STYLECLASS = "ionode"
        const val IOLINE_STYLECLASS = "ioline"
        const val IOLINE_RUNNER_STYLECLASS = "ioline-effect-dot"
        private val pcXNodeDragOver = pseudoclass("drag-over")
        private val pcXNodeSelected = pseudoclass("selected")

        @JvmField val allLayers = observableSet<IOLayer>()!!
        @JvmField val allConnections = Map2D<Put<*>, Put<*>, Any>()
        @JvmField val allInputs = observableSet<Input<*>>()!!
        @JvmField val allOutputs = observableSet<Output<*>>()!!
        @JvmField val allInoutputs = observableSet<InOutput<*>>()!!

        @JvmStatic
        fun relayout() {
            allLayers.forEach { it.requestLayout() }
        }

        @JvmStatic
        fun addConnectionE(i: Put<*>, o: Put<*>) {
            allConnections.put(i, o, Any())
            allLayers.forEach { it.addConnection(i, o) }
        }

        @JvmStatic
        fun remConnectionE(i: Put<*>, o: Put<*>) {
            allConnections.remove2D(i, o)
            allLayers.forEach { it.remConnection(i, o) }
        }

        @Suppress("UNCHECKED_CAST")
        private fun Input<*>.bindAny(output: Output<*>) = (this as Input<Any?>).bind(output as Output<Any?>)

        private fun <T> Put<T>.xPutToStr(): String = "${typeAsStr()} : $name\n${APP.instanceName[value]}"

        private fun <T> Put<T>.typeAsStr(): String {
            val t = typeRaw
            return if (t!=null) {
                val c = getRawType(t)
                when {
                    Collection::class.isSuperclassOf(c) -> {
                        val elementType = getRawType(t.asIf<ParameterizedType>()!!.actualTypeArguments[0])
                        val elementTypeName = APP.className[elementType].plural()
                        "List of $elementTypeName"
                    }
                    else -> APP.className[c]
                }
            } else {
                APP.className[type]
            }
        }

        private fun Path.duplicateTo(path: Path) {
            elements.onItemAdded { path.elements += it }
            elements.onItemRemoved { path.elements.clear() }
        }

        private fun IOLayer.dataArrived(x: Double, y: Double) {
            children += Circle(5.0).apply {
                styleClass += "ioline-effect-receive"
                isManaged = false
                relocate(x-radius, y-radius)

                anim(300.millis) {
                    setScaleXY(4*kotlin.math.sqrt(it))
                    opacity = 1-it*it
                }.then {
                    children -= this
                }.play()
            }
        }
    }

}