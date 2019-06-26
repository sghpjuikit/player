package sp.it.pl.layout.container

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_H
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_V
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MAGIC
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.AnchorPane
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.AltState
import sp.it.pl.layout.Component
import sp.it.pl.layout.Layouter
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetUi
import sp.it.pl.main.APP
import sp.it.util.access.V
import sp.it.util.access.toggleNext
import sp.it.util.access.value
import sp.it.util.collections.map.PropertyMap
import sp.it.util.collections.setToOne
import sp.it.util.dev.failCase
import sp.it.util.dev.failIf
import sp.it.util.functional.asIf
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncTo
import sp.it.util.ui.layFullArea
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.setAnchors
import sp.it.util.ui.x2
import kotlin.reflect.KProperty0

class BiContainerUi(c: BiContainer): ContainerUi<BiContainer>(c) {
    private val prop: PropertyMap<String>
    private val root1 = AnchorPane()
    private val root2 = AnchorPane()
    private val splitPane = SplitPane(root1, root2)
    private var ui1: ComponentUi? = null
    private var ui2: ComponentUi? = null

    var absoluteSize: Int
        get() = prop.getI("abs_size")
        set(i) {
            failIf(i<0 || i>2) { "Only values 0,1,2 allowed" }

            SplitPane.setResizableWithParent(root1, i!=1)
            SplitPane.setResizableWithParent(root2, i!=2)

            prop["abs_size"] = i
            ui1?.asIf<WidgetUi>()?.controls?.updateAbsB()
            ui2?.asIf<WidgetUi>()?.controls?.updateAbsB()
            container.children[1]?.asIf<Container<*>>()?.ui?.asIf<ContainerUi<*>>()?.let { it.controls.ifSet { it.updateIcons() } }
            container.children[2]?.asIf<Container<*>>()?.ui?.asIf<ContainerUi<*>>()?.let { it.controls.ifSet { it.updateIcons() } }
        }
    var collapsed: Int
        get() = prop.getI("col")
        set(i) {
            prop["col"] = i
            splitPane.orientationProperty().removeListener(collapsedL1)
            if (i!=0) splitPane.orientationProperty().addListener(collapsedL1)
            splitPane.removeEventFilter(MOUSE_RELEASED, collapsedL2)
            if (i!=0) splitPane.addEventFilter(MOUSE_RELEASED, collapsedL2)
            splitPane.removeEventFilter(MOUSE_DRAGGED, collapsedL3)
            if (i!=0) splitPane.addEventFilter(MOUSE_DRAGGED, collapsedL3)
            updateSplitPosition()
        }
    val isCollapsed: Boolean
        get() = collapsed!=0

    private val collapsedL1 = ChangeListener<Orientation> { _, _, _ -> updateSplitPosition() }
    private val collapsedL2 = EventHandler<MouseEvent> {
        if (it.clickCount==2 && it.button==PRIMARY && isCollapsed) {
            val so = splitPane.orientation
            val isGrabber = so==VERTICAL && collapsed==-1 && it.y<grabberSize ||
                so==VERTICAL && collapsed==1 && it.y>splitPane.height - grabberSize ||
                so==HORIZONTAL && collapsed==-1 && it.x<grabberSize ||
                so==HORIZONTAL && collapsed==1 && it.x>splitPane.width - grabberSize

            if (isGrabber)
                collapsed = 0
        }
    }
    private val collapsedL3 = EventHandler<MouseEvent> {
        if (isCollapsed) {
            val so = splitPane.orientation
            val isGrabber = so==VERTICAL && collapsed==-1 && it.y<grabberSize ||
                so==VERTICAL && collapsed==1 && it.y>splitPane.height - grabberSize ||
                so==HORIZONTAL && collapsed==-1 && it.x<grabberSize ||
                so==HORIZONTAL && collapsed==1 && it.x>splitPane.width - grabberSize

            if (isGrabber)
                collapsed = 0
        }
    }

    init {
        root.layFullArea += splitPane
        root1.minSize = 0.x2
        root2.minSize = 0.x2

        prop = container.properties
        prop.getOrPut(Double::class.javaObjectType, "pos", 0.5)
        prop.getOrPut(Int::class.javaObjectType, "abs_size", 0)
        prop.getOrPut(Int::class.javaObjectType, "col", 0)

        container.orientation syncTo splitPane.orientationProperty()
        absoluteSize = prop.getI("abs_size")
        collapsed = prop.getI("col")

        splitPane.onMouseClicked = root.onMouseClicked

        // initialize position
        splitPane.sync1IfInScene {
            root.parentProperty() sync { updateSplitPosition() }
        }

        // maintain position in resize (SplitPane position is affected by distortion in case of small sizes)
        splitPane.layoutBoundsProperty() sync {
            if (prop.getI("abs_size")==0)
                updateSplitPosition()
        }

        val position = V(computeSplitPosition())

        // remember position for persistence
        splitPane.dividers[0].positionProperty() attach {
            // only when the value changes manually - by user (hence the isPressed()).
            // This way initialization and rounding errors will never affect the value
            // and can not produce a domino effect. No matter how badly the value gets
            // distorted when applying to the layout, its stored value will be always exact.
            if (splitPane.isPressed)
                position.value = it.toDouble()
        }

        // apply/persist position (cant do this in positionProperty().addListener()) because it corrupts collapsed state restoration
        splitPane.onEventUp(MOUSE_RELEASED) {
            val v = position.value
            if (v>0.01 && v<0.99) {
                if (!isCollapsed)
                    prop["pos"] = v
            } else {
                val collapsedNew = if (v<0.5) -1 else 1
                if (it.clickCount==1 && collapsed!=collapsedNew)
                    collapsed = collapsedNew
            }
        }

        // maintain collapsed pseudoclass
        position sync { splitPane.pseudoClassChanged("collapsed", it<0.01 || it>0.99) }
    }

    override fun buildControls() = super.buildControls().apply {
        val orientB = Icon(MAGIC, -1.0, "Change orientation").addExtraIcon().onClickDo { container.orientation.toggleNext() }.styleclass("header-icon")
        container.orientation sync { orientB.icon(if (it==VERTICAL) ELLIPSIS_V else ELLIPSIS_H) } on disposer
    }

    private fun updateSplitPosition() {
        splitPane.dividers[0].position = computeSplitPosition()
        root1.maxSize = if (collapsed==-1) 0.x2 else (-1).x2
        root2.maxSize = if (collapsed==+1) 0.x2 else (-1).x2
    }

    private fun computeSplitPosition() = when (collapsed) {
        -1 -> 0.0
        0 -> prop.getD("pos")
        1 -> 1.0
        else -> failCase(collapsed)
    }

    fun setComponent(i: Int, c: Component?) {
        failIf(i!=1 && i!=2) { "Index must be 1 or 2" }

        fun <T> T.closeUi(ui: KProperty0<ComponentUi?>) = apply { if (ui.value is Layouter || ui.value is WidgetUi) ui.value?.dispose() }
        fun <T: AltState> T.showIfLM() = apply { if (APP.ui.isLayoutMode) show() }

        val r = if (i==1) root1 else root2
        val ui = if (i==1) ::ui1 else ::ui2
        val n: Node = when (c) {
            is Container<*> -> {
                ui.value = null.closeUi(ui)
                c.load(r).apply {
                    c.showIfLM()
                }
            }
            is Widget -> {
                ui.value = ui.value.takeIf { it is WidgetUi && it.widget==c } ?: WidgetUi(container, i, c).closeUi(ui).showIfLM()
                ui.value!!.root
            }
            null -> {
                ui.value = ui.value.takeIf { it is Layouter } ?: Layouter(container, i).closeUi(ui).showIfLM()
                ui.value!!.root
            }
            else -> failCase(c)
        }

        r.children.setToOne(n)
        n.setAnchors(0.0)
    }

    /** Toggle fixed size between child1/child2/off. */
    fun toggleAbsoluteSize() {
        absoluteSize = absoluteSize.let { if (it==2) 0 else it + 1 }
    }

    /** Toggle fixed size for specified children between true/false. */
    fun toggleAbsoluteSizeFor(i: Int) {
        absoluteSize = if (absoluteSize==i) 0 else i
    }

    /** Collapse on/off to the left or top depending on the orientation. */
    fun toggleCollapsed1() {
        collapsed = if (isCollapsed) 0 else -1
    }

    /** Collapse on/off to the right or bottom depending on the orientation. */
    fun toggleCollapsed2() {
        collapsed = if (isCollapsed) 0 else 1
    }

    override fun show() {
        super.show()
        ui1.asIf<Layouter>()?.show()
        ui2.asIf<Layouter>()?.show()
    }

    override fun hide() {
        super.hide()
        ui1.asIf<Layouter>()?.hide()
        ui2.asIf<Layouter>()?.hide()
    }

    companion object {
        private const val grabberSize = 20.0
    }

}