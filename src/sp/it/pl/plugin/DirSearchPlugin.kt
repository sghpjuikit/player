package sp.it.pl.plugin

import mu.KLogging
import sp.it.pl.gui.objects.autocomplete.ConfigSearch
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.showAppProgress
import sp.it.pl.util.access.v
import sp.it.pl.util.async.NEW
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cList
import sp.it.pl.util.conf.cr
import sp.it.pl.util.conf.only
import sp.it.pl.util.dev.failIfFxThread
import sp.it.pl.util.file.Util.writeFile
import sp.it.pl.util.system.browse
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DirSearchPlugin: PluginBase("Dir Search", false) {

    @IsConfig(name = "Location", info = "Locations to find directories in.")
    private val searchDirs by cList<File>().only(DIRECTORY)

    @IsConfig(name = "Search depth")
    private val searchDepth = v(2)

    @IsConfig(name = "Re-index", info = "Update cache")
    private val searchDo by cr { updateCache() }

    private val cacheFile = getUserResource("dirs.txt")
    private val cacheUpdate = AtomicLong(0)
    private var searchSourceDirs = listOf<File>()
    private val searchSource = { searchSourceDirs.asSequence().map { it.toOpenDirEntry() } }

    override fun onStart() {
        APP.search.sources += searchSource
        computeFiles()
    }

    override fun onStop() {
        APP.search.sources -= searchSource
    }

    private fun computeFiles() {
        runNew {
            val isCacheInvalid = !cacheFile.exists()
            if (isCacheInvalid) updateCache() else readCache()
        }
    }

    private fun readCache() {
        failIfFxThread()

        val dirs = cacheFile.useLines { it.map { File(it) }.toList() }
        runFX {
            searchSourceDirs = dirs
        }
    }

    private fun updateCache() {
        runFX { searchDirs.materialize() }
            .then(NEW) { dirs ->
                val id = cacheUpdate.getAndIncrement()
                dirs.asSequence()
                        .distinct()
                        .flatMap { findDirectories(it, id) }
                        .toList()
                        .also { writeCache(it) }
            }.ui {
                searchSourceDirs = it
            }
            .showAppProgress("$name: Searching for Directories")
    }

    private fun writeCache(files: List<File>) {
        failIfFxThread()

        val lines = files.asSequence().map { it.absolutePath }.joinToString("\n")
        writeFile(cacheFile, lines)
    }

    private fun File.toOpenDirEntry() = ConfigSearch.Entry.of(
            { "Open directory: $absolutePath" },
            { "Opens directory: $absolutePath" },
            { "Open directory: $absolutePath" },
            { Icon(IconFA.FOLDER) },
            { browse() }
    )

    private fun findDirectories(rootDir: File, id: Long) =
            rootDir.walkTopDown()
                    .onEnter { file -> cacheUpdate.get()==id && file.isDirectory }
                    .onFail { file, e -> logger.warn(e) { "Couldn't not properly read/access file=$file" } }
                    .maxDepth(searchDepth.value)

    companion object: KLogging()

}