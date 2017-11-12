package gui.pane

import gui.objects.Text
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.text.TextAlignment
import util.graphics.Util.layScrollVTextCenter
import util.graphics.Util.layStack
import util.graphics.setMinPrefMaxSize

class MessagePane: OverlayPane<String>() {

    private val text: Text

    init {
        text = Text().apply {
            textOrigin = VPos.CENTER
            textAlignment = TextAlignment.JUSTIFY
            setMinPrefMaxSize(-1.0)
        }

        val textPane = layScrollVTextCenter(text).apply {
            prefWidth = 400.0
            maxWidth = 400.0
        }

        content = layStack(textPane, Pos.CENTER).apply {
            padding = Insets(50.0)
            maxHeight = 200.0
        }
    }

    override fun show(data: String?) {
        text.text = data
        super.show()
    }

}