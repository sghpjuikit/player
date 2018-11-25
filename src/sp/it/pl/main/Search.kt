package sp.it.pl.main

import javafx.scene.Node
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.attach
import kotlin.streams.asStream

class Search {
    val sources = HashSet<() -> Sequence<ConfigSearch.Entry>>()
    val history = ConfigSearch.History()

    fun build(): Node {
        val tf = DecoratedTextField().apply {
            val clearB = Icon().also {
                it.styleClass += "search-clear-button"
                it.opacity = 0.0
                it.setOnMouseReleased { clear() }
                it.managedProperty().bind(editableProperty())
                it.visibleProperty().bind(editableProperty())
            }
            val signB = Icon().also {
                it.styleclass("search-icon-sign")
                it.isMouseTransparent = true
            }
            val fade = anim(millis(200)) { clearB.opacity = it }.applyNow()

            styleClass += "search"
            right.value = clearB
            left.value = signB
            textProperty() attach { fade.playFromDir(!it.isNullOrBlank()) }
        }

        ConfigSearch(tf, history, { sources.stream().flatMap { it().asStream() } }).apply {
            hideOnSuggestion.value = true
        }
        return tf
    }
}