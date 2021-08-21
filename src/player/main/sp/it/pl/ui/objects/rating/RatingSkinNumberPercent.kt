package sp.it.pl.ui.objects.rating

import javafx.scene.control.Label
import javafx.scene.control.SkinBase
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on

/** Skin for [Rating] displaying the value as 0-100%. */
class RatingSkinNumberPercent(r: Rating): SkinBase<Rating>(r) {

   private val label = Label()
   private val onDispose = Disposer()

   init {
      children setToOne label

      r.rating attach { update() } on onDispose
      r.partialRating attach { update() } on onDispose
      update()
   }

   private fun update() {
      val s = skinnable
      val v = s.rating.value?.let {
         if (s.partialRating.value) (100 * ((s.icons.value*it).toInt()/s.icons.value.toDouble())).toInt()
         else (it*100.0).toInt()
      }
      label.text = if (v==null) "" else "%d%s".format(v, "%")
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

}