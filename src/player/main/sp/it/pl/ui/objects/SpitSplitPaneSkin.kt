package sp.it.pl.ui.objects

import javafx.collections.ObservableList
import javafx.scene.control.SplitPane
import javafx.scene.control.skin.SplitPaneSkin
import javafx.scene.layout.Pane
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runLater
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.containsMouse
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.units.millis

/** ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover. */
open class SpitSplitPaneSkin(splitPane: SplitPane): SplitPaneSkin(splitPane) {
   private val onDispose = Disposer()

   init {
      initHoverAnimation()
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

   fun initHoverAnimation() {
      val dividers = getFieldValue<ObservableList<Pane>>(this, "contentDividers")!!
      val a = anim(350.millis) {
         val o = 0.4 + 0.6*it*it
         dividers.forEach { it.opacity = o }
      }

      skinnable.sync1IfInScene { runLater { a.applyAt(if (skinnable.containsMouse()) 1.0 else 0.0) } } on onDispose
      skinnable.onHoverOrDrag { a.playFromDir(it) } on onDispose
      onDispose += a::stop
      onDispose += { a.applyAt(1.0) }
   }

}