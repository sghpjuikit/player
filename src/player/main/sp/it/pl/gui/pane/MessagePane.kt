package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.VPos
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.objects.Text
import sp.it.util.ui.Util.layScrollVTextCenter
import sp.it.util.ui.lay
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.stackPane

class MessagePane: OverlayPane<String>() {

    private val text: Text

    init {
        text = Text().apply {
            textOrigin = VPos.CENTER
            textAlignment = TextAlignment.CENTER
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