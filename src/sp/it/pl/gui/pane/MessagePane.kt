package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.VPos
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.objects.Text
import sp.it.pl.util.graphics.Util.layScrollVTextCenter
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.setMinPrefMaxSize
import sp.it.pl.util.graphics.stackPane

class MessagePane: OverlayPane<String>() {

    private val text: Text

    init {
        text = Text().apply {
            textOrigin = VPos.CENTER
            textAlignment = TextAlignment.JUSTIFY
            setMinPrefMaxSize(-1.0)
        }

        content = stackPane {
            padding = Insets(50.0)
            maxHeight = 200.0

            lay(CENTER) += layScrollVTextCenter(text).apply {
                prefWidth = 400.0
                maxWidth = 400.0
            }
        }
    }

    override fun show(data: String?) {
        text.text = data
        super.show()
    }

}