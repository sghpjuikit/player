package sp.it.pl.layout.area

import javafx.animation.PathTransition
import javafx.beans.property.DoubleProperty
import javafx.beans.property.Property
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
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import sp.it.pl.gui.objects.contextmenu.ValueContextMenu
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.container.switchcontainer.SwitchPane
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
import sp.it.util.animation.Loop
import sp.it.util.async.executor.EventReducer
import sp.it.util.collections.map.Map2D
import sp.it.util.collections.map.Map2D.Key
import sp.it.util.dev.failCase
import sp.it.util.functional.Util.forEachCartesianHalfNoSelf
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
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.text
import sp.it.util.units.millis
import java.lang.Math.abs
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.max
import java.lang.Math.signum
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.lang.reflect.ParameterizedType
import java.util.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.properties.Delegates.observable

private typealias Compute<T> = java.util.function.Function<Key<Put<*>, Put<*>>, T>

/**
 * Display for [sp.it.pl.layout.widget.controller.io.XPut] of components, displaying their relations as am editable graph.
 */
class IOLayer(private val switchPane: SwitchPane): StackPane() {
    private val inputNodes = HashMap<Input<*>, XNode>()
    private val outputNodes = HashMap<Output<*>, XNode>()
    private val inoutputNodes = HashMap<InOutput<*>, InOutputNode>()
    private val links = Map2D<Put<*>, Put<*>, IOLink>()

    private val paneLinks = Pane()
    private val paneNodes = Pane()
    private val paneLabels = Pane()
    private val paneLabelsLayouter = Loop(Runnable {
        val labels = xNodes().map { it.label }.toList()

        forEachCartesianHalfNoSelf(labels) { l1, l2 ->
            val dir = atan2(l1.text.layoutY-l2.text.layoutY, l1.text.layoutX-l2.text.layoutX)
            val dist = pyth(l1.text.layoutX-l2.text.layoutX, l1.text.layoutY-l2.text.layoutY)
            when {
                dist>100 -> {}
                else -> {
                    val f = (100.0-(dist.coerceIn(0.0, 100.0)))/10.0
                    l1.text.layoutX += f*cos(dir)
                    l1.text.layoutY += f*sin(dir)
                    l2.text.layoutX -= f*cos(dir)
                    l2.text.layoutY -= f*sin(dir)
                }
            }
        }
        labels.forEach {
            val dist = pyth(it.x-it.text.layoutX, it.y-it.text.layoutY)
            val dir = atan2(it.y-it.text.layoutY, it.x-it.text.layoutX)
            val f = dist/10.0
            it.text.layoutX += f*cos(dir)
            it.text.layoutY += f*sin(dir)
            it.byX = it.x-it.text.layoutX
            it.byY = it.y-it.text.layoutY
        }
    })

    private val padding = 20.0
    private val tTranslate: DoubleProperty
    private val tScaleX: Property<Number>
    private val tScaleY: Property<Number>
    private var anim1Opacity = 0.0
    private var anim2Opacity = 0.0
    private var anim3Opacity = 0.0

    private var edit: EditIOLink? = null
    private var editFrom: XNode? = null
    private var editTo: XNode? = null
    private var selected: XNode? = null

    private val disposer = Disposer()

    private fun addInput(i: Input<*>) {
        allInputs += i
        inputNodes.computeIfAbsent(i) {
            val iNode = InputNode(i)
            i.getSources().forEach { o -> links.computeIfAbsent(Key(i, o), Compute { IOLink(i, o) }) }
            iNode
        }
        requestLayout()
    }

    private fun remInput(i: Input<*>) {
        allInputs -= i
        inputNodes.remove(i)?.let { it.disposer() }
        links.removeIf { it.key1()==i || it.key2()==i }.forEach { it.disposer() }
    }

    private fun addOutput(o: Output<*>) {
        allOutputs += o
        outputNodes.computeIfAbsent(o) {
            val on = OutputNode(o)
            on
        }
        requestLayout()
    }

    private fun remOutput(o: Output<*>) {
        allOutputs -= o
        outputNodes.remove(o)?.let { it.disposer() }
        links.removeIf { it.key1()==o || it.key2()==o }.forEach { it.disposer() }
    }

    private fun addInOutput(io: InOutput<*>) {
        allInoutputs += io
        inoutputNodes.computeIfAbsent(io) {
            val ion = InOutputNode(io)
            inputNodes[io.i] = ion
            outputNodes[io.o] = ion
            io.i.getSources().forEach { o -> links.computeIfAbsent(Key(io.i, o), Compute { IOLink(io.i, o) }) }
            ion
        }
        requestLayout()
    }

    private fun remInOutput(io: InOutput<*>) {
        allInoutputs -= io
        inputNodes -= io.i
        outputNodes -= io.o
        inoutputNodes.remove(io)?.let { it.disposer() }
        links.removeIf { it.key1()==io.i || it.key1()==io.o || it.key2()==io.i || it.key2()==io.o }.forEach { it.disposer() }
    }

    private fun addConnection(i: Put<*>, o: Put<*>) {
        links.computeIfAbsent(Key(i, o)) { IOLink(i, o) }
    }

    private fun remConnection(i: Put<*>, o: Put<*>) {
        links.remove2D(i, o)?.let { it.disposer() }
    }

    init {
        interact(doLayout = true, noMouse = false, noPick = false)
        tTranslate = switchPane.translateProperty()
        tScaleX = switchPane.zoomProperty()
        tScaleX attach { layoutChildren() } on disposer
        tScaleY = v(1.0)    // switchPane.zoomProperty()
        translateXProperty().bind(tTranslate.multiply(tScaleX))
        parentProperty().syncNonNullWhile { it.onEventUp(MOUSE_CLICKED) { selectNode(null) } } on disposer

        paneLinks.interact(doLayout = false, noMouse = false, noPick = false)
        paneNodes.interact(doLayout = false, noMouse = false, noPick = false)
        paneLabels.interact(doLayout = false, noMouse = true, noPick = false)
        children += listOf(paneLinks, paneNodes, paneLabels)

        visibleProperty() attach { if (it) paneLabelsLayouter.start() else paneLabelsLayouter.stop() } on disposer
        disposer += { paneLabelsLayouter.stop() }

        var aDir = true
        val av = anim(900.millis) {
            isVisible = it!=0.0
            isMouseTransparent = it!=1.0
            anim1Opacity = if(aDir) it else mapTo01(it, 0.0, 0.2)
            anim2Opacity = if(aDir) it else mapTo01(it, 0.25, 0.65)
            anim3Opacity = if(aDir) it else mapTo01(it, 0.8, 1.0)

            paneLinks.opacity = anim1Opacity
            links.forEach { _, _, link ->
                link.showClip1.setScaleXY(anim2Opacity)
                link.showClip2.setScaleXY(anim2Opacity)
            }
            paneLabels.opacity = anim3Opacity
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

        allInputs.onItemSync { addInput(it) } on disposer
        allInputs.onItemRemoved { remInput(it) } on disposer
        allOutputs.onItemSync { addOutput(it) } on disposer
        allOutputs.onItemRemoved { remOutput(it) } on disposer
        allInoutputs.onItemSync { addInOutput(it) } on disposer
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

    private fun editBegin(eFrom: XNode?) {
        if (eFrom==null) return

        selectNode(null)

        val e = EditIOLink(eFrom)
        edit = e
        editFrom = eFrom

        // start effect: disable & visually differentiate bindable & unbindable nodes
        e.link.change("highlighted", true)
        eFrom.i.change("highlighted", true)
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
                .filter { pyth(it.x-m.x, it.y-m.y)<8 }
                .find { it.input!!.isAssignable(eFrom.output!!) }

        if (editTo!==n) {
            editTo?.i?.change("highlighted", false)
            editTo = n
            editTo?.i?.change("highlighted", true)
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
        eFrom.i.change("highlighted", false)
        outputNodes.forEach { (_, node) -> node.onEditActive(false, true) }
        inputNodes.forEach { (_, node) -> node.onEditActive(false, true) }
        links.forEach { _, link -> link.onEditActive(false) }
    }

    private fun selectNode(n: XNode?) {
        selected?.select(false)
        selected = n
        selected?.select(true)
    }

    private fun xNodes(): Sequence<XNode> = (inputNodes.asSequence()+outputNodes.asSequence()+inoutputNodes.asSequence()).map { it.value }

    override fun layoutChildren() {
        val headerOffset = switchPane.root.localToScene(0.0, 0.0).y
        val translationOffset = tTranslate.value

        xNodes().forEach { it.graphics.isVisible = false }

        switchPane.container.rootParent.allWidgets.filter { it?.controller!=null }.forEach { w ->
            val c = w.controller
            val ins = c.io.i.getInputs().mapNotNull { inputNodes[it] }
            val ons = c.io.o.getOutputs().mapNotNull { outputNodes[it] }

            val b = w.areaTemp.root.let { it.localToScene(it.layoutBounds) }
            val baseX = b.minX/tScaleX.value.toDouble()-translationOffset
            val baseY = b.minY-headerOffset
            val ww = b.width/tScaleX.value.toDouble()
            val wh = b.height
            val ihx = wh/(ins.size+1)
            val ohx = wh/(ons.size+1)

            ins.forEachIndexed { i, n ->
                n.graphics.isVisible = true
                n.graphics.autosize()
                n.updatePosition(calcScaleX(baseX+padding), calcScaleY(baseY+ihx*(i+1)))
            }
            ons.forEachIndexed { i, n ->
                n.graphics.isVisible = true
                n.graphics.autosize()
                n.updatePosition(calcScaleX(baseX+ww-padding), calcScaleY(baseY+ohx*(i+1)))
            }
        }

        val ioMinWidthX = 200.0
        val ioGapX = 10.0
        var ioOffsetX = 0.0
        val ioOffsetYShift = -10.0
        var ioOffsetY = height-150.0
        val ions = inoutputNodes.values.asSequence().sortedBy { it.inoutput!!.o.id.ownerId }.toList()
        for (n in ions) {
            n.graphics.isVisible = true
            n.graphics.autosize()
            n.updatePosition(ioOffsetX, ioOffsetY)
            ioOffsetX += max(n.graphics.layoutBounds.width, ioMinWidthX)+ioGapX
            ioOffsetY += ioOffsetYShift
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

    private fun calcScaleY(y: Double): Double {
        val middle = height/2
        return middle+tScaleY.value.toDouble()*(y-middle)
    }

    private abstract inner class XNode(xPut: XPut<*>, iconStyleclass: String) {
        val input: Input<*>?
        val output: Output<*>?
        val inoutput: InOutput<*>?
        val i = Icon()
        val graphics = HBox(0.0, i)
        val label = XLabel()
        var x by observable(0.0) { _, _, nv -> label.y = nv+15 }
        var y by observable(0.0) { _, _, nv -> label.x = nv+15 }
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
            paneLabels.children += label.text
            disposer += { paneLabels.children -= label.text }

            i.styleclass(iconStyleclass)
            i.onEventDown(MOUSE_CLICKED) {
                when(it.clickCount) {
                    1 -> {
                        when(it.button) {
                            PRIMARY -> selectNode(this)
                            SECONDARY -> {
                                contextMenuInstance.setItemsFor(xPut)
                                contextMenuInstance.show(i, it)
                            }
                            else -> {}
                        }
                    }
                    2 -> {
                        output?.value?.let {
                            APP.actionPane.show(it)
                        }
                    }
                }
                it.consume()
            }

            val a = anim(250.millis) {
                label.text.opacity = it
                label.text.setScaleXY(0.8+0.2*it)
            }
            val valuePut = if (xPut is Input<*>) input else output
            valuePut!!.sync { a.playCloseDoOpen { label.text.text = valuePut.xPutToStr() } } on disposer
        }

        fun select(v: Boolean) {
            if (selected==v) return
            selected = v
            i.change("selected", v)
        }

        fun onEditActive(active: Boolean, canAccept: Boolean) {
            graphics.isDisable = active && !canAccept
        }

        fun updatePosition(toX: Double, toY: Double) {
            x = toX
            y = toY
            graphics.relocate(x-graphics.layoutBounds.width/2.0, y-graphics.layoutBounds.height/2.0)
            label.updatePosition()
        }

        inner class XLabel {
            var x = 0.0
            var y = 0.0
            var byX = 0.0
            var byY = 0.0
            var text = text {
                translateX = 20.0
                translateY = 20.0
            }

            fun updatePosition() {
                x = this@XNode.x
                y = this@XNode.y
                text.layoutX = this@XNode.x-byX
                text.layoutY = this@XNode.y-byY
            }
        }
    }

    private inner class InputNode(xPut: Input<*>): XNode(xPut, "inode") {
        init {
            i.onEventUp(DRAG_ENTERED) { i.change("drag-over", true) }
            i.onEventUp(DRAG_EXITED) { i.change("drag-over", false) }
            installDrag(
                    i, IconFA.CLIPBOARD, "",
                    { true },
                    {
                        if (Df.WIDGET_OUTPUT in it.dragboard) {
                            val o = it.dragboard[Df.WIDGET_OUTPUT]
                            if (input!!.isAssignable(o))
                                input.bindAny(o)
                        } else {
                            val o = it.dragboard.getAny()
                            if (input!!.isAssignable(o))
                                input.valueAny = o
                        }
                    }
            )
        }
    }

    private inner class OutputNode(xPut: Output<*>): XNode(xPut, "onode") {
        init {
            i.onEventUp(DRAG_DETECTED) {
                if (selected) i.startDragAndDrop(TransferMode.LINK)[Df.WIDGET_OUTPUT] = output!!
                else editBegin(this)
                it.consume()
            }
        }
    }

    private inner class InOutputNode(xPut: InOutput<*>): XNode(xPut, "ionode") {
        init {
            i.onEventUp(DRAG_ENTERED) { i.change("drag-over", true) }
            i.onEventUp(DRAG_EXITED) { i.change("drag-over", false) }
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
                        }
                    }
            )
        }
    }

    private inner class IOLink(val input: Put<*>?, val output: Put<*>?): Path() {
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

        init {
            styleClass += "iolink"
            isMouseTransparent = false
            isPickOnBounds = false
            showClip1.setScaleXY(anim3Opacity)
            showClip2.setScaleXY(anim3Opacity)
            clip = showClip
            paneLinks.children += this
            disposer += { paneLinks.children -= this }

            effect.styleClass += "iolink-effect-link"
            effect.isMouseTransparent = true
            effect.clip = effectClip
            duplicateTo(effect)
            paneLinks.children += effect
            disposer += { paneLinks.children -= effect }

            if (input!=null && output!=null) {
                output.attach { if (edit?.link!=this) dataSend() } on disposer
            }
            hoverProperty() attach {
                val n1 = inputNodes[input] ?: outputNodes[input]
                val n2 = inputNodes[output] ?: outputNodes[output]
                n1?.i?.change("highlighted", it)
                n2?.i?.change("highlighted", it)
            }
            onEventDown(MOUSE_CLICKED, SECONDARY) {
                if (input is Input<*> && output is Output<*>)
                    input.unbind(output)
            }
            onEventDown(DRAG_DETECTED) {
                editBegin(outputNodes[output])
                it.consume()
            }
        }

        fun onEditActive(active: Boolean) {
            isDisable = active
        }

        fun layInputs(inX: Double, inY: Double, outX: Double, outY: Double) {
            layInit(inX, inY, outX, outY)
            elements += MoveTo(inX+loX(1.0, 1.0), inY+loY(1.0, 1.0))
            elements += LineTo(inX+linkGap, inY+linkGap)
            elements += LineTo(outX+linkGap, outY-linkGap)
            elements += LineTo(outX+loX(1.0, 1.0), outY-loY(1.0, 1.0))
        }

        fun layOutputs(inX: Double, inY: Double, outX: Double, outY: Double) {
            layInit(inX, inY, outX, outY)
            elements += MoveTo(inX-loX(1.0, 1.0), inY+loY(1.0, 1.0))
            elements += LineTo(inX-linkGap, inY+linkGap)
            elements += LineTo(outX-linkGap, outY-linkGap)
            elements += LineTo(outX-loX(1.0, 1.0), outY-loY(1.0, 1.0))
        }

        @Suppress("LocalVariableName", "CanBeVal")
        fun lay(inX: Double, inY: Double, outX: Double, outY: Double) {
            layInit(inX, inY, outX, outY)

            val dx = signum(outX-inX)
            val dy = signum(outY-inY)
            val inX_ = inX+linkGap*dx
            val inY_ = inY+linkGap*dy
            elements += MoveTo(inX+loX(dx, dy), inY+loY(dx, dy))
            elements += LineTo(inX_, inY_)
            layTo(inX_, inY_, outX-linkGap*dx, outY-linkGap*dy)
            elements += LineTo(outX-loX(dx, dy), outY-loY(dx, dy))
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
        }

        private fun layTo(inX: Double, inY: Double, outX: Double, outY: Double) {
            val dx = outX-inX
            val dy = outY-inY
            if (dx==0.0 || dy==0.0) {
                elements += LineTo(outX, outY)
            } else {
                val dXy = min(abs(dx), abs(dy))
                val x = inX+signum(dx)*dXy
                val y = inY+signum(dy)*dXy
                elements += LineTo(x, y)
                layTo(x, y, outX, outY)
            }
        }

        private fun loX(x: Double, y: Double) = linkEndOffset*cos(atan2(y, x))

        private fun loY(x: Double, y: Double) = linkEndOffset*sin(atan2(y, x))

        fun dataSend() {
            val lengthNormalized = max(1.0, length/100.0)

            val pRunner = Circle(3.0).apply {
                styleClass += "iolink-effect-dot"
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

    private inner class EditIOLink(node: XNode)  {
        val link = IOLink(node.input, node.output)
        val isValueOnly = v(false)

        init {
            link.styleClass += "iolink-edit"
            isValueOnly attach { link.change("value-only", it) } on disposer

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
        @JvmField val allLayers = observableSet<IOLayer>()!!
        @JvmField val allLinks = Map2D<Put<*>, Put<*>, Any>()
        @JvmField val allInputs = observableSet<Input<*>>()!!
        @JvmField val allOutputs = observableSet<Output<*>>()!!
        @JvmField val allInoutputs = observableSet<InOutput<*>>()!!
        private val contextMenuInstance by lazy { ValueContextMenu<XPut<*>>() }

        @JvmStatic
        fun addLinkForAll(i: Put<*>, o: Put<*>) {
            allLinks.put(i, o, Any())
            allLayers.forEach { it.addConnection(i, o) }
        }

        @JvmStatic
        fun remLinkForAll(i: Put<*>, o: Put<*>) {
            allLinks.remove2D(i, o)
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

        private fun Node.interact(doLayout: Boolean, noMouse: Boolean, noPick: Boolean) {
            isManaged = doLayout
            isMouseTransparent = noMouse
            isPickOnBounds = noPick
        }

        private fun Node.change(pseudoClassState: String, state: Boolean) = pseudoClassStateChanged(pseudoclass(pseudoClassState), state)

        private fun Path.duplicateTo(path: Path) {
            elements.onItemAdded { path.elements += it }
            elements.onItemRemoved { path.elements.clear() }
        }

        private fun IOLayer.dataArrived(x: Double, y: Double) {
            children += Circle(5.0).apply {
                styleClass += "iolink-effect-receive"
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