package configurator

import javafx.geometry.Insets
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.SplitPane
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.StackPane
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
import sp.it.pl.main.emScaled
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.EditMode
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.toListConfigurable
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.recurse
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.ui.expandToRootAndSelect
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.leftAnchor
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.splitPane
import sp.it.util.ui.stackPane
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

   private val groups = TreeView<Name>()
   private val configsRoot = StackPane()
   private val configsPane = ConfigPane<Any>()
   private val subroot = stackPane {
      lay += splitPane {
         setDividerPositions(0.25)

         lay(false) += groups.apply {
            id = "groups"
            prefWidth = 200.0
            leftAnchor
            SplitPane.setResizableWithParent(this, false)
         }
         lay(true) += stackPane {
            id = "configs"
            padding = Insets(10.0)

            lay += scrollPane {
               isFitToWidth = true
               prefSize = -1 x -1
               vbarPolicy = ScrollBarPolicy.AS_NEEDED

               content = configsRoot.apply {
                  prefSize = -1 x -1
                  isFocusTraversable = true

                  lay += configsPane
               }
            }
         }
      }
   }
   private val configs = ArrayList<Config<*>>()
   private val configSelectionName = "app.settings.selected_group"
   private var configSelectionAvoid = false
   private val appConfigurable = APP.configuration

   val showsAppSettings by cv(true).def(editable = EditMode.APP)
   val selectedGroupPath by cv("").def(editable = EditMode.APP)

   init {
      root.prefSize = 800.emScaled x 600.emScaled
      root.consumeScrolling()

      root.lay += subroot
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
      if (showsAppSettings.value) configure(appConfigurable)
      else refreshConfigs()
   }

   override fun configure(configurable: Configurable<*>?, groupToSelect: String?) {
      val configurableFields = configurable?.getFields().orEmpty()

      showsAppSettings.value = configurable==appConfigurable
      configs setTo configurableFields
      configSelectionAvoid = true
      groups.isShowRoot = !showsAppSettings.value
      groups.root = tree(Name.treeOfPaths("All", configurableFields.map { it.group }))
      groups.root.isExpanded = true
      configSelectionAvoid = false
      storeAppSettingsSelection(groupToSelect)
      groups.expandToRootAndSelect(restoreAppSettingsSelection() ?: groups.root)
   }

   private fun showConfigs(group: Name?) = configsPane.configure(
      configs.filter { it.group==group?.pathUp }.toListConfigurable()
   )

   private fun refreshConfigs() = configsPane.getConfigFields().forEach { it.refreshItem() }

   private fun storeAppSettingsSelection(selection: String?) {
      if (configSelectionAvoid) return
      selectedGroupPath.value = selection ?: ""
      if (showsAppSettings.value && selection!=null) appConfigurable.rawAdd(configSelectionName, PropVal1(selection))
   }

   private fun storeAppSettingsSelection(item: TreeItem<Name>?) = storeAppSettingsSelection(item?.value?.pathUp)

   private fun restoreAppSettingsSelection(): TreeItem<Name>? {
      val selection = if (showsAppSettings.value) appConfigurable.rawGetAll()[configSelectionName]?.val1 else selectedGroupPath.value
      return groups.root.recurse { it.children }.find { it.value.pathUp==selection }
   }

}