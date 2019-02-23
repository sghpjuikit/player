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

/**
 * Defines behavior and ui of [Widget].
 *
 * Controller is instantiated dynamically (using constructor) and normally the class is provided to the application
 * by user, so it is impossible to refer to the exact class and use the API. Exposing API by implementing interfaces
 * avoids this problem. See [sp.it.pl.layout.widget.feature.Feature].
 *
 * Nonetheless, encapsulation is still recommended, i.e., standard public/private rules apply.
 *
 * Controller is [sp.it.pl.util.conf.Configurable] and uses [widget] as value source for
 * configurable properties.
 *
 * Lifecycle:
 * - constructor is called
 * - [close] invoked
 *
 * Configurable state:
 * Controller may wish to make its state user customizable. This is provided through the inherited [Configurable] API.
 *
 * Persisted state:
 * Controller may wish to make its state persistable. For fine-grained control, use [Widget.properties] directly, but
 * it is strongly recommended to simply make all persistable state configurable, as all configurable state is persisted
 * automatically. Note that it is not auto-restored. For that either use [SimpleController] or [LegacyController]
 */
abstract class Controller(widget: Widget): CachedConfigurable<Any>, Locatable {

    /** Widget owning this controller. */
    @JvmField val widget = widget
    abstract val ownedOutputs: Outputs
    abstract val ownedInputs: Inputs
    override val location get() = widget.location
    override val userLocation get() = widget.userLocation

    /** @return the ui root of this Controller */
    @Throws(Exception::class)
    abstract fun loadFirstTime(): Node

    /** Focuses the content. */
    abstract fun focus()

    /**
     * Stops all behavior and disposes any resources so that this controller can be garbage collected.
     * Executes when [widget] has [Widget.close] invoked.
     */
    abstract fun close()

    /** @return all implemented features */
    fun getFeatures(): List<Feature> = widget.factory.getFeatures()

}

/** Controller for [Widget] with no [sp.it.pl.layout.widget.WidgetFactory]. */
class NoFactoryController(widget: Widget): SimpleController(widget) {
    init {
        root.lay += label("Widget ${widget.name} is not recognized")
    }
}

/** Controller for [Widget] that fails to instantiate its controller. */
class LoadErrorController(widget: Widget): SimpleController(widget) {
    init {
        root.lay += label("Widget ${widget.name} failed to load properly")
    }
}