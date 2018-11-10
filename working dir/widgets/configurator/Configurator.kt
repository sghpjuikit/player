package configurator

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.HOME
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.RECYCLE
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.util.Callback
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.tree.Name
import sp.it.pl.gui.objects.tree.buildTreeCell
import sp.it.pl.gui.objects.tree.tree
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.main.APP
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.c
import sp.it.pl.util.functional.seqRec
import sp.it.pl.util.graphics.expandAndSelect
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.graphics.propagateESCAPE
import sp.it.pl.util.reactive.attach
import java.util.ArrayList

@Suppress("MemberVisibilityCanBePrivate")
@Widget.Info(
        author = "Martin Polakovic",
        name = "Settings",
        description = "Provides access to application settings",
        howto = "Available actions:\n"
                +"    Select category\n"
                +"    Change setting value: Automatically takes change\n"
                +"    Default : Set default value for this setting\n",
        notes = "To do: generate active widget settings",
        version = "1",
        year = "2016",
        group = Widget.Group.APP
)
class Configurator(widget: Widget<*>): SimpleController(widget), ConfiguringFeature<Any> {

    @FXML lateinit var groups: TreeView<Name>
    @FXML lateinit var controls: Pane
    @FXML lateinit var configsRootPane: AnchorPane
    private val configsPane = ConfigPane<Any>()
    private val configs = ArrayList<Config<*>>()
    private val configSelectionName = "app.settings.selected_group"
    private var configSelectionAvoid = false

    @IsConfig(editable = EditMode.APP)
    var showsAppSettings by c(true)

    init {
        inputs.create<Configurable<out Any>>("To configure", { configure(it) })

        ConventionFxmlLoader(this).loadNoEx<Any>()

        configsRootPane.children += configsPane
        groups.isShowRoot = false
        groups.selectionModel.selectionMode = SINGLE
        groups.cellFactory = Callback { buildTreeCell(it) }
        groups.propagateESCAPE()
        onClose += groups.selectionModel.selectedItemProperty() attach { nv ->
            storeAppSettingsSelection(nv)
            showConfigs(configs.filter { it.group==nv?.value?.pathUp })
        }

        controls.children += listOf(
                Icon(HOME, 13.0, "App settings", Runnable { showsAppSettings = true; configure(APP.configuration.getFields()) }),
                Label("    "),
                Icon(REFRESH, 13.0, "Refresh all", Runnable { refresh() }),
                Icon(RECYCLE, 13.0, "Set all to default", Runnable { defaults() })
        )
        onScroll = EventHandler { it.consume() }
    }

    /** Set and apply values. */
    fun apply() = configsPane.getConfigFields().forEach { it.apply() }

    /** Set default app settings. */
    fun defaults() = configsPane.getConfigFields().forEach { it.setNapplyDefault() }

    override fun refresh() {
        if (showsAppSettings) configure(APP.configuration.getFields())
        else refreshConfigs()
    }

    override fun configure(configurable: Collection<Config<out Any>>) {
        configs.clear()
        configs += configurable
        configSelectionAvoid = true
        groups.root = tree(Name.treeOfPaths("Groups", configurable.map { it.group }))
        groups.root.isExpanded = true
        configSelectionAvoid = false
        restoreAppSettingsSelection()
    }

    private fun showConfigs(configs: Collection<Config<*>>) = configsPane.configure(configs)

    private fun refreshConfigs() = configsPane.getConfigFields().forEach { it.refreshItem() }

    private fun storeAppSettingsSelection(item: TreeItem<Name>?) {
        if (!showsAppSettings || configSelectionAvoid) return
        val selectedGroupPath = item?.value?.pathUp ?: ""
        APP.configuration.rawAdd(configSelectionName, selectedGroupPath)
    }

    private fun restoreAppSettingsSelection() {
        if (!showsAppSettings) return
        val path = APP.configuration.rawGetAll()[configSelectionName]
        val item = groups.root.seqRec { it.children }.find { it.value.pathUp==path; } ?: groups.root
        groups.expandAndSelect(item)    // invokes showConfigs()
    }

}