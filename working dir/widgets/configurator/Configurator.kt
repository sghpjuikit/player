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
import sp.it.pl.layout.widget.controller.ClassController
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.functional.seqRec
import sp.it.pl.util.graphics.expandAndSelect
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.reactive.attach
import java.util.*

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
class Configurator: ClassController(), ConfiguringFeature {

    @FXML lateinit var groups: TreeView<Name>
    @FXML lateinit var controls: Pane
    @FXML lateinit var configsRootPane: AnchorPane
    private val configsPane = ConfigPane<Any>()
    private val configs = ArrayList<Config<*>>()
    private val configSelectionName = "app.settings.selected_group"
    private var configSelectionAvoid = false

    @IsConfig(editable = IsConfig.EditMode.APP)
    @JvmField var showsAppSettings = true

    init {
        inputs.create("To configure", Configurable::class.java, { configure(it) })

        ConventionFxmlLoader(this).loadNoEx<Any>()

        configsRootPane.children += configsPane
        groups.selectionModel.selectionMode = SINGLE
        groups.cellFactory = Callback { buildTreeCell(it) }
        onClose += groups.selectionModel.selectedItemProperty() attach { nv ->
            storeAppSettingsSelection(nv)
            showConfigs(configs.filter { it.group==nv?.value?.pathUp })
        }

        controls.children += listOf(
                Icon(HOME, 13.0, "AppUtil settings", Runnable { showsAppSettings = true; configure(APP.configuration.fields) }),
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
        if (showsAppSettings) configure(APP.configuration.fields)
        else refreshConfigs()
    }

    override fun configure(c: Collection<Config<*>>?) {
        if (c==null) return
        configs.clear()
        configs += c
        configSelectionAvoid = true
        groups.root = tree(Name.treeOfPaths("Groups", c.map { it.group }))
        groups.root.isExpanded = true
        configSelectionAvoid = false
        restoreAppSettingsSelection()
    }

    private fun showConfigs(configs: Collection<Config<*>>) = configsPane.configure(configs)

    private fun refreshConfigs() = configsPane.getConfigFields().forEach { it.refreshItem() }

    private fun storeAppSettingsSelection(item: TreeItem<Name>?) {
        if (!showsAppSettings || configSelectionAvoid) return
        val selectedGroupPath = item?.value?.pathUp ?: ""
        APP.configuration.rawAddProperty(configSelectionName, selectedGroupPath)
    }

    private fun restoreAppSettingsSelection() {
        if (!showsAppSettings) return
        val path = APP.configuration.rawGet()[configSelectionName]
        val item = groups.root.seqRec { it.children }.find { it.value.pathUp==path; } ?: groups.root
        groups.expandAndSelect(item)    // invokes showConfigs()
    }

}