package sp.it.pl.ui.objects

import javafx.scene.control.Label
import javafx.scene.control.skin.LabelSkin
import javafx.scene.control.skin.ListCellSkin

class SpitLabelSkin(label: Label): LabelSkin(label) {

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