package sp.it.pl.plugin

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import mu.KLogging
import sp.it.pl.audio.Player
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.cList
import sp.it.pl.main.cr
import sp.it.pl.main.only
import sp.it.pl.util.access.v
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.file.Util.writeFile
import sp.it.pl.util.system.browse
import sp.it.pl.util.type.atomic
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Function

class DirSearchPlugin: PluginBase("Dir Search", false) {

    @IsConfig(name = "Location", info = "Locations to find directories in.")
    private val searchDirs by cList<File>().only(DIRECTORY)

    @IsConfig(name = "Search depth")
    private val searchDepth = v(2)

    @IsConfig(name = "Re-index", info = "Update cache")
    private val searchDo by cr { updateCache() }

    private val cacheFile = getUserResource("dirs.txt")
    private val cacheUpdate = AtomicLong(0)
    private var dirs by atomic(listOf<File>())
    private val searchProvider = { dirs.asSequence().map { it.toOpenDirEntry() } }

    override fun onStart() {
        computeFiles()
        APP.search.sources += searchProvider
    }

    override fun onStop() {
        APP.search.sources -= searchProvider
    }

    private fun computeFiles() {
        val cacheFileExists = cacheFile.exists()
        val isCacheInvalid = !cacheFileExists

        if (isCacheInvalid) updateCache()
        else readCache()
    }

    private fun readCache() {
        dirs = cacheFile.useLines { it.map { File(it) }.toList() }
    }

    private fun writeCache(files: List<File>) {
        val lines = files.asSequence().map { it.absolutePath }.joinToString("\n")
        writeFile(cacheFile, lines)
    }

    private fun updateCache() {
        val id = cacheUpdate.getAndIncrement()
        Fut.fut(searchDirs.materialize())
                .map(Player.IO_THREAD, Function<List<File>, List<File>> {
                    it.asSequence()
                            .distinct()
                            .flatMap { dir -> findDirectories(dir, id) }
                            .toList()
                })
                .use(Player.IO_THREAD, Consumer { writeCache(it) })
                .use(FX, Consumer { dirs = it })
                .showProgressOnActiveWindow()
    }

    private fun File.toOpenDirEntry() = ConfigSearch.Entry.of(
            { "Open directory: $absolutePath" },
            { "Opens directory: $absolutePath" },
            { "Open directory: $absolutePath" },
            { browse() },
            { Icon(FontAwesomeIcon.FOLDER) }
    )

    private fun findDirectories(rootDir: File, id: Long) =
            rootDir.walkTopDown()
                    .onEnter { file -> cacheUpdate.get()==id && file.isDirectory }
                    .onFail { file, e -> logger.warn(e) { "Couldn't not properly read/access file=$file" } }
                    .maxDepth(searchDepth.value)

    companion object: KLogging()
}