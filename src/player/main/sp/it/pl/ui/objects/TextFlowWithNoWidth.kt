package sp.it.pl.ui.objects

import javafx.scene.text.TextFlow

/** [javafx.scene.text.Text] with 0.0 [TextFlow.computeMinWidth], [TextFlow.computePrefWidth]. */
class TextFlowWithNoWidth: TextFlow() {
   override fun computeMinWidth(height: Double) = 0.0
   override fun computePrefWidth(height: Double) = 0.0
}