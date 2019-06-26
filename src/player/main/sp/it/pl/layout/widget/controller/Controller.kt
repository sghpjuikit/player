package sp.it.pl.layout.widget.controller

import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.layout.Pane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.IO
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.layout.widget.isCompiling
import sp.it.pl.main.appProgressIndicator
import sp.it.util.Locatable
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.interpolator.ElasticInterpolator
import sp.it.util.conf.Configurable
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.vBox
import sp.it.util.units.millis

/**
 * Defines behavior and ui of [Widget].
 *
 * Providing API:
 * Controller is instantiated dynamically (using constructor) and normally the class is provided to the application
 * by user, so it is impossible to refer to the exact class and use the API. Exposing API by implementing interfaces
 * avoids this problem. See [sp.it.pl.layout.widget.feature.Feature].
 *
 * Nonetheless, encapsulation is still recommended, i.e., standard public/private rules apply.
 *
 * Lifecycle:
 * - constructor is called
 * - [close] invoked
 *
 * Configurable state:
 * Controller may wish to make its state user customizable. This is provided through the inherited
 * [sp.it.util.conf.Configurable] API.
 *
 * Persisted state:
 * Controller may wish to make its state persistable. For fine-grained control, use [Widget.properties] directly, but
 * it is strongly recommended to simply make all persistable state configurable, as all configurable state is persisted
 * automatically. Note that it is not auto-restored. For that either use [SimpleController] or [LegacyController].
 */
abstract class Controller(widget: Widget): Configurable<Any>, Locatable {

   /** Widget owning this controller. */
   @JvmField val widget = widget
   @JvmField val io = IO(widget.id)
   override val location get() = widget.location
   override val userLocation get() = widget.userLocation

   /** @return the ui root of this Controller */
   @Throws(Exception::class)
   abstract fun loadFirstTime(): Pane

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
      root.lay += vBox(5, CENTER) {
         lay += label("Widget ${widget.name} is not recognized")
         lay += compileInfoUi()
      }
   }
}

/** Controller for [Widget] that fails to instantiate its controller. */
class LoadErrorController(widget: Widget): SimpleController(widget) {
   init {
      root.lay += vBox(5, CENTER) {
         lay += label("Widget ${widget.name} failed to load properly")
         lay += compileInfoUi()
      }
   }
}

private fun SimpleController.compileInfoUi(): Node {
   val isCompiling = widget.factory.isCompiling(onClose)
   return hBox(10, CENTER) {
      lay += label("Compiling...").apply {
         val a = anim { setScaleXY(it*it) }.delay(500.millis).dur(500.millis).intpl(ElasticInterpolator()).applyNow()
         isCompiling sync { if (it) a.playOpen() else a.playClose() } on onClose
      }
      lay += appProgressIndicator().apply {
         isCompiling sync { progress = if (it) -1.0 else 1.0 } on onClose
      }
   }
}