package sp.it.pl.plugin

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import mu.KotlinLogging
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.textfield.autocomplete.ConfigSearch
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Config.VarList
import sp.it.pl.util.conf.Config.VarList.Elements
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.file.nameWithoutExtensionOrRoot
import sp.it.pl.util.system.runAsProgram
import sp.it.pl.util.validation.Constraint
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File

private const val NAME = "App Search"
private const val GROUP = "${Plugin.CONFIG_GROUP}.$NAME"
private val logger = KotlinLogging.logger {}

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
    private val searchProvider = { searchSource.stream().map { it.toRunApplicationEntry() } }

    override fun onStart() {
        findApps()
        APP.search.sources += searchProvider
    }

    override fun onStop() {
        APP.search.sources -= searchProvider
    }

    private fun findApps() {
        searchSource = searchDirs.list.asSequence()
                .distinct()
                .flatMap { findApps(it) }
                .toList()
    }

    private fun findApps(rootDir: File): Sequence<File> {
        return rootDir.walkTopDown()
                .onFail { file, e -> logger.warn(e) { "Ignoring file=$file. No read/access permission" } }
                .maxDepth(searchDepth.value)
                .filter { it.path.endsWith(".exe") }
    }

    private fun File.toRunApplicationEntry() = ConfigSearch.Entry.of(
            { "Run app: $nameWithoutExtensionOrRoot" },
            { "Runs application: $absolutePath" },
            { "Run app: $absolutePath" },
            { runAsProgram() },
            { Icon(MaterialIcon.APPS) }
    )

}