package sp.it.pl.ui.objects

import javafx.scene.control.TreeTableView
import javafx.scene.control.skin.TreeTableViewSkin
import javafx.scene.control.skin.TreeViewSkin
import javafx.scene.control.skin.VirtualFlow
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.Region
import kotlin.math.sign
import kotlin.math.sqrt
import sp.it.pl.main.emScaled
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Interpolators.Companion.easeOut
import sp.it.util.animation.Anim.Interpolators.Companion.interpolator
import sp.it.util.math.clip
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.onEventUp
import sp.it.util.type.Util
import sp.it.util.units.em
import sp.it.util.units.millis

/**
 * [TreeTableViewSkin] with:
 * - vertical scroll animation
 */
class SpitTreeTableViewSkin<S>(tree: TreeTableView<S>): TreeTableViewSkin<S>(tree) {

   override fun install() {
      super.install()
      scrollAnimationEffect.subscribe()
   }

   override fun dispose() {
      scrollAnimationEffect.unsubscribe()
      super.dispose()
   }

   private val scrollAnimationEffect = Subscribed {
      val flow = Util.getFieldValue<VirtualFlow<*>>(this, "flow")
      var a: Anim? = null
      skinnable.onEventUp(SCROLL) {
         it.consume()
         if (it.deltaY != 0.0 || it.deltaX != 0.0) {
            val scrollAmount = 0.25
            val vFrom = flow.position
            val vBy = -it.deltaY.sign*skinnable.height*scrollAmount/(skinnable.expandedItemCount*(skinnable.fixedCellSize.takeIf { it!=Region.USE_COMPUTED_SIZE } ?: 2.em.emScaled))
            val isMin = vBy<0 && vFrom<=0.0
            val isMax = vBy>0 && vFrom>=1.0
            if (!isMin && !isMax) {
               a?.stop()
               a = Anim.anim(200.millis) { flow.position = (vFrom + it*vBy).clip(0.0, 1.0) }.intpl(interpolator { sqrt(it) }.easeOut())
               a.play()
            }
         }
      }
   }

}