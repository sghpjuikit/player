package sp.it.pl.layout.area

import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Region
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetLoader
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.graphics.pseudoclass
import sp.it.pl.util.graphics.size
import sp.it.pl.util.reactive.sync1If

abstract class Area<T: Container<*>>: ContainerNode {

    /** Container this area is associated with. */
    @JvmField val container: T
    /** Index of the child in the [container] */
    @JvmField val index: Int
    @JvmField val contentRoot = AnchorPane()
    final override val root = AnchorPane()

    /**
     * @param container widget's parent container
     * @param index index of the widget within the container
     */
    constructor(container: T, index: Int) {
        this.container = container.apply {
            properties.getOrPut(Double::class.javaObjectType, "padding", 0.0)
        }
        this.index = index

        contentRoot.styleClass += STYLECLASS_BGR
        root.layFullArea += contentRoot
    }

    /** @return the primary component */
    abstract fun getWidget(): Widget

    /** Detaches the primary component into standalone content in new window. */
    open fun detach() {
        val widget = getWidget()
        val sizeArea = root.size
        val sizeOld = widget.load().asIf<Region>()?.size ?: sizeArea
        widget.parent.addChild(widget.indexInParent(), null)

        WidgetLoader.WINDOW(widget)

        val w = widget.graphics.scene.window
        w.showingProperty().sync1If({ it }) {
            val wSize = w.size
            val sizeNew = widget.load().asIf<Region>()?.size ?: sizeArea
            val sizeDiff = sizeOld - sizeNew
            w.size = wSize+sizeDiff
        }
    }

    companion object {
        @JvmField val PSEUDOCLASS_DRAGGED = pseudoclass("dragged")
        @JvmField val STYLECLASS_BGR = listOf("block", "area")
        const val STYLECLASS_WIDGET_AREA_CONTROLS = "widget-control"
        const val STYLECLASS_CONTAINER_AREA_CONTROLS = "container-control"
    }

}