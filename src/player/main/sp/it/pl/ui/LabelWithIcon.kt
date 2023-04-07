package sp.it.pl.ui

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.control.Hyperlink
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.onClickDelegateKeyTo
import sp.it.pl.ui.objects.icon.onClickDelegateMouseTo

class LabelWithIcon(glyph: GlyphIcons, text: String = ""): Hyperlink(text) {
   val icon = Icon(glyph).apply { isMouseTransparent = true; isFocusTraversable = false }

   init {
      isUnderline = false
      graphic = icon
      icon.focusOwner.value = this
      icon.onClickDelegateKeyTo(this)
      icon.onClickDelegateMouseTo(this)
   }

   fun select(s: Boolean) {
      icon.selectHard(s)
   }
}