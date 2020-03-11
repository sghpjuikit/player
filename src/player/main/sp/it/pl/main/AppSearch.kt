package sp.it.pl.main

import javafx.scene.Node
import sp.it.pl.core.NameUi
import sp.it.pl.ui.objects.autocomplete.ConfigSearch
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.pl.main.AppSettings.search as conf

private typealias Src = () -> Sequence<ConfigSearch.Entry>

class AppSearch: GlobalSubConfigDelegator(conf.name) {
   val sources by cList<Source>().def(conf.sources).noPersist().butElement(UiConverter<Source> { it.nameUi })
   val history = ConfigSearch.History()

   fun buildUi(onAutoCompleted: (ConfigSearch.Entry) -> Unit = {}): Node {
      val tf = searchTextField()

      ConfigSearch(tf, history) { sources.asSequence().flatMap { it() } }.apply {
         this.hideOnSuggestion.value = true
         this.onAutoCompleted += onAutoCompleted
      }

      return tf
   }

   class Source(override val nameUi: String, source: Src): Src by source, NameUi
}