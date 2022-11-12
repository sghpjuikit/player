package sp.it.pl.ui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ScrollBarSkin
import javafx.scene.layout.StackPane
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.dev.printStacktrace
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.ui.onHoverOrInDrag
import sp.it.util.units.millis

/** ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover. */
open class SpitScrollBarSkin(scrollbar: ScrollBar): ScrollBarSkin(scrollbar) {
   private val disposer = Disposer()

   override fun install() {
      super.install()
      initHoverAnimation()
      initHoverParentAnimation()
   }

   override fun dispose() {
      disposer()
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
      skinnable.onHoverOrInDrag { a.playFromDir(it); if (!it) printStacktrace() } on disposer
      disposer += a::stop
   }

   fun initHoverParentAnimation() {
      val thumb = getFieldValue<StackPane>(this, "thumb")!!
      val track = getFieldValue<StackPane>(this, "track")!!
      val a = anim(350.millis) {
         val opacity = 0.6 + 0.4*it*it
         thumb.opacity = opacity
         track.opacity = opacity
      }.applyNow()

      disposer += skinnable.parentProperty() syncNonNullWhile {
         a.applyAt(if (it.isHover) 1.0 else 0.0)
         it.onHoverOrDrag { a.playFromDir(it) }
      }
      disposer += a::stop
   }

}