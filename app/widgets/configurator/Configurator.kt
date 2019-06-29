package configurator

import javafx.fxml.FXML
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
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
import sp.it.pl.layout.widget.controller.fxmlLoaderForController
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.scaleEM
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.recurse
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.ui.expandToRootAndSelect
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import java.util.ArrayList

@Suppress("MemberVisibilityCanBePrivate")
@Widget.Info(
   author = "Martin Polakovic",
   name = "Settings",
   description = "Provides access to application settings",
   howto = "Available actions:\n"
      + "    Select category\n"
      + "    Change setting value: Automatically takes change\n"
      + "    Default : Set default value for this setting\n",
   notes = "To do: generate active widget settings",
   version = "0.9.0",
   year = "2016",
   group = Widget.Group.APP
)
class Configurator(widget: Widget): SimpleController(widget), ConfiguringFeature {

   private val inputValue = io.i.create<Configurable<Any>>("To configure") { configure(it) }
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
      root.prefSize = 800.scaleEM() x 600.scaleEM()
      root.consumeScrolling()


      fxmlLoaderForController(this).loadNoEx<Any>()

      configsRootPane.layFullArea += configsPane

      root.lay(TOP_LEFT) += hBox(10, TOP_RIGHT) {
         isPickOnBounds = false

         lay += Icon(IconFA.RECYCLE, 13.0, "Set all to default").onClickDo { defaults() }
         lay += Icon().blank()
         lay += Icon(IconFA.HOME, 13.0, "App settings").onClickDo { configure(appConfigurable) }
         lay += Icon().blank()
      }

      groups.selectionModel.selectionMode = SINGLE
      groups.cellFactory = Callback { buildTreeCell(it) }
      groups.propagateESCAPE()
      groups.selectionModel.selectedItemProperty() attach { storeAppSettingsSelection(it) } on onClose
      groups.selectionModel.selectedItemProperty() attach { showConfigs(it?.value) } on onClose

      refresh()
   }

   /** Set and apply values. */
   fun apply() = configsPane.getConfigFields().forEach { it.apply() }

   /** Set default app settings. */
   fun defaults() = configs.forEach { it.value = it.defaultValue }

   fun refresh() {
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
      appConfigurable.rawAdd(configSelectionName, PropVal1(selectedGroupPath))
   }

   private fun restoreAppSettingsSelection(): TreeItem<Name>? {
      if (!showsAppSettings) return null
      val selectedGroupPath = appConfigurable.rawGetAll()[configSelectionName]?.val1
      return groups.root.recurse { it.children }.find { it.value.pathUp==selectedGroupPath; }
   }

}