package sp.it.pl.ui.objects.search

import sp.it.pl.main.AppSettings.search as conf
import sp.it.pl.ui.objects.search.SearchAutoCancelable.Match.CONTAINS
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.units.millis
import sp.it.util.units.seconds

/** Search that auto-cancels after given period of time. */
abstract class SearchAutoCancelable: Search() {

   @JvmField protected val searchAutoCanceller = fxTimer(3.seconds, 1) { cancel() }

   public override fun isMatch(text: String, query: String): Boolean = matcher.value.predicate(text, query)

   enum class Match constructor(val predicate: (String, String) -> Boolean) {
      CONTAINS({ text, s -> text.contains(s, isIgnoreCase.value) }),
      STARTS_WITH({ text, s -> text.startsWith(s, isIgnoreCase.value) }),
      ENDS_WITH({ text, s -> text.endsWith(s, isIgnoreCase.value) });
   }

   companion object: GlobalSubConfigDelegator() {
      val cancelQueryDelay by cv(700.millis) def conf.searchDelay
      val isCancelable by cv(true) def conf.searchAutoCancel
      val cancelActivityDelay by cv(3000.millis) def conf.searchAutoCancelDelay
      val matcher by cv(CONTAINS) def conf.searchAlgorithm
      val isIgnoreCase by cv(true) def conf.searchIgnoreCase
   }
}