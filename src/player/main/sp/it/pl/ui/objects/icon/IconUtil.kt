package sp.it.pl.ui.objects.icon

import javafx.scene.text.Text
import javafx.scene.text.TextBoundsType
import sp.it.util.functional.asIs

/** Setting [TextBoundsType.VISUAL] has an effect of center aligning the icon. See more at [TextBoundsType]. */
var Icon.boundsType: TextBoundsType
   get() = lookup("Text").asIs<Text>().boundsType
   set(value) {
      lookup("Text").asIs<Text>().boundsType = value
      requestLayout()
   }