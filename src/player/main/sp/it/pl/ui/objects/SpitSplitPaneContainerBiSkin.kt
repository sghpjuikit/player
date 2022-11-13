package sp.it.pl.ui.objects

import javafx.collections.ObservableList
import javafx.scene.control.SplitPane
import javafx.scene.control.skin.SplitPaneSkin
import javafx.scene.layout.Pane
import sp.it.pl.main.APP
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.reactive.onChange
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.type.Util.getFieldValue

/**
 * SplitPane skin that adds animations & improved usability
 * - divider opacity animation on mouse hover
 * - mouse clicks are (always) passed to the parent (fixes [SplitPaneSkin] bug that causes it to consume all clicks)
 */
open class SpitSplitPaneContainerBiSkin(splitPane: SplitPane): SpitSplitPaneSkin(splitPane) {

   override fun install() {
      superInstall()
      initFixConsumingMouseClicks()
      initLayoutAnimation()
   }

   fun initLayoutAnimation() {
      val dividers = getFieldValue<ObservableList<Pane>>(this, "contentDividers")!!
      val a = anim(APP.ui.layoutModeDuration) {
         val o = it*it
         dividers.forEach { it.opacity = o }
      }

      fun isLayout() = skinnable?.parent?.pseudoClassStates?.any { it.pseudoClassName=="layout-mode" } == true

      a.applyAt(if (isLayout()) 1.0 else 0.0)
      skinnable.parentProperty() syncNonNullWhile { it.pseudoClassStates.onChange { a.playFromDir(isLayout()) } }

      disposer += a::stop
      disposer += { a.applyAt(0.0) }
   }

}