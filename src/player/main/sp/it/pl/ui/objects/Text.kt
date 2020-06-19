package sp.it.pl.ui.objects

import javafx.scene.text.Font
import sp.it.pl.main.fontScaled
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.sync

/** [javafx.scene.text.Text] with support for [wrappingWithNatural].  */
class Text: javafx.scene.text.Text {

   /** Whether [wrappingWidth] is maintained to achieve natural width and height based on current text. Default false. */
   val wrappingWithNatural = Subscribed { textProperty() sync { resizeToNatural() } }

   constructor(text: String? = null): super(text)

   constructor(x: Double, y: Double, text: String? = null): super(x, y, text)

   /** Sets wrappingWidth to achieve natural width and height based on current text. */
   fun resizeToNatural() {
      wrappingWidth = computeNaturalWrappingWidth(text, font)
   }

   companion object {
      fun computeNaturalWrappingWidth(text: String?, font: Font?) = (110 + text.orEmpty().length/4).fontScaled(font)
   }
}