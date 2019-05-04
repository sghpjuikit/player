package sp.it.pl.layout.widget

import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Region
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.ContainerUi
import sp.it.util.functional.asIf
import sp.it.util.reactive.sync1If
import sp.it.util.ui.layFullArea
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.size

// TODO: remove class
abstract class Area<T: Container<*>>: ContainerUi {

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

        root.id = "widget-ui"
        root.layFullArea += contentRoot.apply {
            id = "widget-ui-contentRoot"
            styleClass += STYLECLASS
        }
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
        const val STYLECLASS = "widget-ui"
        @JvmField val PSEUDOCLASS_DRAGGED = pseudoclass("dragged")
    }

}