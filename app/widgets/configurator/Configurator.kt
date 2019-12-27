package configurator

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.scene.layout.StackPane
import javafx.util.Callback
import sp.it.pl.gui.objects.icon.CheckIcon
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
import sp.it.pl.main.searchTextField
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.c
import sp.it.util.conf.noUi
import sp.it.util.conf.toListConfigurable
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.net
import sp.it.util.functional.recurse
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.text.nullIfBlank
import sp.it.util.ui.expandToRootAndSelect
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.leftAnchor
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.splitPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
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
   version = "1.0.0",
   year = "2016",
   group = Widget.Group.APP
)
class Configurator(widget: Widget): SimpleController(widget), ConfiguringFeature {

   private val inputValue = io.i.create<Configurable<Any>>("To configure") { configure(it) }

   private val groups = TreeView<Name>()
   private val editorsRoot = StackPane()
   private val editorsPane = ConfigPane<Any>()
   private val filterTextField = searchTextField()
   private val filter = filterTextField.textProperty().map {
      it.nullIfBlank()?.net {
         text -> { it: Config<*> -> it.group.contains(text, true) || it.nameUi.contains(text, true) }
      }
   }
   private val filterActions = v(false)
   private val subroot = stackPane {
      lay += vBox {
         lay(NEVER) += hBox(0, CENTER_RIGHT) {
            id = "header"

            lay += CheckIcon(filterActions).icons(IconFA.COMPRESS)
            lay += filterTextField
            lay += Icon(IconFA.RECYCLE, 13.0, "Set all to default").onClickDo { defaults() }
            lay += Icon().blank()
            lay += Icon(IconFA.HOME, 13.0, "App settings").onClickDo { configure(appConfigurable) }
            lay += Icon().blank()
         }
         lay(ALWAYS) += splitPane {
            setDividerPositions(0.25)

            lay(false) += groups.apply {
               id = "groups"
               prefWidth = 200.0
               leftAnchor
            }
            lay(true) += stackPane {
               id = "editors"
               padding = Insets(10.0)

               lay += scrollPane {
                  isFitToWidth = true
                  prefSize = -1 x -1
                  vbarPolicy = ScrollBarPolicy.AS_NEEDED

                  content = editorsRoot.apply {
                     prefSize = -1 x -1
                     isFocusTraversable = true

                     lay += editorsPane
                  }
               }
            }
         }
      }
   }
   private val configs = ArrayList<Config<*>>()
   private val configsFiltered = ArrayList<Config<*>>()
   private val configSelectionName = "app.settings.selected_group"
   private var configSelectionAvoid = false
   private val appConfigurable = APP.configuration

   var showsAppSettings by c(true).noUi()
   var selectedGroupPath by c("").noUi()

   init {
      root.prefSize = 800.emScaled x 600.emScaled
      root.consumeScrolling()
      root.lay += subroot

      groups.selectionModel.selectionMode = SINGLE
      groups.cellFactory = Callback { buildTreeCell(it) }
      groups.propagateESCAPE()
      groups.selectionModel.selectedItemProperty() attach { storeAppSettingsSelection(it) } on onClose
      groups.selectionModel.selectedItemProperty() attach { showConfigs(it?.value) } on onClose
      filter attach { configureFiltered() }
      filterActions attach { configureFiltered() }

      refresh()
   }

   /** Set and apply values. */
   fun apply() = editorsPane.getConfigEditors().forEach { it.apply() }

   /** Set default app settings. */
   fun defaults() = configs.forEach { it.value = it.defaultValue }

   fun refresh() {
      if (showsAppSettings) configure(appConfigurable)
      else refreshConfigs()
   }

   override fun configure(configurable: Configurable<*>?, groupToSelect: String?) {
      showsAppSettings = configurable==appConfigurable
      configs setTo configurable?.getConfigs().orEmpty()
      configureFiltered(groupToSelect)
   }

   fun configureFiltered(groupToSelect: String? = groups.selectionModel.selectedItem?.value?.pathUp) {
      configsFiltered setTo configs.filter { filter.value?.invoke(it)!=false && (it.type!=Action::class.java) xor filterActions.value }
      configSelectionAvoid = true
      groups.isShowRoot = !showsAppSettings
      groups.root = tree(Name.treeOfPaths("All", configsFiltered.map { it.group }))
      groups.root.isExpanded = true
      configSelectionAvoid = false
      storeAppSettingsSelection(groupToSelect)
      groups.expandToRootAndSelect(restoreAppSettingsSelection() ?: groups.root)
   }

   private fun showConfigs(group: Name?) = editorsPane.configure(
      configsFiltered.filter { it.group==group?.pathUp }.toListConfigurable()
   )

   private fun refreshConfigs() = editorsPane.getConfigEditors().forEach { it.refreshItem() }

   private fun storeAppSettingsSelection(selection: String?) {
      if (configSelectionAvoid) return
      selectedGroupPath = selection ?: ""
      if (showsAppSettings && selection!=null) appConfigurable.rawAdd(configSelectionName, PropVal1(selection))
   }

   private fun storeAppSettingsSelection(item: TreeItem<Name>?) = storeAppSettingsSelection(item?.value?.pathUp)

   private fun restoreAppSettingsSelection(): TreeItem<Name>? {
      val selection = if (showsAppSettings) appConfigurable.rawGetAll()[configSelectionName]?.val1 else selectedGroupPath
      return groups.root.recurse { it.children }.find { it.value.pathUp==selection }
   }

}