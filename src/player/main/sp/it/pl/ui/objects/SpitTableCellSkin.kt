package sp.it.pl.ui.objects

import javafx.scene.control.TableCell
import javafx.scene.control.skin.ListCellSkin
import javafx.scene.control.skin.TableCellSkin

/**
 * [TableCellSkin] with:
 * - tooltip when showing truncated text
 */
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