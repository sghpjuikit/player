package sp.it.pl.gui.itemnode

import javafx.beans.Observable
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.TOP_LEFT
import javafx.scene.control.ColorPicker
import javafx.scene.control.ListCell
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.UNDEFINED
import javafx.scene.input.KeyEvent.ANY
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextFlow
import javafx.util.Callback
import sp.it.pl.gui.itemnode.ChainValueNode.ListChainValueNode
import sp.it.pl.gui.itemnode.textfield.FileTextField
import sp.it.pl.gui.itemnode.textfield.FontTextField
import sp.it.pl.gui.objects.combobox.ImprovedComboBox
import sp.it.pl.gui.objects.icon.CheckIcon
import sp.it.pl.gui.objects.icon.NullCheckIcon
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.main.APP
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBox
import sp.it.pl.plugin.PluginManager
import sp.it.util.access.toggle
import sp.it.util.access.vAlways
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfList.Companion.FailFactory
import sp.it.util.conf.Config
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.conf.Constraint.FileRelative
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.ListConfig
import sp.it.util.file.FilePickerType
import sp.it.util.functional.Try
import sp.it.util.functional.Util.by
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.onItemSync
import sp.it.util.reactive.orEmpty
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.listView
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import java.io.File

private val overTooltip = appTooltip("Override value\n\nUses specified value if true or inherited value if false.")

private open class BoolCE(c: Config<Boolean>): ConfigEditor<Boolean>(c) {
   private val v = getObservableValue(c)
   protected val graphics = NullCheckIcon()
   private val isObservable = v!=null
   private val isNullable = c.findConstraint<ObjectNonNull>()==null
   private val disposer = graphics.onNodeDispose

   init {
      graphics.styleclass("boolean-config-editor")
      graphics.onClickDo {
         graphics.selected.value = when (graphics.selected.value) {
            null -> true
            true -> false
            false -> if (isNullable) null else true
         }
      }
      graphics.selected.value = c.value
      graphics.selected attach { apply(false) } on disposer
      v?.attach { graphics.selected.value = it }.orEmpty() on disposer
   }

   override fun getEditor() = graphics

   override fun get() = Try.ok(graphics.selected.value)

   override fun refreshItem() {
      if (!isObservable)
         graphics.selected.value = config.value
   }
}

private class OrBoolCE(c: Config<Boolean>): BoolCE(c) {
   init {
      editor.styleclass("override-config-editor")
      editor.tooltip(overTooltip)
   }
}

private open class EnumerableCE<T> @JvmOverloads constructor(c: Config<T>, enumeration: Collection<T> = c.enumerateValues()): ConfigEditor<T>(c) {
   val v = getObservableValue(c)
   val isObservable = v!=null
   val isSortable = c.constraints.none { it is PreserveOrder }
   private val converter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: { it.toUi() }
   protected val n = ImprovedComboBox(converter)
   protected var suppressChanges = false
   private val disposer = n.onNodeDispose

   init {
      n.styleClass += "combobox-field-config"
      v?.attach { n.value = c.value }.orEmpty() on disposer
      if (enumeration is Observable) {
         val list = observableArrayList<T>()
         val listSorted = if (isSortable) list.sorted(by(converter)) else list
         n.items = listSorted
         disposer += { n.items = null }

         list setTo enumeration
         enumeration.onChange {
            suppressChanges = true
            list setTo enumeration
            n.value = null   // prevents JavaFX incorrectly applying next line
            n.value = c.value
            suppressChanges = false
         } on disposer
      } else {
         n.items setTo if (isSortable) enumeration.sortedBy(converter) else enumeration
      }
      n.value = c.value
      n.valueProperty() attach {
         if (!suppressChanges)
            apply(false)
      }
   }

   override fun get() = Try.ok(n.value)

   override fun refreshItem() {
      if (!isObservable)
         n.value = config.value
      // TODO: update warn icon since remove operation may render value illegal
   }

   override fun getEditor() = n
}

private class KeyCodeCE(c: Config<KeyCode>): EnumerableCE<KeyCode>(c) {

   init {
      n.onKeyPressed = EventHandler { it.consume() }
      n.onKeyReleased = EventHandler { it.consume() }
      n.onKeyTyped = EventHandler { it.consume() }
      n.onEventUp(ANY) {
         // UP, DOWN, LEFT, RIGHT arrow keys and potentially others (any which cause selection change) do not fire
         // KEY_PRESSED event. Hence set the KeyEvent.ANY. Causes the value to be set twice, but that's idempotent
         if (it.code!=UNDEFINED) {
            n.value = it.code
            it.consume()
         }
      }
   }

}

private class FileCE(c: Config<File>): ConfigEditor<File>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private var editor: FileTextField
   private val isNullable = c.findConstraint<ObjectNonNull>()==null

   init {
      val fileType = c.findConstraint<FileActor>() ?: FileActor.ANY
      val relativeTo = c.findConstraint<FileRelative>()?.to
      val pickerType = if (c.findConstraint<Constraint.FileOut>()==null) FilePickerType.IN else FilePickerType.OUT

      editor = FileTextField(fileType, relativeTo, pickerType)
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onEventDown(KEY_PRESSED, ENTER) { it.consume() }
      editor.value = config.value
      editor.onValueChange.addS { apply(false) } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun getEditor() = editor

   override fun get() = if (isNullable || editor.value!=null) Try.ok(editor.value) else Try.error(ObjectNonNull.message())

   override fun refreshItem() {
      if (!isObservable)
         editor.value = config.value
   }
}


private class FontCE(c: Config<Font>): ConfigEditor<Font>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private val editor = FontTextField()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply(false) } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun getEditor() = editor

   override fun get() = Try.ok(editor.value)

   override fun refreshItem() {
      if (!isObservable)
         editor.value = config.value
   }
}

private class InsetsCE(c: Config<Insets>): ConfigEditor<Insets>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private val editor = DecoratedTextField()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.text = config.value.toS()
      editor.textProperty() attach { apply(false) } on editor.onNodeDispose
      v?.attach { editor.text = it.toS() }.orEmpty() on editor.onNodeDispose
   }

   override fun getEditor() = editor

   override fun get() = APP.converter.general.ofS<Insets>(editor.text)

   override fun refreshItem() {
      if (!isObservable)
         editor.text = config.value.toS()
   }
}

private class ColorCE(c: Config<Color>): ConfigEditor<Color>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private val editor = ColorPicker()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.valueProperty() attach { apply(false) } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun getEditor() = editor

   override fun get() = Try.ok(editor.value)

   override fun refreshItem() {
      if (!isObservable)
         editor.value = config.value
   }
}

private class ObservableListCE<T>(c: ListConfig<T>): ConfigEditor<ObservableList<T>>(c) {
   private val lc = c
   private val list = lc.a.list
   private val chain = ListChainValueNode<T?, ConfigurableEditor>(0) { ConfigurableEditor(lc.a.itemFactory?.invoke()) }
   private var isSyntheticLinkEvent = false
   private var isSyntheticListEvent = false
   private var isSyntheticSetEvent = false
   private val disposer = chain.getNode().onNodeDispose

   init {
      chain.isHeaderVisible = true
      chain.editable syncFrom when {
         lc.a.itemFactory is FailFactory -> vAlways(false)
         else -> !chain.getNode().disableProperty()
      }

      // bind list to chain
      chain.onUserItemAdded += {
         isSyntheticLinkEvent = true
         if (isNullableOk(it.chained.getVal())) list += it.chained.getVal()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemRemoved += {
         isSyntheticLinkEvent = true
         list -= it.chained.getVal()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemsCleared += {
         isSyntheticLinkEvent = true
         list.clear()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemEnabled += {
         isSyntheticLinkEvent = true
         if (isNullableOk(it.chained.getVal())) list += it.chained.getVal()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemDisabled += {
         isSyntheticLinkEvent = true
         list -= it.chained.getVal()
         isSyntheticLinkEvent = false
      }

      // bind chain to list
      disposer += list.onItemSync {
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.addChained(ConfigurableEditor(it))
      }
      disposer += list.onItemRemoved {
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.chain.find { it.chained.getVal()==it }?.let { chain.chain.remove(it) }
      }

   }

   private fun isNullableOk(it: T?) = lc.a.isNullable || it!=null

   override fun getEditor() = chain.getNode()

   override fun get() = Try.ok(config.value)

   override fun refreshItem() {}

   private inner class ConfigurableEditor(initialValue: T?): ValueNode<T?>(initialValue) {
      private val pane = ConfigPane<T?>()

      init {
         pane.onChange = Runnable {
            if (lc.a.isSimpleItemType && !isSyntheticListEvent && !isSyntheticLinkEvent) {
               isSyntheticSetEvent = true
               list setTo chain.chain.map { it.chained.getVal() }.filter { isNullableOk(it) }
               isSyntheticSetEvent = false
            }
         }
         pane.configure(lc.toConfigurable(value))
      }

      override fun getNode() = pane

      override fun getVal(): T? = if (lc.a.isSimpleItemType) pane.getConfigEditors()[0].configValue else value

   }
}

private class PluginsCE(c: Config<PluginManager>): ConfigEditor<PluginManager>(c) {
   private val pluginInfo = PluginInfoPane()
   private val editor = stackPane {
      val d = onNodeDispose
      lay += vBox {
         lay += label("Installed plugins:").apply {
            styleClass += "h4p"
         }
         lay += hBox {
            lay += listView<PluginBox<*>> {
               minPrefMaxWidth = 250.emScaled
               cellFactory = Callback {
                  object: ListCell<PluginBox<*>>() {
                     override fun updateItem(item: PluginBox<*>?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = item?.info?.name
                     }
                  }
               }
               selectionModel.selectionMode = SINGLE
               selectionModel.selectedItemProperty() sync { pluginInfo.plugin = it }
               d += { selectionModel.clearSelection() }
               items = sp.it.pl.main.APP.plugins.pluginsObservable.sorted { a, b -> a.info.name.compareTo(b.info.name) }
               d += { items = null }
            }
            lay(ALWAYS) += pluginInfo
         }

      }
   }

   override fun get() = Try.ok(config.value)
   override fun refreshItem() {}
   override fun getEditor() = editor

   class PluginInfoPane: VBox() {
      private val disposer = Disposer()
      var plugin: PluginBox<*>? = null
         set(value) {
            field = value
            children.clear()
            disposer()
            if (value!=null) {
               alignment = TOP_LEFT
               lay += label("Name: " + value.info.name.toUi())
               lay += label("Supported: " + value.info.isSupported.toUi())
               lay += hBox(0, CENTER_LEFT) {
                  lay += label("Enabled: ")
                  lay += CheckIcon().apply {
                     gap(0)
                     selected syncFrom value.enabled on disposer
                     onClickDo { value.enabled.toggle() }
                  }
               }
               lay += label("Version: " + value.info.version.toUi())
               lay += label("Bundled: " + value.isBundled.toUi())
               lay += label("Enabled by default: " + value.info.isEnabledByDefault.toUi())
               lay += label("Runs in SLAVE application: " + value.info.isSingleton.not().toUi())
               lay += TextFlow().apply {
                  styleClass += "h4p"
                  lay += text(value.info.description.toUi())
               }
            }
         }
   }
}
