package sp.it.pl.gui.objects.search

import sp.it.pl.gui.objects.search.SearchAutoCancelable.Match.CONTAINS
import sp.it.pl.main.Settings
import sp.it.pl.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.MultiConfigurableBase
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cv
import sp.it.pl.util.units.millis
import sp.it.pl.util.units.seconds

/** Search that auto-cancels after given period of time. */
abstract class SearchAutoCancelable: Search() {

    @JvmField protected var searchTime: Long = -1
    @JvmField protected val searchAutoCanceller = fxTimer(3.seconds, 1) { cancel() }

    public override fun isMatch(text: String, query: String): Boolean =  matcher.value.predicate(text, query)

    enum class Match constructor(val predicate: (String, String) -> Boolean) {
        CONTAINS({ text, s -> text.contains(s, isIgnoreCase) }),
        STARTS_WITH({ text, s -> text.startsWith(s, isIgnoreCase) }),
        ENDS_WITH({ text, s -> text.endsWith(s, isIgnoreCase) });
    }

    companion object: MultiConfigurableBase("${Settings.UI}.Search") {

        @IsConfig(name = "Search delay", info = "Maximal time delay between key strokes. Search text is reset after the delay runs out.")
        var cancelQueryDelay by c(500.millis)
        @IsConfig(name = "Search auto-cancel", info = "Deactivates search after period of inactivity.")
        var isCancelable by c(true)
        @IsConfig(name = "Search auto-cancel delay", info = "Period of inactivity after which search is automatically deactivated.")
        var cancelActivityDelay by c(3000.millis)
        @IsConfig(name = "Search algorithm", info = "Algorithm for string matching.")
        val matcher by cv(CONTAINS)
        @IsConfig(name = "Search ignore case", info = "Algorithm for string matching will ignore case.")
        var isIgnoreCase by c(true)

    }
}