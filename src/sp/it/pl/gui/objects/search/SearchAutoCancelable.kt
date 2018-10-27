package sp.it.pl.gui.objects.search

import sp.it.pl.gui.objects.search.SearchAutoCancelable.Match.CONTAINS
import sp.it.pl.util.conf.MultiConfigurableBase
import sp.it.pl.main.Settings
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cv
import sp.it.pl.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.math.millis
import sp.it.pl.util.math.seconds

/** Search that auto-cancels after given period of time. */
abstract class SearchAutoCancelable: Search() {

    @JvmField protected var searchTime: Long = -1
    @JvmField protected val searchAutoCanceller = fxTimer(seconds(3), 1) { cancel() }

    public override fun isMatch(text: String, query: String): Boolean {
        val t = if (isIgnoreCase) text.toLowerCase() else text
        val q = if (isIgnoreCase) query.toLowerCase() else query
        return matcher.value.predicate(t, q)
    }

    enum class Match constructor(val predicate: (String, String) -> Boolean) {
        CONTAINS({ text, s -> text.contains(s) }),
        STARTS_WITH({ text, s -> text.startsWith(s) }),
        ENDS_WITH({ text, s -> text.endsWith(s) });
    }

    companion object: MultiConfigurableBase("${Settings.UI}.Search") {

        @IsConfig(name = "Search delay", info = "Maximal time delay between key strokes. Search text is reset after the delay runs out.")
        var cancelQueryDelay by c(millis(500))
        @IsConfig(name = "Search auto-cancel", info = "Deactivates search after period of inactivity.")
        var isCancelable by c(true)
        @IsConfig(name = "Search auto-cancel delay", info = "Period of inactivity after which search is automatically deactivated.")
        var cancelActivityDelay by c(millis(3000))
        @IsConfig(name = "Search algorithm", info = "Algorithm for string matching.")
        val matcher by cv(CONTAINS)
        @IsConfig(name = "Search ignore case", info = "Algorithm for string matching will ignore case.")
        var isIgnoreCase by c(true)
    }
}