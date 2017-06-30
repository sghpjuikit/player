package main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import gui.objects.icon.Icon
import gui.objects.textfield.DecoratedTextField
import gui.objects.textfield.autocomplete.ConfigSearch
import javafx.animation.FadeTransition
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.util.Duration.millis
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream

class Search {
    val sources = HashSet<Supplier<Stream<ConfigSearch.Entry>>>()
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
        val fade = FadeTransition(millis(250.0), clearB)
        val tf = DecoratedTextField().apply {
            right.value = clearB
            left.value = Icon<Icon<*>>(FontAwesomeIcon.SEARCH).apply { isMouseTransparent = true }
            clearB.setOnMouseReleased { clear() }
            clearB.managedProperty().bind(editableProperty())
            clearB.visibleProperty().bind(editableProperty())
            textProperty().addListener(object: InvalidationListener {
                override fun invalidated(arg0: Observable) {
                    val text = text
                    val isTextEmpty = text==null || text.isEmpty()
                    val isButtonVisible = fade.node.opacity>0

                    if (isTextEmpty && isButtonVisible) {
                        setButtonVisible(false)
                    } else if (!isTextEmpty && !isButtonVisible) {
                        setButtonVisible(true)
                    }
                }

                private fun setButtonVisible(visible: Boolean) {
                    fade.fromValue = if (visible) 0.0 else 1.0
                    fade.toValue = if (visible) 1.0 else 0.0
                    fade.play()
                }
            })
        }

        ConfigSearch(tf, history, *sources.toTypedArray())
        return tf

    }
}