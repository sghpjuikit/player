package sp.it.pl.layout.controller

import javafx.scene.layout.Pane
import sp.it.pl.layout.Widget
import sp.it.pl.layout.controller.io.IO
import sp.it.util.Locatable
import sp.it.util.conf.Configurable

/**
 * Defines behavior and ui of [Widget].
 *
 * Companion:
 * The class should be accompanied by [sp.it.pl.layout.WidgetCompanion].
 *
 * Providing API:
 * Controller is instantiated dynamically (using constructor) and normally the class is provided to the application
 * by user, so it is impossible to refer to the exact class and use the API. Exposing API by implementing interfaces
 * avoids this problem. See [sp.it.pl.layout.feature.Feature].
 *
 * Nonetheless, encapsulation is still recommended, i.e., standard public/private rules apply.
 *
 * Lifecycle:
 * - constructor is called
 * - [close] invoked
 *
 * Configurable [per instance] state:
 * Controller may wish to make its state user customizable. This is provided through the inherited
 * [sp.it.util.conf.Configurable] API.
 *
 * Persisted [per instance] state:
 * Controller may wish to make its state persistable. For fine-grained control, use [Widget.properties] directly, but
 * it is strongly recommended to simply make all persistable state configurable, as all configurable state is persisted
 * automatically. Note that it is not auto-restored. For that either use [SimpleController] or [LegacyController].
 *
 * Global [shared instance] state:
 * See [sp.it.pl.layout.appProperty].
 */
abstract class Controller(widget: Widget): Configurable<Any?>, Locatable by widget {

   /** Widget owning this controller. */
   @JvmField val widget = widget
   @JvmField val io = IO(widget.id)

   /** @return the ui root of this Controller */
   abstract fun uiRoot(): Pane

   /** Focuses the content. */
   abstract fun focus()

   /**
    * Stops all behavior and disposes any resources so that this controller can be garbage collected.
    * Executes when [widget] has [Widget.close] invoked.
    */
   abstract fun close()

}