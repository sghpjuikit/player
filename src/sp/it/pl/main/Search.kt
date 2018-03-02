package sp.it.pl.main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.animation.FadeTransition
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.attach
import kotlin.streams.asStream

class Search {
    val sources = HashSet<() -> Sequence<ConfigSearch.Entry>>()
    val history = ConfigSearch.History()

    fun build(): Node {
        val clearButton = Region().apply {
            styleClass += "graphic"
        }
        val clearB = StackPane(clearButton).apply {
            styleClass += "clear-button"
            opacity = 0.0
            cursor = Cursor.DEFAULT
        }
        val fade = FadeTransition(millis(250), clearB)
        val tf = DecoratedTextField().apply {
            right.value = clearB
            left.value = Icon(FontAwesomeIcon.SEARCH).apply { isMouseTransparent = true }
            clearB.setOnMouseReleased { clear() }
            clearB.managedProperty().bind(editableProperty())
            clearB.visibleProperty().bind(editableProperty())
            textProperty() attach {
                val isTextEmpty = it==null || it.isEmpty()
                val isButtonVisible = fade.node.opacity>0
                fun setButtonVisible(visible: Boolean) {
                    fade.fromValue = if (visible) 0.0 else 1.0
                    fade.toValue = if (visible) 1.0 else 0.0
                    fade.play()
                }

                if (isTextEmpty && isButtonVisible) {
                    setButtonVisible(false)
                } else if (!isTextEmpty && !isButtonVisible) {
                    setButtonVisible(true)
                }
            }
        }

        ConfigSearch(tf, history, { sources.stream().flatMap { it().asStream() } })
        return tf
    }
}