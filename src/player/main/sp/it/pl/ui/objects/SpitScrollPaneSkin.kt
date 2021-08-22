package sp.it.pl.ui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.control.skin.ScrollPaneSkin
import javafx.scene.input.ScrollEvent
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.Util

/** ScrollPaneSkin skin that consumes scroll events (if the content does not fit). */
open class SpitScrollPaneSkin(scrollPane: ScrollPane): ScrollPaneSkin(scrollPane) {

   init {
      initConsumeScrolling()
   }

   fun initConsumeScrolling() {
      node.onEventDown(ScrollEvent.ANY) {
         fun ScrollBar?.isScrollingNeeded() = this!=null && visibleAmount < max
         val vsb = Util.getScrollBar(skinnable, VERTICAL)
         val hsb = Util.getScrollBar(skinnable, VERTICAL)

         if (vsb.isScrollingNeeded() || hsb.isScrollingNeeded())
            it.consume()
      }
   }

}