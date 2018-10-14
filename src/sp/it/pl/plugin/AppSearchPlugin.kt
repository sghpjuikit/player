package sp.it.pl.plugin

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import mu.KLogging
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.conf.cList
import sp.it.pl.util.conf.cr
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.async.oneCachedThreadExecutor
import sp.it.pl.util.async.runOn
import sp.it.pl.util.async.threadFactory
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.file.endsWithSuffix
import sp.it.pl.util.file.nameWithoutExtensionOrRoot
import sp.it.pl.util.math.seconds
import sp.it.pl.util.system.runAsProgram
import sp.it.pl.util.type.atomic
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File

class AppSearchPlugin: PluginBase("App Search", false) {

    @IsConfig(name = "Location", info = "Locations to find applications in.")
    private val searchDirs by cList<File>().only(DIRECTORY)

    @IsConfig(name = "Search depth")
    private val searchDepth by cv(Int.MAX_VALUE)

    @IsConfig(name = "Re-scan apps")
    private val searchDo by cr { findApps() }

    private val thread by lazy { oneCachedThreadExecutor(seconds(2), threadFactory("appFinder", true)) }
    private var searchSource by atomic(listOf<File>())
    private val searchProvider = { searchSource.asSequence().map { it.toRunApplicationEntry() } }

    override fun onStart() {
        APP.search.sources += searchProvider
        findApps()
    }

    override fun onStop() {
        APP.search.sources -= searchProvider
    }

    private fun findApps() {
        runOn(thread) {
            // TODO: show progress
            searchSource = searchDirs.materialize().asSequence()
                    .distinct()
                    .flatMap { findApps(it) }
                    .toList()
        }
    }

    private fun findApps(dir: File): Sequence<File> {
        return dir.walkTopDown()
                .onFail { file, e -> logger.warn(e) { "Ignoring file=$file. No read/access permission" } }
                .maxDepth(searchDepth.value)
                .filter { it endsWithSuffix "exe" }
    }

    private fun File.toRunApplicationEntry() = ConfigSearch.Entry.of(
            { "Run app: $nameWithoutExtensionOrRoot" },
            { "Runs application: $absolutePath" },
            { "Run app: $absolutePath" },
            { runAsProgram() },
            { Icon(MaterialIcon.APPS) }
    )

    companion object: KLogging()
}