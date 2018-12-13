package sp.it.pl.plugin

import mu.KLogging
import sp.it.pl.gui.objects.autocomplete.ConfigSearch
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.APP
import sp.it.pl.main.IconMA
import sp.it.pl.main.showAppProgress
import sp.it.pl.util.async.runNew
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cList
import sp.it.pl.util.conf.cr
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.nameWithoutExtensionOrRoot
import sp.it.pl.util.system.runAsProgram
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File

class AppSearchPlugin: PluginBase("App Search", false) {

    @IsConfig(name = "Location", info = "Locations to find applications in.")
    private val searchDirs by cList<File>().only(DIRECTORY)

    @IsConfig(name = "Search depth")
    private val searchDepth by cv(Int.MAX_VALUE)

    @IsConfig(name = "Re-scan apps")
    private val searchDo by cr { findApps() }

    private var searchSourceApps = listOf<File>()
    private val searchSource = { searchSourceApps.asSequence().map { it.toRunApplicationEntry() } }

    override fun onStart() {
        APP.search.sources += searchSource
        findApps()
    }

    override fun onStop() {
        APP.search.sources -= searchSource
    }

    private fun findApps() {
        val dirs = searchDirs.materialize()
        runNew {
            dirs.asSequence()
                    .distinct()
                    .flatMap { findApps(it) }
                    .toList()
        }.ui {
            searchSourceApps = it
        }
        .showAppProgress("$name: Searching for applications")
    }

    private fun findApps(dir: File): Sequence<File> {
        return dir.walkTopDown()
                .onFail { file, e -> logger.warn(e) { "Ignoring file=$file. No read/access permission" } }
                .maxDepth(searchDepth.value)
                .filter { it hasExtension "exe" }
    }

    private fun File.toRunApplicationEntry() = ConfigSearch.Entry.of(
            { "Run app: $nameWithoutExtensionOrRoot" },
            { "Runs application: $absolutePath" },
            { "Run app: $absolutePath" },
            { Icon(IconMA.APPS) },
            { runAsProgram() }
    )

    companion object: KLogging()
}