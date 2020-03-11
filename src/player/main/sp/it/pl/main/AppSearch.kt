package sp.it.pl.main

import javafx.collections.ObservableList
import javafx.scene.Node
import sp.it.pl.core.NameUi
import sp.it.pl.ui.objects.autocomplete.ConfigSearch
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry.SimpleEntry
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.History
import sp.it.util.collections.materialize
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.reactive.onChangeAndNow
import sp.it.pl.main.AppSettings.search as conf

class AppSearch: GlobalSubConfigDelegator(conf.name) {
   val sources by cList<Source>().def(conf.sources).noPersist().butElement(UiConverter<Source> { it.nameUi })
   val history = History()
   @Volatile private var sourcesMaterialized: List<Source> = listOf()

   init {
      sources.onChangeAndNow { sourcesMaterialized = sources.materialize() }
   }

   fun buildUi(onAutoCompleted: (Entry) -> Unit = {}): Node {
      val tf = searchTextField()
      ConfigSearch(
         tf,
         history,
         { text ->
            when {
               text.isEmpty() -> listOf()
               else -> {
                  val maxResults = 50
                  val phrases = text.split(" ").toList()
                  val results = sourcesMaterialized.asSequence().flatMap { it.source(phrases) }.take(maxResults + 1).toList()
                  if (results.size<maxResults + 1) results
                  else results.dropLast(1) + SimpleEntry("more items...", null, { "" }, {})
               }
            }
         }
      ).apply {
         this.hideOnSuggestion.value = true
         this.onAutoCompleted += onAutoCompleted
      }

      return tf
   }

   class Source private constructor(override val nameUi: String, val source: (List<String>) -> Sequence<Entry>): NameUi {
      companion object {
         operator fun <T> invoke(nameUi: String, source: ObservableList<T>) = SourceDef(nameUi) { source.materialize().asSequence() }
         operator fun <T> invoke(nameUi: String, source: () -> Sequence<T>) = SourceDef(nameUi, source)
         fun complete(nameUi: String, source: (List<String>) -> Sequence<Entry>) = Source(nameUi, source)
      }
   }

   class SourceDef<T>(val nameUi: String, val source: () -> Sequence<T>) {
      infix fun by(searchText: (T) -> String) = SourceDef2(nameUi, source, searchText)
   }

   class SourceDef2<T>(val nameUi: String, val source: () -> Sequence<T>, val searchText: (T) -> String) {
      infix fun toSource(entryBuilder: (T) -> Entry) = Source.complete(nameUi) { phrases ->
         source()
            .filter { phrases.all { phrase -> searchText(it).contains(phrase, true) } }
            .map(entryBuilder)
      }
   }
}