package sp.it.pl.main

import javafx.scene.Node
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import sp.it.pl.core.NameUi
import sp.it.pl.gui.objects.autocomplete.ConfigSearch
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.reactive.attach
import sp.it.util.reactive.map
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.syncFrom
import sp.it.util.units.millis
import sp.it.pl.main.AppSettings.search as conf

private typealias Src = () -> Sequence<ConfigSearch.Entry>

class AppSearch: GlobalSubConfigDelegator(conf.name) {
   val sources by cList<Source>().def(conf.sources).noPersist()
   val history = ConfigSearch.History()


   fun buildUi(onAutoCompleted: (ConfigSearch.Entry) -> Unit = {}): Node {
      val tf = DecoratedTextField().apply {
         styleClass += "search"
         val isEmpty = textProperty().map { it.isNullOrBlank() }

         right.value = Icon().also { i ->
            i.styleClass += "search-clear-button"
            i.opacity = 0.0
            i.onEventDown(MOUSE_RELEASED) { clear() }
            i.managedProperty() syncFrom editableProperty()
            i.visibleProperty() syncFrom editableProperty()
            i.disableProperty() syncFrom isEmpty

            val fade = anim(200.millis) { i.opacity = it }.applyNow()
            isEmpty attach { fade.playFromDir(!it) }
         }
         left.value = Icon().also {
            it.styleclass("search-icon-sign")
            it.isMouseTransparent = true
            it.isFocusTraversable = false
         }
      }

      ConfigSearch(tf, history) { sources.asSequence().flatMap { it() } }.apply {
         this.hideOnSuggestion.value = true
         this.onAutoCompleted += onAutoCompleted
      }

      return tf
   }

   class Source(override val nameUi: String, source: Src): Src by source, NameUi
}