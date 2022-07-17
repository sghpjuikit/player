package sp.it.util.ui

import javafx.scene.control.ScrollPane
import javafx.scene.input.ScrollEvent.SCROLL
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown

/** Scroll this [ScrollPane] content horizontally even when vertical scrolling takes place. Consumes [SCROLL] event. */
fun ScrollPane.onScrollOnlyScrollHorizontally(): Subscription = onEventDown(SCROLL) {
   if (it.deltaX==0.0 && it.deltaY!=0.0) hvalue -= it.deltaY/(content?.layoutBounds?.width ?: 1.0)
   it.consume()
}