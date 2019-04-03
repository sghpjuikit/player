package sp.it.pl.gui.objects

import javafx.scene.control.TextArea
import javafx.scene.control.skin.TextAreaSkin

/** TextAreaSkin skin that fixes [dispose] throwing unsupported operation exception (JDK bug). */
class ImprovedTextAreaSkin(control: TextArea): TextAreaSkin(control) {

    override fun dispose() {
        runCatching {
            super.dispose()
        }
    }

}