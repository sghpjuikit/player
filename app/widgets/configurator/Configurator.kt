package configurator

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Pos.CENTER
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
import sp.it.pl.main.IconFA
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.c
import sp.it.pl.util.functional.recurse
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.expandToRootAndSelect
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.propagateESCAPE
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.on
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
class Configurator(widget: Widget<*>): SimpleController(widget), ConfiguringFeature {

    @FXML private lateinit var groups: TreeView<Name>
    @FXML private lateinit var controls: Pane
    @FXML private lateinit var configsRootPane: AnchorPane
    private val configsPane = ConfigPane<Any>()
    private val configs = ArrayList<Config<*>>()
    private val configSelectionName = "app.settings.selected_group"
    private var configSelectionAvoid = false
    private val appConfigurable = APP.configuration

    @IsConfig(editable = EditMode.APP)
    var showsAppSettings by c(true)

    init {
        inputs.create<Configurable<out Any>>("To configure", { configure(it) })

        ConventionFxmlLoader(this).loadNoEx<Any>()
        configsRootPane.children += configsPane
        lay(0, 0, null, null) += hBox(10, CENTER) {
            lay += Icon(IconFA.RECYCLE, 13.0, "Set all to default").onClickDo { defaults() }
            lay += Icon(IconFA.REFRESH, 13.0, "Refresh all").onClickDo { refresh() }
            lay += Icon().blank()
            lay += Icon(IconFA.HOME, 13.0, "App settings").onClickDo { configure(appConfigurable) }
            lay += Icon().blank()
        }

        groups.selectionModel.selectionMode = SINGLE
        groups.cellFactory = Callback { buildTreeCell(it) }
        groups.propagateESCAPE()
        groups.selectionModel.selectedItemProperty() attach { storeAppSettingsSelection(it) } on onClose
        groups.selectionModel.selectedItemProperty() attach { showConfigs(it?.value) } on onClose
        onScroll = EventHandler { it.consume() }

        refresh()
    }

    /** Set and apply values. */
    fun apply() = configsPane.getConfigFields().forEach { it.apply() }

    /** Set default app settings. */
    fun defaults() = configsPane.getConfigFields().forEach { it.setNapplyDefault() }

    override fun refresh() {
        if (showsAppSettings) configure(appConfigurable)
        else refreshConfigs()
    }

    override fun configure(configurable: Configurable<*>?) {
        val configurableFields = configurable?.fields.orEmpty()

        showsAppSettings = configurable==appConfigurable
        configs setTo configurableFields
        configSelectionAvoid = true
        groups.isShowRoot = !showsAppSettings
        groups.root = tree(Name.treeOfPaths("All", configurableFields.map { it.group }))
        groups.root.isExpanded = true
        configSelectionAvoid = false
        groups.expandToRootAndSelect(restoreAppSettingsSelection() ?: groups.root)
    }

    private fun showConfigs(group: Name?) = showConfigs(configs.filter { c -> c.group==group?.pathUp })

    private fun showConfigs(configs: Collection<Config<*>>) = configsPane.configure(configs)

    private fun refreshConfigs() = configsPane.getConfigFields().forEach { it.refreshItem() }

    private fun storeAppSettingsSelection(item: TreeItem<Name>?) {
        if (!showsAppSettings || configSelectionAvoid) return
        val selectedGroupPath = item?.value?.pathUp ?: ""
        appConfigurable.rawAdd(configSelectionName, selectedGroupPath)
    }

    private fun restoreAppSettingsSelection(): TreeItem<Name>? {
        if (!showsAppSettings) return null
        val selectedGroupPath = appConfigurable.rawGetAll()[configSelectionName]
        return groups.root.recurse { it.children }.find { it.value.pathUp==selectedGroupPath; }
    }

}