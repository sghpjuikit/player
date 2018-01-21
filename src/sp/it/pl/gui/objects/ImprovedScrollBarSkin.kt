package sp.it.pl.gui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ScrollBarSkin
import javafx.scene.layout.StackPane
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.graphics.onHoverOrDragEnd
import sp.it.pl.util.graphics.onHoverOrDragStart
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.type.Util.getFieldValue

/** ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover. */
open class ImprovedScrollBarSkin(scrollbar: ScrollBar): ScrollBarSkin(scrollbar) {

    init {
        initHoverAnimation()
        initHoverParentAnimation()
    }

    fun initHoverAnimation() {
        val thumb = getFieldValue<StackPane>(this, "thumb")!!
        val a = Anim(millis(350)) {
            val isVertical = skinnable.orientation==VERTICAL
            val p = 1+1*it*it
            thumb.scaleX = if (isVertical) p else 1.0
            thumb.scaleY = if (isVertical) 1.0 else p
        }
        skinnable.onHoverOrDragStart { a.playOpen() }
        skinnable.onHoverOrDragEnd { a.playClose() }
    }

    fun initHoverParentAnimation() {
        val disposer = Disposer()
        val a = Anim(millis(350)) { node.opacity = 0.6+0.4*it*it }
        a.applier.accept(0.0)
        skinnable.parentProperty() sync {
            disposer()
            disposer += it.onHoverOrDragStart { a.playOpen() }
            disposer += it.onHoverOrDragEnd { a.playClose() }
        }
    }

}