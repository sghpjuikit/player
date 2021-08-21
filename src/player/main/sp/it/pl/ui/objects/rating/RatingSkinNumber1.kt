package sp.it.pl.ui.objects.rating

import javafx.scene.control.Label
import javafx.scene.control.SkinBase
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on

/** Skin for [Rating] displaying the value as rational 0-1 double. */
class RatingSkinNumber1(r: Rating): SkinBase<Rating>(r) {

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
         if (s.partialRating.value) (s.icons.value*it).toInt()/s.icons.value.toDouble()
         else it
      }
      label.text = if (v==null) "" else "%.2f".format(v)
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

}