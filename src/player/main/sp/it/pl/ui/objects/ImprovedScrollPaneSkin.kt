package sp.it.pl.ui.objects

import javafx.scene.control.ScrollPane
import javafx.scene.control.skin.ScrollPaneSkin
import javafx.scene.input.ScrollEvent
import sp.it.util.reactive.onEventDown

/** ScrollPaneSkin skin that consumes scroll events. */
open class ImprovedScrollPaneSkin(scrollPane: ScrollPane): ScrollPaneSkin(scrollPane) {

   init {
      initConsumeScrolling()
   }

   fun initConsumeScrolling() {
      node.onEventDown(ScrollEvent.ANY) { it.consume() }
   }

}