package sp.it.pl.ui.objects

import javafx.scene.control.TreeTableCell
import javafx.scene.control.skin.TreeTableCellSkin

class SpitTreeTableCellSkin<S, T>(cell: TreeTableCell<S, T>): TreeTableCellSkin<S, T>(cell) {

   private val tooltipForTruncatedRowEffect = SpitLabeledSkin.tooltipForTruncatedRowEffectBuilder(this)

   override fun install() {
      super.install()
      tooltipForTruncatedRowEffect.subscribe()
   }

   override fun dispose() {
      tooltipForTruncatedRowEffect.unsubscribe()
      super.dispose()
   }

}