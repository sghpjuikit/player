package plugin

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import gui.objects.icon.Icon
import gui.objects.textfield.autocomplete.ConfigSearch
import main.App
import util.access.v
import util.conf.Config
import util.conf.Config.VarList
import util.conf.Config.VarList.Elements
import util.conf.IsConfig
import util.dev.log
import util.file.Environment
import util.file.nameWithoutExtensionOrRoot
import util.validation.Constraint
import util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.util.function.Supplier

private const val NAME = "App Search"
private const val GROUP = "${Plugin.CONFIG_GROUP}.$NAME"

class AppSearchPlugin: PluginBase(NAME) {

    @Constraint.FileType(DIRECTORY)
    @IsConfig(name = "Location", group = GROUP, info = "Root directories to search through")
    private val searchDirs = VarList(File::class.java, Elements.NOT_NULL)

    @IsConfig(name = "Search depth", group = GROUP)
    private val searchDepth = v(Int.MAX_VALUE)

    @Suppress("unused")
    @IsConfig(name = "Re-scan", group = GROUP)
    private val searchDo = Config.RunnableConfig("rescan", "Rescan apps", GROUP, "", { findApps() })

    private var searchSource: List<File> = emptyList()
    private val searchProvider = Supplier { searchSource.stream().map { it.toRunApplicationEntry() } }

    override fun onStart() {
        findApps()
        App.APP.search.sources.add(searchProvider)
    }

    override fun onStop() {
        App.APP.search.sources.remove(searchProvider)
    }

    private fun findApps() {
        searchSource = searchDirs.list.asSequence()
                .distinct()
                .flatMap { dir -> findApps(dir) }
                .toList()
    }

    private fun findApps(rootDir: File): Sequence<File> {
        return rootDir.walkTopDown()
                .onFail { file, e -> log().warn("Ignoring file={}. No read/access permission", file, e) }
                .maxDepth(searchDepth.value)
                .filter { it.path.endsWith(".exe") }
    }

    private fun File.toRunApplicationEntry() = ConfigSearch.Entry.of(
            { "Run app: $nameWithoutExtensionOrRoot" },
            { "Runs application: $absolutePath" },
            { "Run app: $absolutePath" },
            { Environment.runProgram(this) },
            { Icon(MaterialIcon.APPS) }
    )
}