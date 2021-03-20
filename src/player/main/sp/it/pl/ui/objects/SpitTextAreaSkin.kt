package sp.it.pl.ui.objects

import javafx.scene.control.TextArea
import javafx.scene.control.skin.TextAreaSkin

/** TextAreaSkin skin that fixes [dispose] failing (JDK bug: https://github.com/javafxports/openjdk-jfx/issues/405). */
class SpitTextAreaSkin(control: TextArea): TextAreaSkin(control) {

   override fun dispose() {
      runCatching {
         super.dispose()
      }
   }

}