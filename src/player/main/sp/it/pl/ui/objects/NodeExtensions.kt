package sp.it.pl.ui.objects

import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import sp.it.pl.main.emScaled
import sp.it.util.reactive.onEventUp
import sp.it.util.ui.install
import sp.it.util.ui.screenXy
import sp.it.util.ui.x2
import sp.it.util.ui.xy
import sp.it.util.units.em

/** Equivalent to [javafx.scene.control.Tooltip.install] but for [sp.it.pl.main.appTooltipInstant]. */
fun <NODE: Node> NODE.installInstant(tooltip: Tooltip, text: (NODE) -> String) {
   install(tooltip)
   onEventUp(MouseEvent.MOUSE_MOVED) {
      tooltip.xy = it.screenXy + 1.em.emScaled.x2
   }
   onEventUp(MouseEvent.MOUSE_ENTERED) {
      tooltip.hide()
      tooltip.text = text(this)
      tooltip.xy = it.screenXy + 1.em.emScaled.x2
   }
}