package sp.it.pl.ui.objects

import javafx.scene.control.ListCell
import javafx.scene.control.skin.ListCellSkin

class SpitListCellSkin<T>(cell: ListCell<T>): ListCellSkin<T>(cell) {

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