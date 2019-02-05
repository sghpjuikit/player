package sp.it.pl.main

import javafx.scene.Node
import sp.it.pl.gui.objects.autocomplete.ConfigSearch
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.syncFrom

class Search {
    val sources = HashSet<() -> Sequence<ConfigSearch.Entry>>()
    val history = ConfigSearch.History()

    fun build(onAutoCompleted: (ConfigSearch.Entry) -> Unit = {}): Node {
        val tf = DecoratedTextField().apply {
            val clearB = Icon().also {
                it.styleClass += "search-clear-button"
                it.opacity = 0.0
                it.setOnMouseReleased { clear() }
                it.managedProperty() syncFrom editableProperty()
                it.visibleProperty() syncFrom editableProperty()
            }
            val signB = Icon().also {
                it.styleclass("search-icon-sign")
                it.isMouseTransparent = true
            }
            val fade = anim(200.millis) { clearB.opacity = it }.applyNow()

            styleClass += "search"
            right.value = clearB
            left.value = signB
            textProperty() attach { fade.playFromDir(!it.isNullOrBlank()) }
        }

        ConfigSearch(tf, history, { sources.asSequence().flatMap { it() } }).apply {
            this.hideOnSuggestion.value = true
            this.onAutoCompleted += onAutoCompleted
        }
        return tf
    }
}