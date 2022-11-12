package sp.it.pl.ui.objects

import javafx.collections.ObservableList
import javafx.scene.control.SplitPane
import javafx.scene.control.skin.SplitPaneSkin
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.dev.printStacktrace
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.units.millis

/**
 * ScrollBar skin that adds animations & improved usability
 * - thumb expands on mouse hover
 * - mouse clicks are (always) passed to the parent (fixes [SplitPaneSkin] bug that causes it to consume all clicks)
 */
open class SpitSplitPaneSkin(splitPane: SplitPane): SplitPaneSkin(splitPane) {
   private val disposer = Disposer()

   override fun install() {
      super.install()
      initHoverAnimation()
      initFixConsumingMouseClicks()
   }

   override fun dispose() {
      disposer()
      super.dispose()
   }

   fun initFixConsumingMouseClicks() {
      consumeMouseEvents(false)

      node.onEventDown(MOUSE_CLICKED) {
         skinnable?.parent?.fireEvent(
            it.copyFor(it.source, skinnable?.parent!!)
         )
         it.consume()
      } on disposer
   }

   fun initHoverAnimation() {
      val dividers = getFieldValue<ObservableList<Pane>>(this, "contentDividers")!!
      val a = anim(350.millis) {
         val o = 0.4 + 0.6*it*it
         dividers.forEach { it.opacity = o }
      }

      a.applyAt(if (skinnable.isHover) 1.0 else 0.0)
      skinnable.onHoverOrDrag { a.playFromDir(it) } on disposer
      skinnable.onHoverOrDrag { if (!it) printStacktrace() } on disposer
      disposer += a::stop
      disposer += { a.applyAt(1.0) }
   }

}