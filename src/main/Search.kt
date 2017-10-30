package main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import gui.objects.icon.Icon
import gui.objects.textfield.DecoratedTextField
import gui.objects.textfield.autocomplete.ConfigSearch
import javafx.animation.FadeTransition
import javafx.scene.Cursor
import javafx.scene.control.TextField
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.util.Duration.millis
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream

class Search {
    val sources = HashSet<Supplier<Stream<ConfigSearch.Entry>>>()
    val history = ConfigSearch.History()

    fun build(): TextField {
        val tf = DecoratedTextField()
        val clearButton = Region().apply {
            styleClass += "graphic"
        }

        val clearB = StackPane(clearButton).apply {
            styleClass += "clear-button"
            opacity = 0.0
            cursor = Cursor.DEFAULT
            setOnMouseReleased { tf.clear() }
            managedProperty().bind(tf.editableProperty())
            visibleProperty().bind(tf.editableProperty())
        }
        tf.right.value = clearB

        val fade = FadeTransition(millis(250.0), clearB)
        tf.textProperty().addListener { _ ->
            val text = tf.text
            val isTextEmpty = text.isNullOrEmpty()
            val isButtonVisible = fade.node.opacity>0
            fun setButtonVisible(visible: Boolean) {
                fade.fromValue = if (visible) 0.0 else 1.0
                fade.toValue = if (visible) 1.0 else 0.0
                fade.play()
            }

            when {
                isTextEmpty && isButtonVisible -> setButtonVisible(false)
                !isTextEmpty && !isButtonVisible -> setButtonVisible(true)
            }
        }

        tf.left.value = Icon(FontAwesomeIcon.SEARCH)
        tf.left.value.isMouseTransparent = true

        ConfigSearch(tf, history, *sources.toTypedArray())

        return tf
    }
}