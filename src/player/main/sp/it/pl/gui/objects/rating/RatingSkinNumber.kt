package sp.it.pl.gui.objects.rating

import javafx.scene.control.Label
import javafx.scene.control.SkinBase
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on

/** Skin for [Rating] displaying the value as string. */
class RatingSkinNumber(r: Rating): SkinBase<Rating>(r) {

   private val label = Label()
   private var ratingOld = r.rating.value
   private val onDispose = Disposer()

   init {
      children setToOne label

      r.rating attach { update() } on onDispose
      r.partialRating attach { update() } on onDispose
      update()
   }

   private fun update() {
      val v = skinnable.rating.value?.let { if (skinnable.partialRating.value) it.toInt().toDouble() else it }
      label.text = "%.2f".format(v)
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

}