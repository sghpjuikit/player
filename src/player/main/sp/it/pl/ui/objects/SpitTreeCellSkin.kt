package sp.it.pl.ui.objects

import javafx.scene.control.TreeCell
import javafx.scene.control.TreeTableCell
import javafx.scene.control.skin.TreeCellSkin
import javafx.scene.control.skin.TreeTableCellSkin

class SpitTreeCellSkin<T>(cell: TreeCell<T>): TreeCellSkin<T>(cell) {

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