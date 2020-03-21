package sp.it.pl.ui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ScrollBarSkin
import javafx.scene.layout.StackPane
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.units.millis

/** ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover. */
open class ImprovedScrollBarSkin(scrollbar: ScrollBar): ScrollBarSkin(scrollbar) {
   private val onDispose = Disposer()

   init {
      initHoverAnimation()
      initHoverParentAnimation()
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

   fun initHoverAnimation() {
      val thumb = getFieldValue<StackPane>(this, "thumb")!!
      val a = anim(350.millis) {
         val isVertical = skinnable.orientation==VERTICAL
         val p = 1 + 1*it*it
         thumb.scaleX = if (isVertical) p else 1.0
         thumb.scaleY = if (isVertical) 1.0 else p
      }
      skinnable.onHoverOrDrag { a.playFromDir(it) } on onDispose
      onDispose += a::stop
   }

   fun initHoverParentAnimation() {
      val a = anim(350.millis) { node.opacity = 0.6 + 0.4*it*it }.applyNow()
      onDispose += a::stop
      onDispose += skinnable.parentProperty() syncNonNullWhile {
         a.applyAt(if (it.isHover) 1.0 else 0.0)
         it.onHoverOrDrag { a.playFromDir(it) }
      }
   }

}