package sp.it.pl.gui.itemnode

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_LEFT
import javafx.scene.Node
import javafx.scene.control.ColorPicker
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.Slider
import javafx.scene.effect.Effect
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ALT
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.CONTROL
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.META
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.TAB
import javafx.scene.input.KeyCode.UNDEFINED
import javafx.scene.input.KeyCombination.NO_MATCH
import javafx.scene.input.KeyCombination.keyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.ANY
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextFlow
import javafx.util.Callback
import sp.it.pl.gui.itemnode.ChainValueNode.ListChainValueNode
import sp.it.pl.gui.itemnode.textfield.EffectTextField
import sp.it.pl.gui.itemnode.textfield.FileTextField
import sp.it.pl.gui.itemnode.textfield.FontTextField
import sp.it.pl.gui.objects.combobox.ImprovedComboBox
import sp.it.pl.gui.objects.icon.CheckIcon
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.icon.NullCheckIcon
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBox
import sp.it.pl.plugin.PluginManager
import sp.it.util.access.Values
import sp.it.util.access.toggle
import sp.it.util.access.vAlways
import sp.it.util.action.Action
import sp.it.util.action.ActionRegistrar
import sp.it.util.collections.setTo
import sp.it.util.conf.CheckList
import sp.it.util.conf.CheckListConfig
import sp.it.util.conf.ConfList.Companion.FailFactory
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.conf.Constraint.FileOut
import sp.it.util.conf.Constraint.FileRelative
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.ListConfig
import sp.it.util.conf.OrPropertyConfig
import sp.it.util.conf.PropertyConfig
import sp.it.util.conf.PropertyConfigRO
import sp.it.util.dev.failCase
import sp.it.util.file.FilePickerType
import sp.it.util.functional.Try
import sp.it.util.functional.Util.by
import sp.it.util.functional.and
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getAny
import sp.it.util.functional.getOr
import sp.it.util.functional.invoke
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
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
import sp.it.util.reactive.syncWhile
import sp.it.util.system.Os
import sp.it.util.text.nullIfBlank
import sp.it.util.type.raw
import sp.it.util.ui.dsl
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
import kotlin.reflect.KClass

private val warnTooltip = appTooltip("Erroneous value")
private val actTooltip = appTooltip("Run action")
private val globTooltip = appTooltip("Global shortcut"
   + "\n\nGlobal shortcuts can be used even when the application has no focus or window."
   + "\n\nOnly one application can use this shortcut. If multiple applications use "
   + "the same shortcut, usually the one that was first started will work properly.")
private val overTooltip = appTooltip("Override value\n\nUses specified value if true or inherited value if false.")

private const val STYLECLASS_TEXT_CONFIG_EDITOR = "text-config-editor"
const val STYLECLASS_CONFIG_EDITOR_WARN_BUTTON = "config-editor-warn-button"


private fun <T> getObservableValue(c: Config<T>): ObservableValue<T>? = when {
   c is PropertyConfig<T> && c.property is ObservableValue<*> -> c.property.asIs<ObservableValue<T>>()
   c is PropertyConfigRO<T> -> c.property
   else -> null
}

open class BoolCE(c: Config<Boolean?>): ConfigEditor<Boolean?>(c) {
   final override val editor = NullCheckIcon()
   private val v = getObservableValue(c)
   private val isObservable = v!=null
   private val disposer = editor.onNodeDispose

   init {
      editor.styleclass("boolean-config-editor")
      editor.onClickDo {
         editor.selected.value = when (editor.selected.value) {
            null -> true
            true -> false
            false -> if (c.type.isNullable) null else true
         }
      }
      editor.selected.value = c.value
      editor.selected attach { apply() } on disposer
      v?.attach { editor.selected.value = it }.orEmpty() on disposer
   }


   override fun get() = Try.ok(editor.selected.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.selected.value = config.value
   }
}

class OrBoolCE(c: Config<Boolean?>): BoolCE(c) {
   init {
      editor.styleclass("override-config-editor")
      editor.tooltip(overTooltip)
   }
}

class OrCE<T>(c: OrPropertyConfig<T>): ConfigEditor<OrPropertyConfig.OrValue<T>>(c) {
   override val editor = FlowPane(5.0, 5.0)
   private val oCE = create(Config.forProperty<Boolean>("Override", c.property.override))
   private val vCE = create(Config.forProperty(c.valueType, "", c.property.real))

   init {
      c.property.override sync { vCE.editor.isDisable = !it } on editor.onNodeDispose
      editor.children.addAll(vCE.buildNode(), oCE.buildNode())
   }

   override fun get(): Try<OrPropertyConfig.OrValue<T>, String> = oCE.get().and(vCE.get()).map { config.value }

   override fun refreshValue() {
      oCE.refreshValue()
      vCE.refreshValue()
   }

}

class SliderCE(c: Config<Number>): ConfigEditor<Number>(c) {
   val v = getObservableValue(c)
   private val isObservable = v!=null
   val range = c.findConstraint<NumberMinMax>()!!
   private val cur: Label
   private val min = Label(range.min!!.toUi())
   private val max = Label(range.max!!.toUi())
   private val slider = Slider(range.min!!, range.max!!, config.value.toDouble())
   override val editor = HBox(min, slider, max)

   init {
      cur = Label(computeLabelText())
      cur.padding = Insets(0.0, 5.0, 0.0, 0.0) // add gap

      slider.styleClass += "slider-config-editor"
      slider.setOnMouseReleased { apply() }
      slider.blockIncrement = (range.max!! - range.min!!)/20
      slider.minPrefMaxWidth = -1.0

      editor.alignment = CENTER_LEFT
      editor.spacing = 5.0
      if (config.type.raw in setOf<Any>(Int::class, Short::class, Long::class)) {
         editor.children.add(0, cur)
         slider.majorTickUnit = 1.0
         slider.isSnapToTicks = true
      }

      slider.value = config.value.toDouble()
      slider.valueProperty() attach {
         // there is a slight bug where isValueChanging is false even if it should not. It appears when mouse clicks
         // NOT on the thumb but on the slider track instead and keeps dragging. valueChanging does not activate
         cur.text = computeLabelText() // also bug with snap to tick, which does not work on mouse drag so we use get() which returns correct value
         if (!slider.isValueChanging)
            apply()
      } on editor.onNodeDispose
      v?.attach { slider.value = it.toDouble() }.orEmpty() on editor.onNodeDispose
   }

   override fun get(): Try<Number, String> = when (config.type.raw) {
      Int::class -> Try.ok(slider.value.toInt())
      Double::class -> Try.ok(slider.value)
      Float::class -> Try.ok(slider.value.toFloat())
      Long::class -> Try.ok(slider.value.toLong())
      Short::class -> Try.ok(slider.value.toShort())
      else -> failCase(config.type.raw)
   }

   override fun refreshValue() {
      if (!isObservable)
         slider.value = config.value.toDouble()
   }

   private fun computeLabelText(): String = getValid().map { it.toUi() }.getOr("")

}

open class EnumerableCE<T>(c: Config<T>, enumeration: Collection<T> = c.enumerateValues()): ConfigEditor<T>(c) {
   val v = getObservableValue(c)
   val isObservable = v!=null
   val isSortable = c.constraints.none { it is PreserveOrder }
   private val converter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: { it.toUi() }
   final override val editor = ImprovedComboBox(converter)
   private var suppressChanges = false
   private val disposer = editor.onNodeDispose

   init {
      editor.styleClass += "combobox-field-config"
      v?.attach { editor.value = c.value }.orEmpty() on disposer
      if (enumeration is Observable) {
         val list = observableArrayList<T>()
         val listSorted = if (isSortable) list.sorted(by(converter)) else list
         editor.items = listSorted
         disposer += { editor.items = null }

         list setTo enumeration
         enumeration.onChange {
            suppressChanges = true
            list setTo enumeration
            editor.value = null   // prevents JavaFX incorrectly applying next line
            editor.value = c.value
            suppressChanges = false
         } on disposer
      } else {
         editor.items setTo if (isSortable) enumeration.sortedBy(converter) else enumeration
      }
      editor.value = c.value
      editor.valueProperty() attach {
         if (!suppressChanges)
            apply()
      }
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
      // TODO: update warn icon since remove operation may render value illegal
   }

}

class KeyCodeCE(c: Config<KeyCode?>): EnumerableCE<KeyCode?>(c) {
   init {
      editor.onKeyPressed = EventHandler { it.consume() }
      editor.onKeyReleased = EventHandler { it.consume() }
      editor.onKeyTyped = EventHandler { it.consume() }
      editor.onEventUp(ANY) {
         // UP, DOWN, LEFT, RIGHT arrow keys and potentially others (any which cause selection change) do not fire
         // KEY_PRESSED event. Hence set the KeyEvent.ANY. Causes the value to be set twice, but that's idempotent
         if (it.code!=UNDEFINED) {
            editor.value = it.code
            it.consume()
         }
      }
   }
}

class FileCE(c: Config<File?>): ConfigEditor<File?>(c) {
   private val v = getObservableValue(c)
   private val fileType = c.findConstraint<FileActor>() ?: FileActor.ANY
   private val relativeTo = c.findConstraint<FileRelative>()?.to
   private val pickerType = if (c.findConstraint<FileOut>()==null) FilePickerType.IN else FilePickerType.OUT
   private var isObservable = v!=null
   override var editor = FileTextField(fileType, relativeTo, pickerType)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onEventDown(KEY_PRESSED, ENTER) { it.consume() }
      editor.value = config.value
      editor.onValueChange.addS { apply() } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun get() = if (config.type.isNullable || editor.value!=null) Try.ok(editor.value) else Try.error(ObjectNonNull.message())

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}


class FontCE(c: Config<Font?>): ConfigEditor<Font?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = FontTextField()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply() } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class InsetsCE(c: Config<Insets?>): ConfigEditor<Insets?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = DecoratedTextField()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.text = config.value.toS()
      editor.textProperty() attach { apply() } on editor.onNodeDispose
      v?.attach { editor.text = it.toS() }.orEmpty() on editor.onNodeDispose
   }

   override fun get() = APP.converter.general.ofS<Insets>(editor.text)

   override fun refreshValue() {
      if (!isObservable)
         editor.text = config.value.toS()
   }
}

class ColorCE(c: Config<Color?>): ConfigEditor<Color?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = ColorPicker()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.valueProperty() attach { apply() } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class EffectCE(c: Config<Effect?>, effectType: KClass<out Effect>): ConfigEditor<Effect?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = EffectTextField(effectType)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply() } on editor.onNodeDispose
      v?.attach { editor.value = it }.orEmpty() on editor.onNodeDispose
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class ObservableListCE<T>(c: ListConfig<T>): ConfigEditor<ObservableList<T>>(c) {
   private val lc = c
   private val list = lc.a.list
   private val chain = ListChainValueNode<T?, ConfigurableEditor>(0) { ConfigurableEditor(lc.a.itemFactory?.invoke()) }
   private var isSyntheticLinkEvent = false
   private var isSyntheticListEvent = false
   private var isSyntheticSetEvent = false
   override val editor = chain.getNode()
   private val disposer = editor.onNodeDispose

   init {
      chain.isHeaderVisible = true
      chain.editable syncFrom when (lc.a.itemFactory) {
         is FailFactory -> vAlways(false)
         else -> !editor.disableProperty()
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
      disposer += list.onItemSync { item ->
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.addChained(ConfigurableEditor(item))
      }
      disposer += list.onItemRemoved { item ->
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.chain.find { it.chained.getVal()==item }?.let { chain.chain.remove(it) }
      }

   }

   private fun isNullableOk(it: T?) = lc.a.itemType.isNullable || it!=null

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

   private inner class ConfigurableEditor(initialValue: T?): ValueNode<T?>(initialValue) {
      private val pane = ConfigPane<T?>()

      init {
         pane.onChange = Runnable {
            if (lc.a.isSimpleItemType && !isSyntheticListEvent && !isSyntheticLinkEvent) {
               isSyntheticSetEvent = true
               list setTo chain.chain.map { it.chained.getVal() }.filter { isNullableOk(it) }
               isSyntheticSetEvent = false
               this@ObservableListCE.onChange?.invoke()
            }

         }
         pane.configure(lc.toConfigurable(value))
      }

      override fun getNode() = pane

      override fun getVal(): T? = if (lc.a.isSimpleItemType) pane.getConfigEditors()[0].config.value else value

   }
}

@Suppress("UNCHECKED_CAST")
class CheckListCE<T, S: Boolean?>(c: CheckListConfig<T, S>): ConfigEditor<CheckList<T, S>>(c) {
   private val list = c.value
   private val possibleValues = if (list.checkType.isNullable) listOf(true, false, null) else listOf(true, false)
   private val checkIcons = list.all.mapIndexed { i, _ ->
      NullCheckIcon().apply {
         selected.value = list.selections[i]
         selected attach { list.selections[i] = it as S }
         selected attach { updateSuperIcon() }
         styleclass("boolean-config-editor")
         icons(IconMA.CHECK_BOX, IconMA.CHECK_BOX_OUTLINE_BLANK, IconMA.DO_NOT_DISTURB)
         onClickDo {
            selected.value = when (selected.value) {
               null -> true
               true -> false
               false -> if (list.checkType.isNullable) null else true
            }
         }
      }
   }
   private val noSuperIconUpdate = Suppressor()
   private val superIcon = Icon(null).apply {
      styleclass("boolean-config-editor")
      onClickDo {
         val nextValue = when {
            checkIcons.isEmpty() || isIndeterminate() -> true
            else -> Values.next(possibleValues, checkIcons.first().selected.value)
         }
         noSuperIconUpdate.suppressing {
            list.selections setTo list.selections.map { nextValue as S }
            checkIcons.forEach { it.selected.value = nextValue }
         }
         updateSuperIcon()
      }
   }

   init {
      updateSuperIcon()
   }

   override val editor = vBox(0, CENTER_LEFT) {
      lay += superIcon
      list.all.forEachIndexed { i, element ->
         lay += hBox(0, CENTER_LEFT) {
            padding = Insets(0.0, 0.0, 0.0, 5.0.emScaled)
            lay += checkIcons[i]
            lay += label(element.toUi())
         }
      }
   }

   private fun isIndeterminate() = checkIcons.groupBy { it.selected.value }.size!=1

   private fun updateSuperIcon(): Unit = noSuperIconUpdate.suppressed {
      superIcon.icon(
         when {
            checkIcons.isEmpty() -> IconMA.INDETERMINATE_CHECK_BOX
            isIndeterminate() -> IconFA.QUESTION
            else -> {
               when (checkIcons.first().selected.value) {
                  null -> IconMA.DO_NOT_DISTURB
                  true -> IconMD.CHECKBOX_MULTIPLE_MARKED
                  false -> IconMD.CHECKBOX_MULTIPLE_BLANK_OUTLINE
               }
            }
         } as GlyphIcons
      )
   }

   override fun get(): Try<CheckList<T, S>, String> = Try.ok(config.value)

   override fun refreshValue() {}
}

class PluginsCE(c: Config<PluginManager>): ConfigEditor<PluginManager>(c) {
   private val pluginInfo = PluginInfoPane()
   override val editor = stackPane {
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
               items = APP.plugins.pluginsObservable.sorted { a, b -> a.info.name.compareTo(b.info.name) }
               d += { items = null }
            }
            lay(ALWAYS) += pluginInfo
         }

      }
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

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

class ConfigurableCE(c: Config<Configurable<*>?>): ConfigEditor<Configurable<*>?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = ConfigPane<Any>()

   init {
      editor.onChange = onChange
      editor.configure(c.value)
      v?.attach { editor.configure(it) }.orEmpty() on editor.onNodeDispose
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.configure(config.value)
   }
}

class PaginatedObservableListCE(private val c: ListConfig<Configurable<*>?>): ConfigEditor<ObservableList<Configurable<*>?>>(c) {
   private var at = -1
   private var prevB = Icon(FontAwesomeIcon.ANGLE_LEFT, 16.0, "Previous item").onClickDo { prev() }
   private var nextB = Icon(FontAwesomeIcon.ANGLE_RIGHT, 16.0, "Next item").onClickDo { next() }
   private var configPane = ConfigPane<Any?>()
   override var editor: Node = vBox(10, CENTER_RIGHT) {
      lay += hBox(5, CENTER_RIGHT) {
         lay += prevB
         lay += nextB
      }
      lay(ALWAYS) += configPane
   }

   init {
      configPane.onChange = onChange
      next()
   }

   private fun prev() {
      val size = c.a.list.size
      if (size<=0) at = -1
      if (size<=0) return

      at = if (at==-1 || at==0) size - 1 else at - 1
      configPane.configure(c.a.list[at])
   }

   private fun next() {
      val size = c.a.list.size
      if (size<=0) at = -1
      if (size<=0) return

      at = if (at==-1 || at==size - 1) 0 else at + 1
      configPane.configure(c.a.list[at])
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {
      configPane.configure(c.a.list[at])
   }
}

class GeneralCE<T>(c: Config<T>): ConfigEditor<T>(c) {
   override val editor = DecoratedTextField()
   val obv = getObservableValue(c)
   private val isObservable = obv!=null
   private var isNullEvent = Suppressor()
   private var isNull = config.value==null
   private val warnI = lazy {
      Icon().apply {
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
         tooltip(warnTooltip)
      }
   }

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.promptText = c.nameUi

      // refreshing value
      editor.text = toS(getConfigValue())
      obv?.attach { refreshValue() }.orEmpty() on editor.onNodeDispose
      obv?.syncWhile { config.value?.asIf<Observable>()?.onChange { refreshValue() }.orEmpty() }.orEmpty() on editor.onNodeDispose
      editor.focusedProperty() attach {
         if (!it)
            refreshValue()
      }
      editor.onEventDown(KEY_RELEASED, ESCAPE) { refreshValue() }

      // applying value
      editor.textProperty() attach {
         if (!isNullEvent.isSuppressed) {
            if (isNull) isNull = false // cancel null mode on text change
            showWarnButton(getValid())
            apply()
         }
      }
      editor.onEventDown(KEY_PRESSED, ENTER) { apply() }

      // null state toggle
      fun handleNullEvent(e: KeyEvent) {
         if (config.type.isNullable) {
            if (isNull) {
               editor.text = "" // invokes text change and cancels null mode
               e.consume()
            } else {
               if (editor.text.isEmpty()) {
                  isNullEvent.suppressing {
                     isNull = true
                     editor.text = null.toUi()
                     showWarnButton(getValid())
                     apply()
                     e.consume()
                  }
               }
            }
         }
      }
      editor.onEventUp(KEY_PRESSED, BACK_SPACE, false, ::handleNullEvent)
      editor.onEventUp(KEY_PRESSED, DELETE, false, ::handleNullEvent)
      editor.onEventUp(KEY_PRESSED) {
         if (isNull && it.code!=BACK_SPACE && it.code!=DELETE) {
            isNullEvent.suppressing {
               editor.text = ""
               // not consuming handles event natively & invokes text change and cancels null mode
            }
         }
      }

      isEditableByUser sync { showWarnButton(getValid()) } on editor.onNodeDispose
   }

   @Suppress("UNCHECKED_CAST")
   override fun get(): Try<T, String> = if (isNull) Try.ok(null as T) else Config.convertValueFromString(config, editor.text)

   override fun refreshValue() {
      isNull = config.value==null
      editor.text = toS(config.value)
      showWarnButton(getValid())
   }

   private fun showWarnButton(value: Try<*, String>) {
      val shouldBeVisible = value.isError && isEditableByUser.value
      editor.right.value = if (shouldBeVisible) warnI.value else null
      warnI.orNull()?.isVisible = shouldBeVisible
      warnTooltip.text = value.map { "" }.getAny()
   }

   private fun toS(o: Any?): String = when (o) {
      null -> o.toUi()
      is Collection<*> -> o.joinToString(", ", "[", "]") { it.toUi() }
      is Map<*, *> -> o.entries.joinToString(", ", "[", "]") { it.key.toUi() + " -> " + it.value.toUi() }
      else -> if (config.isEditable.isByUser) o.toS() else o.toUi()
   }
}

class ShortcutCE(c: Config<Action>): ConfigEditor<Action>(c) {
   override val editor = DecoratedTextField()
   private var globB = CheckIcon()
   private val warnI = lazy {
      Icon().apply {
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
         tooltip(warnTooltip)
      }
   }

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.styleClass += "shortcut-config-editor"
      editor.isEditable = false
      editor.focusedProperty() attach { refreshValue() }
      editor.onEventDown(KEY_RELEASED) { e ->
         when (e.code) {
            TAB -> {}
            BACK_SPACE, DELETE -> applyShortcut("")
            ESCAPE -> refreshValue()
            else -> {
               if (!e.code.isModifierKey) {
                  val keys = listOfNotNull(
                     CONTROL.takeIf { e.isControlDown || (Os.WINDOWS.isCurrent && e.isShortcutDown) },
                     ALT.takeIf { e.isAltDown },
                     SHIFT.takeIf { e.isShiftDown },
                     META.takeIf { e.isMetaDown || (Os.OSX.isCurrent && e.isShortcutDown) },
                     e.code
                  )
                  applyShortcut(keys.joinToString("+"))
               }
            }
         }
         e.consume()
      }
      editor.promptText = null.toUi()
      editor.text = computePromptText()
      editor.contextMenu = ContextMenu().dsl {
         item("Clear") { applyShortcut("") }
      }
      editor.left.value = hBox(5, CENTER_LEFT) {
         lay += Icon().apply {
            styleclass("config-shortcut-run-icon")
            action(config.value)
            tooltip(actTooltip)
         }
         lay += globB.apply {
            styleclass("config-shortcut-global-icon")
            selected.value = config.value.isGlobal
            tooltip(globTooltip)
            selected attach { applyShortcut(config.value.keys) }
         }
      }
   }

   private fun applyShortcut(newKeys: String) {
      editor.text = newKeys

      val action = config.value
      val sameGlobal = globB.selected.value==action.isGlobal
      val sameKeys = newKeys==action.keys
      if (!sameGlobal || !sameKeys) {
         runTry {
            if (newKeys.isBlank()) NO_MATCH
            else keyCombination(newKeys)
         }.mapError { it.message ?: "Unknown error" }.and { keys ->
            val isUsed = ActionRegistrar.getActions().find { it!==action && it.keyCombination!=NO_MATCH && it.keyCombination==keys }
            if (isUsed==null) Try.ok() else Try.error("Shortcut $newKeys already in use by '${isUsed.group}.${isUsed.nameUi}'")
         }.ifOk {
            action.set(globB.selected.value, newKeys)
            refreshValue()
         }.ifError {
            showWarnButton(Try.error(it))
         }
      } else {
         refreshValue()
      }
   }

   override fun get() = Try.ok(config.value)

   private fun computePromptText(): String = config.value.keys.nullIfBlank().toUi()

   override fun refreshValue() {
      editor.text = computePromptText()
      globB.selected.value = config.value.isGlobal
      showWarnButton(Try.ok())
   }

   private fun showWarnButton(value: Try<*, String>) {
      val shouldBeVisible = value.isError && isEditableByUser.value
      this.editor.right.value = if (shouldBeVisible) warnI.value else null
      warnI.orNull()?.isVisible = shouldBeVisible
      warnTooltip.text = value.map { "" }.getAny()
   }

}