package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.VPos
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.objects.Text
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.IconFA
import sp.it.util.access.toggleEnum
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.sync
import sp.it.util.ui.Util.layScrollVTextCenter
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.stackPane

class MessagePane: OverlayPane<String>() {

    private val text: Text
    private val history = ArrayList<String>()
    private var historyAt = -1

    init {
        text = Text().apply {
            textOrigin = VPos.CENTER
            textAlignment = TextAlignment.CENTER
            setMinPrefMaxSize(-1.0)
        }

        content = stackPane {
            padding = Insets(50.0)
            prefHeight = 200.0
            minHeight = 200.0
            maxHeight = 200.0
            lay(CENTER) += layScrollVTextCenter(text).apply {
                prefWidth = 400.0
                maxWidth = 400.0
            }
            lay(TOP_RIGHT) += hBox(5.0, TOP_RIGHT) {
                isPickOnBounds = false

                lay+= Icon(IconFA.ANGLE_LEFT, -1.0, "Previous message").onClickDo { visitLeft() }
                lay+= Icon(IconFA.ANGLE_RIGHT, -1.0, "Next message").onClickDo { visitRight() }
                lay += Icon(null, -1.0, "Toggle text alignment").apply {
                    text.textAlignmentProperty() sync {
                        val glyph = when (it) {
                            TextAlignment.CENTER -> IconFA.ALIGN_CENTER
                            TextAlignment.JUSTIFY -> IconFA.ALIGN_JUSTIFY
                            TextAlignment.RIGHT -> IconFA.ALIGN_RIGHT
                            TextAlignment.LEFT -> IconFA.ALIGN_LEFT
                        }
                        icon(glyph)
                    }
                    onClickDo {
                        text.textAlignmentProperty().toggleEnum()
                    }
                }
                lay += supplyIf(display.value!=Display.WINDOW) {
                    Icon(IconFA.SQUARE, -1.0, "Always on top\n\nForbid hiding this window behind other application windows").apply{
                        onClickDo {
                            stage?.let {
                                it.isAlwaysOnTop = !it.isAlwaysOnTop
                                icon(if (it.isAlwaysOnTop) IconFA.SQUARE else IconFA.SQUARE_ALT)
                            }
                        }
                    }
                }
            }
        }
        makeResizableByUser()
    }

    override fun show(data: String?) {
        if (data!=null) history += data
        visit(history.lastIndex)
        super.show()
    }

    private fun visit(at: Int) {
        historyAt = at
        text.text = history[historyAt]
    }

    private fun visitLeft() = visit(historyAt.coerceAtLeast(1)-1)

    private fun visitRight() = visit(historyAt.coerceAtMost(history.size-2)+1)

}