package sp.it.pl.plugin

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.collections.ObservableList
import mu.KotlinLogging
import sp.it.pl.audio.Player
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.main.App
import sp.it.pl.util.access.v
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.conf.Config.RunnableConfig
import sp.it.pl.util.conf.Config.VarList
import sp.it.pl.util.conf.Config.VarList.Elements
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.file.Util.writeFile
import sp.it.pl.util.system.browse
import sp.it.pl.util.validation.Constraint
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

private const val NAME = "Dir Search"
private const val GROUP = "${Plugin.CONFIG_GROUP}.$NAME"
private val logger = KotlinLogging.logger {}

class DirSearchPlugin: PluginBase(NAME) {

    @Constraint.FileType(DIRECTORY)
    @IsConfig(name = "Location", group = GROUP, info = "Root directory the contents of to display "
            +"This is not a file system browser, and it is not possible to visit parent of this directory.")
    private val searchDirs = VarList(File::class.java, Elements.NOT_NULL)

    @IsConfig(name = "Search depth", group = GROUP)
    private val searchDepth = v(2)

    @Suppress("unused")
    @IsConfig(name = "Re-index", group = GROUP)
    private val searchDo = RunnableConfig("reindex", "Update cache", GROUP, "", { updateCache() })

    private val cacheFile = getUserResource("dirs.txt")
    private val cacheUpdate = AtomicLong(0)

    private var dirs: List<File> = emptyList()
    private val searchProvider = Supplier { dirs.stream().map { it.toOpenDirEntry() } }

    override fun onStart() {
        computeFiles()
        App.APP.search.sources += searchProvider
    }

    override fun onStop() {
        App.APP.search.sources -= searchProvider
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
        Fut.fut(searchDirs.list)
                .map(Player.IO_THREAD, Function<ObservableList<File>, List<File>> {
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

}