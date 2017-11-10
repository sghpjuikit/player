package plugin

import audio.Player
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import gui.objects.icon.Icon
import gui.objects.textfield.autocomplete.ConfigSearch
import javafx.collections.ObservableList
import main.App
import mu.KotlinLogging
import util.access.v
import util.async.Async.FX
import util.async.future.Fut
import util.conf.Config.RunnableConfig
import util.conf.Config.VarList
import util.conf.Config.VarList.Elements
import util.conf.IsConfig
import util.file.Util.readFileLines
import util.file.Util.writeFile
import util.system.Environment
import util.validation.Constraint
import util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.asSequence

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
        dirs = readFileLines(cacheFile).asSequence()
                .map {
                    try {
                        Paths.get(it).toFile()
                    } catch (e: Exception) {
                        logger.warn(e) { "Illegal path value in plugin cache" }
                        null
                    }
                }
                .filterNotNull()
                .toList()
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
            { Environment.browse(this) },
            { Icon(FontAwesomeIcon.FOLDER) }
    )

    private fun findDirectories(rootDir: File, id: Long) =
        rootDir.walkTopDown()
                .onEnter { file -> cacheUpdate.get()==id && file.isDirectory }
                .onFail { file, e -> logger.warn(e) { "Couldn't not properly read/access file=$file" } }
                .maxDepth(searchDepth.value)

}