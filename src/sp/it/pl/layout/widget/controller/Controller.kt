package sp.it.pl.layout.widget.controller

import javafx.scene.Node
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.Inputs
import sp.it.pl.layout.widget.controller.io.Outputs
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.util.Locatable
import sp.it.pl.util.conf.CachedConfigurable
import sp.it.pl.util.graphics.label
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.graphics.stackPane

/**
 * Defines behavior and content of [Widget].
 *
 * Controller is instantiated dynamically, i.e., (safely) by reflection, so its public API is not readily
 * visible (unless reflection is used). It can expose its API by implementing interfaces.
 * More in [sp.it.pl.layout.widget.feature.Feature].
 *
 * Nonetheless, encapsulation is still recommended, i.e., standard public/private rules apply.
 *
 * Controller is [sp.it.pl.util.conf.Configurable] and uses [ownerWidget] as value source for
 * configurable properties.
 *
 * Lifecycle:
 * - constructor is called
 * - optionally [refresh] invoked by user
 * - [close] invoked
 */
interface Controller: CachedConfigurable<Any>, Locatable {

    /** Loads the content first time. Only called once.  */
    @JvmDefault
    @Throws(Exception::class)
    fun loadFirstTime(): Node = this as Node // TODO: remove dangerous implementation

    /** To be removed. */
    @JvmDefault
    fun init() {}

    /**
     * Refreshes the content. Invoked by user.
     *
     * Default implementation does nothing.
     */
    @JvmDefault
    fun refresh() {}

    /**
     * Focuses the content.
     *
     * Default implementation does nothing.
     */
    @JvmDefault
    fun focus() {}

    /** @return widget owning this controller */
    val ownerWidget: Widget

    /**
     * Executes immediately before widget is closed. Widget is not
     * expected to be used after this method is invoked. Use to free resources.
     * Note that incorrect or no releasing of the resources (such as listeners)
     * might prevent this controller from being garbage collected.
     *
     * Default implementation does nothing.
     */
    @JvmDefault
    fun close() {}

    /**
     * Whether widget displays any content. Empty controller generally means there is no content
     * in the widget whatsoever, but it depends on implementation.
     *
     * Default implementation returns false.
     *
     * @return widget content is empty
     */
    @JvmDefault
    fun isEmpty(): Boolean = false

    val ownedOutputs: Outputs

    val ownedInputs: Inputs

    /** @return all implemented features */
    @JvmDefault
    fun getFeatures(): List<Feature> = ownerWidget.factory.getFeatures()

    @JvmDefault
    override val location get() = ownerWidget.location

    @JvmDefault
    override val userLocation get() = ownerWidget.userLocation

}

/** Controller for [Widget] with no [sp.it.pl.layout.widget.WidgetFactory]. */
class NoFactoryController(widget: Widget): SimpleController(widget) {
    init {
        layFullArea += stackPane {
            lay += label("Widget ${widget.name} is not recognized")
        }
    }
}

/** Controller for [Widget] that fails to instantiate its controller. */
class LoadErrorController(widget: Widget): SimpleController(widget) {
    init {
        layFullArea += stackPane {
            lay += label("Widget ${widget.name} failed to load properly")
        }
    }
}