package sp.it.pl.ui.objects

import javafx.scene.control.TableCell
import javafx.scene.control.skin.TableCellSkin

class SpitTableCellSkin<S, T>(cell: TableCell<S, T>): TableCellSkin<S, T>(cell) {

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