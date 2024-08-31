package sp.it.pl.ui.objects

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.Labeled
import javafx.scene.control.Skin
import javafx.scene.control.TableCell
import javafx.scene.control.skin.LabeledSkinBase
import javafx.scene.control.skin.TableCellSkin
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import sp.it.util.functional.asIf
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.util.dev.printIt
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.zip
import sp.it.util.ui.install
import sp.it.util.ui.screenXy
import sp.it.util.ui.uninstall
import sp.it.util.ui.x2
import sp.it.util.ui.xy
import sp.it.util.units.em

object SpitLabeledSkin {

   private val configInfoTooltip = appTooltip("").apply {
      isWrapText = true
      maxWidth = 350.emScaled
   }

   interface SpitLabeledSkinWithTooltip { fun needsTooltip(): ObservableValue<Boolean> }

   val tooltipForTruncatedRowEffectTextPropertyKey = Any()

   val tooltipForTruncatedRowEffectBuilder = { skin: Skin<out Labeled> ->
      val t = skin.skinnable.asIf<SpitLabeledSkinWithTooltip>()?.needsTooltip() ?: skin.skinnable.textTruncatedProperty()
      Subscribed {
         Subscription(
            t attach {
               if (it) skin.skinnable.install(configInfoTooltip)
               else skin.skinnable.uninstall(configInfoTooltip)
            },
            skin.skinnable.onEventUp(MOUSE_MOVED) {
               if (t.value)
                  configInfoTooltip.xy = it.screenXy + 1.em.emScaled.x2
            },
            skin.skinnable.onEventUp(MOUSE_ENTERED) {
               if (t.value) {
                  configInfoTooltip.text = skin.skinnable.properties[tooltipForTruncatedRowEffectTextPropertyKey]?.toString() ?: skin.skinnable.text
                  configInfoTooltip.xy = it.screenXy + 1.em.emScaled.x2
               }
            }
         )
      }
   }

}