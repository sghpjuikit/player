package configurator

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode.F
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.scene.layout.StackPane
import javafx.util.Callback
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.tree.Name
import sp.it.pl.ui.objects.tree.buildTreeCell
import sp.it.pl.ui.objects.tree.tree
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.layout.Widget
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.ConfiguringFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.emScaled
import sp.it.pl.main.searchTextField
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
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.text.nullIfBlank
import sp.it.util.type.raw
import sp.it.util.ui.expandToRootAndSelect
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.leftAnchor
import sp.it.util.ui.prefSize
import sp.it.util.ui.splitPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import java.util.ArrayList
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode.ESCAPE
import kotlin.math.PI
import kotlin.math.sin
import sp.it.pl.main.WidgetTags
import sp.it.pl.main.formEditorsUiToggleIcon
import sp.it.pl.ui.objects.icon.NullCheckIcon
import sp.it.util.access.focused
import sp.it.util.access.vn
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.executor.EventReducer
import sp.it.util.conf.Configuration
import sp.it.util.conf.Constraint
import sp.it.util.dev.printIt
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.onChange
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.zip
import sp.it.util.units.millis

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
   tags = [ WidgetTags.APP ]
)
class Configurator(widget: Widget): SimpleController(widget), ConfiguringFeature {

   private val inputValue = io.i.create<Configurable<Any?>?>("To configure", null) { configure(it) }

   private val groups = TreeView<Name>()
   private val editorsScroll = ScrollPane()
   private val editorsRoot = StackPane()
   private val editorsPane = ConfigPane<Any>()
   private val filterTextField = searchTextField()
   private val filter = filterTextField.textProperty() map {
      it.nullIfBlank()?.net {
         text -> { it: Config<*> -> it.group.contains(text, true) || it.nameUi.contains(text, true) }
      }
   }
   private val filterActions = vn(false)
   private val subroot = stackPane {
      lay += vBox {
         lay(NEVER) += hBox(0, CENTER_RIGHT) {
            id = "header"

            lay += formEditorsUiToggleIcon(editorsPane.ui)
            lay += NullCheckIcon(true, filterActions).icons(IconMA.FILTER_1, IconMA.FILTER_2, IconMA.FILTER).tooltip("Toggle shortcuts\n\nToggle shortcut editors filter only shortcuts | no shortcuts | all")
            lay += filterTextField.apply {
               val focusAnim = anim(200.millis) { prefWidth = (25 + it*125).emScaled }.intpl { sin(PI/2*it) }.applyNow()
               focused zip textProperty() map { (f,t) -> f || t.isNotBlank() } attach focusAnim::playFromDir
            }
            lay += Icon(IconFA.RECYCLE, 13.0).onClickDo { defaults() }.tooltip("Default\n\nSet all editors to default values")
            lay += Icon().blank()
         }
         lay(ALWAYS) += splitPane {
            setDividerPositions(0.2)

            lay(false) += groups.apply {
               id = "groups"
               prefWidth = 200.emScaled
               leftAnchor
            }
            lay(true) += stackPane {
               id = "editors"
               padding = Insets(10.0)

               lay += editorsScroll.apply {
                  isFitToWidth = true
                  isFitToHeight = false
                  prefSize = -1 x -1
                  vbarPolicy = ScrollBarPolicy.AS_NEEDED
                  hbarPolicy = ScrollBarPolicy.NEVER

                  content = editorsRoot.apply {
                     prefSize = -1 x -1
                     isFocusTraversable = true

                     lay += editorsPane.apply {
                        ui syncBiFrom APP.ui.formLayout on onClose
                     }
                  }
               }
            }
         }
      }
   }
   private val configs = ArrayList<Config<*>>()
   private val configsFiltered = ArrayList<Config<*>>()
   private val configSelectionName = "app.settings.selected_group"
   private var configSelectionIgnore = false
   private val configsDisposer = Disposer()
   private val appConfigurable = APP.configuration

   var showsAppSettings by c(true).noUi()
   var selectedGroupPath by c("").noUi()

   init {
      root.prefSize = 1000.emScaled x 800.emScaled
      root.consumeScrolling()
      root.onEventDown(KEY_PRESSED, ESCAPE, consume = false) {
         if (filterTextField.text.isNotBlank()) {
            filterTextField.text = ""
            it.consume()
         }
      }
      root.onEventDown(KEY_PRESSED, F, consume = false) {
         if (it.isShortcutDown) {
            filterTextField.requestFocus()
            filterTextField.selectAll()
            it.consume()
         }
      }
      root.lay += subroot

      groups.selectionModel.selectionMode = SINGLE
      groups.cellFactory = Callback { buildTreeCell(it) }
      groups.propagateESCAPE()
      groups.selectionModel.selectedItemProperty() attach { storeAppSettingsSelection(it) } on onClose
      groups.selectionModel.selectedItemProperty() attach { showConfigs(it?.value) } on onClose
      filter attach { configureFiltered() } on onClose
      filterActions attach { configureFiltered() }

      onClose += configsDisposer
      root.sync1IfInScene { refresh() }
   }

   override fun focus() = root.sync1IfInScene { groups.requestFocus() }.toUnit()

   /** Set all editors to default values. */
   fun defaults() = configs.forEach { it.setValueToDefault() }

   /** Set all editors to actual values. */
   fun refresh() {
      if (showsAppSettings) configure(appConfigurable)
      else refreshConfigs()
   }

   override fun configure(configurable: Configurable<*>?, groupToSelect: String?) {
      configsDisposer()
      showsAppSettings = configurable==appConfigurable
      configs setTo configurable?.getConfigs().orEmpty()
      configureFiltered(groupToSelect)
      if (configurable is Configuration) {
         val updater = EventReducer.toLast<Unit>(200.0) {
            configs setTo configurable.configsObservable
            configureFilteredChange()
         }
         configurable.configsObservable.onChange { updater.push(Unit) } on configsDisposer
      }
   }

   fun configureFiltered(groupToSelect: String? = groups.selectionModel.selectedItem?.value?.pathUp) {
      configsFiltered setTo configs.filter { filter.value?.invoke(it)!=false && filterActions.value.net { a -> a==null || a == (it.type.raw==Action::class) } }
      configSelectionIgnore = true
      groups.isShowRoot = !showsAppSettings
      groups.root = tree(Name.treeOfPaths("All", configsFiltered.map { it.group }))
      groups.root.isExpanded = true
      configSelectionIgnore = false
      storeAppSettingsSelection(groupToSelect)
      groups.expandToRootAndSelect(restoreAppSettingsSelection() ?: groups.root)
   }

   fun configureFilteredChange() {
      configsFiltered setTo configs.filter { filter.value?.invoke(it)!=false && filterActions.value.net { a -> a==null || a == (it.type.raw==Action::class) } }
      configSelectionIgnore = true
      val selection = groups.selectionModel.selectedItem?.value
      groups.isShowRoot = !showsAppSettings
      groups.root treeRecreate Name.treeOfPaths("All", configsFiltered.map { it.group })
      groups.expandToRootAndSelect(groups.root.recurse { it.children }.find { it.value.pathUp==selection?.pathUp } ?: groups.root)
      configSelectionIgnore = false
      showConfigs(groups.selectionModel.selectedItem?.value)
   }

   private fun showConfigs(group: Name?) {
      if (configSelectionIgnore) return
      val c = configsFiltered.filter { it.group==group?.pathUp }.toListConfigurable()
      val changed = editorsPane.getConfigs() differ c.getConfigs()
      if (changed) {
         editorsScroll.isFitToHeight = c.getConfigs().size==1 && c.getConfigs()[0].hasConstraint<Constraint.UiSingleton>()
         editorsPane.configure(c)
      }
   }

   private fun refreshConfigs() = editorsPane.getConfigEditors().forEach { it.refreshValue() }

   private fun storeAppSettingsSelection(selection: String?) {
      if (configSelectionIgnore) return
      selectedGroupPath = selection ?: ""
      if (showsAppSettings && selection!=null) appConfigurable.rawAdd(configSelectionName, PropVal1(selection))
   }

   private fun storeAppSettingsSelection(item: TreeItem<Name>?) = storeAppSettingsSelection(item?.value?.pathUp)

   private fun restoreAppSettingsSelection(): TreeItem<Name>? {
      val selection = if (showsAppSettings) appConfigurable.rawGetAll()[configSelectionName]?.val1 else selectedGroupPath
      return groups.root.recurse { it.children }.find { it.value.pathUp==selection }
   }

   private infix fun <T> Iterable<T>.differ(c: Iterable<T>): Boolean = (this xor c).isNotEmpty()
   private infix fun <T> Iterable<T>.xor(c: Iterable<T>): Set<T> = this.union(c) - this.intersect(c)
   private infix fun TreeItem<Name>.treeRecreate(name: Name) { children setTo name.hChildren.map { n -> children.find { it.value.pathUp == n.pathUp }.apply { treeRecreate(n) } ?: tree(n) } }

}