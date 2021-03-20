package sp.it.pl.ui.itemnode

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.Node
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
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.util.Callback
import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.DeserializingFactory
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.main.textColon
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBox
import sp.it.pl.plugin.PluginManager
import sp.it.pl.ui.itemnode.ChainValueNode.ListChainValueNode
import sp.it.pl.ui.objects.textfield.EffectTextField
import sp.it.pl.ui.objects.textfield.FileTextField
import sp.it.pl.ui.objects.textfield.FontTextField
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.pl.ui.objects.SpitComboBox.ImprovedComboBoxListCell
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.NullCheckIcon
import sp.it.pl.ui.objects.textfield.SpitTextField
import sp.it.pl.ui.pane.ConfigPane
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
import sp.it.util.conf.Constraint.IconConstraint
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.Constraint.UiElementConverter
import sp.it.util.conf.Constraint.UiInfoConverter
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
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.invoke
import sp.it.util.functional.net
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
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
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
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.textFlow
import sp.it.util.ui.vBox
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.SPACE
import kotlin.reflect.KClass
import sp.it.pl.core.UiStringHelper
import sp.it.pl.ui.objects.textfield.ColorTextField
import sp.it.pl.ui.objects.textfield.DateTextField
import sp.it.pl.ui.objects.textfield.DateTimeTextField
import sp.it.pl.ui.objects.textfield.TimeTextField
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.objects.SpitSliderSkin
import sp.it.pl.ui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.ui.objects.complexfield.ComplexTextField
import sp.it.util.access.OrV
import sp.it.util.access.editable
import sp.it.util.conf.Constraint.ReadOnlyIf
import sp.it.util.conf.UnsealedEnumerator
import sp.it.util.reactive.suppressingAlways
import sp.it.util.reactive.syncTo

private val warnTooltip = appTooltip("Erroneous value")
private val actTooltip = appTooltip("Run action")
private val globTooltip = appTooltip("Global shortcut"
   + "\n\nGlobal shortcuts can be used even when the application has no focus or window."
   + "\n\nOnly one application can use this shortcut. If multiple applications use "
   + "the same shortcut, usually the one that was first started will work properly.")

const val STYLECLASS_COMBOBOX_CONFIG_EDITOR = "combobox-field-config"
const val STYLECLASS_TEXT_CONFIG_EDITOR = "text-config-editor"
const val STYLECLASS_CONFIG_EDITOR_WARN_BUTTON = "config-editor-warn-button"

private fun <T> getObservableValue(c: Config<T>): ObservableValue<T>? = when {
   c is PropertyConfig<T> && c.property is ObservableValue<*> -> c.property.asIs<ObservableValue<T>>()
   c is PropertyConfigRO<T> -> c.property
   else -> null
}

open class BoolCE(c: Config<Boolean?>): ConfigEditor<Boolean?>(c) {
   private val v = getObservableValue(c)
   private val isObservable = v!=null
   final override val editor = NullCheckIcon(isNullable)

   init {
      editor.styleclass("boolean-config-editor")

      // value
      editor.onClickDo {
         if (isEditable.value)
            editor.selected.value = when (editor.selected.value) {
               null -> true
               true -> false
               false -> if (c.type.isNullable) null else true
            }
      }
      editor.selected.value = c.value
      editor.selected attach { apply() } on disposer
      v?.attach { editor.selected.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer

      // single icon mode using disabled style to mimic false
      val icon = c.findConstraint<IconConstraint>()?.icon
      if (!c.type.isNullable && icon!=null) {
         editor.icons(icon, icon, icon)
         editor.selected sync { editor.pseudoClassChanged("disabled", it!=true) }
      }
   }

   override fun get() = Try.ok(editor.selected.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.selected.value = config.value
   }
}

class OrCE<T>(c: OrPropertyConfig<T>): ConfigEditor<OrV.OrValue<T>>(c) {
   override val editor = FlowPane()
   private val oCE = create(Config.forProperty<Boolean>("Override", c.property.override).addConstraints(ReadOnlyIf(isEditable)))
   private val vCE = create(Config.forProperty(c.valueType, "", c.property.real).addConstraints(ReadOnlyIf(isEditable)).addConstraints(ReadOnlyIf(c.property.override, true)))

   init {
      editor.styleClass += "override-config-editor"
      editor.lay += listOf(vCE.buildNode(), oCE.buildNode())
   }

   override fun get(): Try<OrV.OrValue<T>, String> = oCE.get().and(vCE.get()).map { config.value }

   override fun refreshValue() {
      oCE.refreshValue()
      vCE.refreshValue()
   }

}

class ComplexCE<T>(c: Config<T>): ConfigEditor<T>(c) {
   private val v = getObservableValue(c)
   private val isObservable = v!=null
   private val parser = c.findConstraint<UiStringHelper<T>>()!!
   val editorPrimary = ComplexTextField(parser)
   val editorSecondary = TextField()
   override val editor = vBox {
      lay += editorSecondary
      lay(ALWAYS) += editorPrimary
   }
   private val valueChanging = Suppressor()
   private val valueTextChangingApp = Suppressor()
   private val valueTextChangingUser = Suppressor()
   private var valueFromPrimary = true

   init {
      editorSecondary.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR

      // value
      editorSecondary.apply {
         editorPrimary.valueText sync {
            valueTextChangingUser.suppressed {
               valueTextChangingApp.suppressing {
                  valueFromPrimary = true
                  text = it
               }
            }
         }
         textProperty() attach {
            valueTextChangingApp.suppressed {
               valueTextChangingUser.suppressing {
                  valueFromPrimary = false
                  editorPrimary.clearValue()
                  apply()
               }
            }
         }
      }
      editorPrimary.updateValue(c.value)
      editorPrimary.onValueChange.addS { valueTextChangingUser.suppressed { valueChanging.suppressing { apply() } } } on disposer
      v?.attach { valueChanging.suppressed { editorPrimary.updateValue(it) } }.orEmpty() on disposer

      // readonly
      isEditable syncTo editorPrimary.isEditable on disposer
      isEditable syncTo editorSecondary.editable on disposer
   }

   override fun get() = if (valueFromPrimary) editorPrimary.computeValue() else parser.parse.parse(editorSecondary.text)

   override fun refreshValue() {
      if (!isObservable)
         valueChanging.suppressed {
            editorPrimary.updateValue(config.value)
         }
   }
}

class SliderCE(c: Config<Number>): ConfigEditor<Number>(c) {
   private val v = getObservableValue(c)
   private val isObservable = v!=null
   private val isDecimal = config.type.raw in setOf<Any>(Int::class, Short::class, Long::class)
   private val range = c.findConstraint<NumberMinMax>()!!
   private val labelFormatter = SpitSliderSkin.labelFormatter(isDecimal, range.min!!, range.max!!)
   private val min = Label(labelFormatter.toString(range.min!!))
   private val max = Label(labelFormatter.toString(range.max!!))
   private val slider = Slider(range.min!!, range.max!!, config.value.toDouble())
   override val editor = HBox(min, slider, max)

   init {
      slider.styleClass += "slider-config-editor"
      slider.setOnMouseReleased { apply() }
      slider.blockIncrement = (range.max!! - range.min!!)/20
      slider.minPrefMaxWidth = -1.0

      editor.alignment = CENTER_LEFT
      editor.spacing = 5.0
      if (isDecimal) {
         slider.majorTickUnit = 1.0
         slider.isSnapToTicks = true
         slider.labelFormatter = labelFormatter
      }

      // value
      slider.value = config.value.toDouble()
      slider.valueProperty() attach { if (!slider.isValueChanging) apply() } on disposer
      v?.attach { slider.value = it.toDouble() }.orEmpty() on disposer

      // readonly
      isEditable sync { editor.isDisable = !it } on disposer
   }

   override fun get(): Try<Number, String> = when (config.type.raw) {
      Int::class -> Try.ok(slider.value.toInt())
      Double::class -> Try.ok(slider.value)
      Float::class -> Try.ok(slider.value.toFloat())
      Long::class -> Try.ok(slider.value.toLong())
      Short::class -> Try.ok(slider.value.toInt().toShort())
      else -> failCase(config.type.raw)
   }

   override fun refreshValue() {
      if (!isObservable)
         slider.value = config.value.toDouble()
   }

}

open class EnumerableCE<T>(c: Config<T>, enumeration: Collection<T> = c.enumerateValues()): ConfigEditor<T>(c) {
   val v = getObservableValue(c)
   val isObservable = v!=null
   val isSortable = c.constraints.none { it is PreserveOrder }
   private val uiConverter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: { it.toUi() }
   private val uiInfoConverter: ((T) -> String)? = c.findConstraint<UiInfoConverter<T>>()?.converter
   final override val editor = SpitComboBox(uiConverter)
   private var suppressChanges = false

   init {
      editor.styleClass += STYLECLASS_COMBOBOX_CONFIG_EDITOR
      v?.attach { editor.value = c.value }.orEmpty() on disposer
      if (enumeration is Observable) {
         val list = observableArrayList<T>()
         val listSorted = if (isSortable) list.sorted(by(uiConverter)) else list
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
         editor.items setTo if (isSortable) enumeration.sortedBy(uiConverter) else enumeration
      }
      editor.value = c.value
      editor.valueProperty() attach {
         if (!suppressChanges)
            apply()
      }
      
      // readonly
      isEditable sync { editor.readOnly.value = !it } on disposer

      // info button
      uiInfoConverter.ifNotNull { converter ->
         editor.cellFactory = Callback {
            object: ImprovedComboBoxListCell<T>(editor) {
               val infoIcon = Icon(IconFA.INFO)

               override fun updateItem(item: T, empty: Boolean) {
                  super.updateItem(item, empty)
                  super.setGraphic(infoIcon)
                  infoIcon.tooltip(item.net(converter))
               }
            }
         }
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
//      editor.onKeyPressed = EventHandler { it.consume() }
//      editor.onKeyReleased = EventHandler { it.consume() }
//      editor.onKeyTyped = EventHandler { it.consume() }
      editor.onEventUp(KEY_PRESSED) {
         // UP, DOWN, LEFT, RIGHT arrow keys and potentially others (any which cause selection change) do not fire
         // KEY_PRESSED event. Hence set the KeyEvent.ANY. Causes the value to be set twice, but that's idempotent
         if (isEditable.value && it.code!=UNDEFINED && !it.code.isArrowKey && !it.code.isNavigationKey && it.code!=TAB && it.code!=SPACE) {
            editor.value = it.code
         }
      }
   }
}

class FileCE(c: Config<File?>): ConfigEditor<File?>(c) {
   private val v = getObservableValue(c)
   private val fileType = c.findConstraint<FileActor>() ?: FileActor.ANY
   private val relativeTo = c.findConstraint<FileRelative>()?.to
   private val pickerType = if (c.hasConstraint<FileOut>()) FilePickerType.OUT else FilePickerType.IN
   private var isObservable = v!=null
   override var editor = FileTextField(fileType, relativeTo, pickerType)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onEventDown(KEY_PRESSED, ENTER) { it.consume() }

      // value
      editor.value = config.value
      editor.onValueChange.addS { apply() } on disposer
      v?.attach { editor.value = it }.orEmpty() on disposer
      
      // readonly
      isEditable syncTo editor.editable on disposer
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
      editor.onValueChange.addS { apply() } on disposer
      v?.attach { editor.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
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
   override val editor = SpitTextField()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.text = toS(config.value)
      editor.textProperty() attach { apply() } on disposer
      v?.attach { editor.text = toS(it) }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = APP.converter.general.ofS<Insets?>(editor.text)

   override fun refreshValue() {
      if (!isObservable)
         editor.text = toS(config.value)
   }

   private fun toS(o: Any?) = when (o) {
      null -> o.toUi()
      else -> o.toS()
   }
}

class ColorCE(c: Config<Color?>): ConfigEditor<Color?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = ColorTextField()

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply() } on disposer
      v?.attach { editor.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class LocalTimeCE(c: Config<LocalTime?>): ConfigEditor<LocalTime?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = TimeTextField(APP.converter.timeFormatter)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply() } on disposer
      v?.attach { editor.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class LocalDateCE(c: Config<LocalDate?>): ConfigEditor<LocalDate?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = DateTextField(Locale.getDefault(), APP.converter.dateFormatter)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply() } on disposer
      v?.attach { editor.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = Try.ok(editor.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class LocalDateTimeCE(c: Config<LocalDateTime?>): ConfigEditor<LocalDateTime?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = DateTimeTextField(Locale.getDefault(), APP.converter.dateTimeFormatter)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value = config.value
      editor.onValueChange.addS { apply() } on disposer
      v?.attach { editor.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
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
   override val editor = EffectTextField(isNullable, effectType, config.value)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.value.value = config.value
      editor.value attach { apply() } on disposer
      v?.attach { editor.value.value = it }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = Try.ok(editor.value.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value.value = config.value
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

   init {
      chain.isHeaderVisible = true

      // readonly
      chain.editable syncFrom when { lc.a.itemFactory is FailFactory -> vAlways(false) else -> isEditable }

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
         pane.editable syncFrom isEditable on disposer
         pane.onChange = Runnable {
            if (lc.a.isSimpleItemType && !isSyntheticListEvent && !isSyntheticLinkEvent) {
               isSyntheticSetEvent = true
               list setTo chain.chain.map { it.chained.getVal() }.filter { isNullableOk(it) }
               isSyntheticSetEvent = false
               this@ObservableListCE.onChange?.invoke()
               this@ObservableListCE.onChangeOrConstraint?.invoke()
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
   private val isSelectionNullable = list.checkType.isNullable
   private val possibleValues = if (isSelectionNullable) listOf(true, false, null) else listOf(true, false)
   private val uiConverter: (T) -> String = c.findConstraint<UiElementConverter<T>>()?.converter ?: { it.toUi() }
   private val checkIcons = list.all.mapIndexed { i, _ ->
      NullCheckIcon(isSelectionNullable).apply {
         selected.value = list.selections[i]
         selected attach { list.selections[i] = it as S }
         selected attach { updateSuperIcon() }
         editable syncFrom isEditable
         styleclass("boolean-config-editor")
         icons(IconMA.CHECK_BOX, IconMA.CHECK_BOX_OUTLINE_BLANK, IconMA.DO_NOT_DISTURB)
         onClickDo {
            toggle()
            onChange?.run()
            onChangeOrConstraint?.run()
         }
      }
   }
   private val noSuperIconUpdate = Suppressor()
   private val superIcon = Icon(null).apply {
      styleclass("boolean-config-editor")
      onClickDo {
         if (isEditable.value) {
            val nextValue = when {
               checkIcons.isEmpty() || isIndeterminate() -> true
               else -> Values.next(possibleValues, checkIcons.first().selected.value)
            }
            noSuperIconUpdate.suppressing {
               list.selections setTo list.selections.map { nextValue as S }
               checkIcons.forEach { it.selected.value = nextValue }
            }
            updateSuperIcon()
            onChange?.run()
            onChangeOrConstraint?.run()
         }
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
            lay += label(uiConverter(element)) {
               labelForWithClick setTo checkIcons[i]
            }
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
         }
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
               pseudoClassChanged("no-fixed-cell-size", true)
               minPrefMaxWidth = 250.emScaled
               cellFactory = Callback {
                  object: ListCell<PluginBox<*>>() {
                     val icon = Icon(null, 48.0).apply {
                        isFocusTraversable = false
                        isMouseTransparent = true
                     }
                     val label1 = label("") {
                        styleClass += "text-weight-bold"
                     }
                     val label2 = label("")
                     val root = hBox {
                        lay += icon
                        lay += vBox {
                           lay += label1
                           lay += label2
                        }
                     }

                     override fun updateItem(item: PluginBox<*>?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = root
                        icon.icon(IconOC.PLUG)
                        label1.text = item?.info?.name?.toS()
                        label2.text = item?.let { if (it.isBundled) "bundled" else it.info.version.toS() + "\t" + it.info.author.toS() }
                     }
                  }
               }
               selectionModel.selectionMode = SINGLE
               selectionModel.selectedItemProperty() sync { pluginInfo.plugin = it }
               d += { selectionModel.clearSelection() }
               items = APP.plugins.pluginsObservable.toJavaFx().sorted { a, b -> a.info.name.compareTo(b.info.name) }
               d += { items = null }
               d += { pluginInfo.plugin = null }
            }
            lay(ALWAYS) += pluginInfo
         }

      }
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

   private class PluginInfoPane: StackPane() {
      val disposer = Disposer()
      var plugin: PluginBox<*>? = null
         set(value) {
            field = value
            children.clear()
            disposer()
            if (value!=null) {
               alignment = TOP_LEFT

               lay(TOP_RIGHT) += Icon(value.info.icon ?: IconFA.PLUG, 128.0).apply {
                  isFocusTraversable = false
                  isMouseTransparent = true
               }
               lay += vBox {
                  lay += label(value.info.name.toUi()) {
                     styleClass += listOf("h4", "h4p", "text-weight-bold")
                  }
                  lay += textColon("Name", value.info.name)
                  lay += textColon("Supported", value.info.isSupported)
                  lay += hBox(0, CENTER_LEFT) {
                     lay += label("Enabled: ")
                     lay += CheckIcon().apply {
                        gap(0)
                        selected syncFrom value.enabled on disposer
                        onClickDo { value.enabled.toggle() }
                     }
                  }
                  if (value.isBundled) {
                     lay += text("Version: " + value.info.version.toUi() + " (bundled)")
                  } else {
                     lay += textColon("Version", value.info.version)
                     lay += textColon("Author", value.info.author)
                  }
                  lay += textColon("Location", value.location)
                  lay += textColon("Location (user data)", value.userLocation)
                  lay += textColon("Enabled by default", value.info.isEnabledByDefault)
                  lay += textColon("Runs in SLAVE application", value.info.isSingleton.not())
                  lay += textFlow {
                     styleClass += "h4p"
                     lay += text(value.info.description.toUi())
                  }
               }
            }
         }
   }
}

class WidgetsCE(c: Config<WidgetManager.Widgets>): ConfigEditor<WidgetManager.Widgets>(c) {
   private val widgetInfo = WidgetInfoPane()
   override val editor = stackPane {
      val d = onNodeDispose
      lay += vBox {
         lay += label("Installed widgets:").apply {
            styleClass += "h4p"
         }
         lay += hBox {
            lay += listView<ComponentFactory<*>> {
               pseudoClassChanged("no-fixed-cell-size", true)
               minPrefMaxWidth = 250.emScaled
               cellFactory = Callback {
                  object: ListCell<ComponentFactory<*>>() {
                     val icon = Icon(null, 48.0).apply {
                        isFocusTraversable = false
                        onClickDo(2) { APP.windowManager.showWindow(item.create()) }
                     }
                     val label1 = label("") {
                        styleClass += "text-weight-bold"
                     }
                     val label2 = label("")
                     val root = hBox {
                        lay += icon
                        lay += vBox {
                           lay += label1
                           lay += label2
                        }
                     }

                     override fun updateItem(item: ComponentFactory<*>?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = root
                        icon.icon(item.uiIcon)
                        label1.text = item?.name?.toS()
                        label2.text = when (item) {
                           is WidgetFactory<*> -> item.version.toS() + "\t" + item.author.toS()
                           else -> null
                        }
                     }
                  }
               }
               selectionModel.selectionMode = SINGLE
               selectionModel.selectedItemProperty() sync { widgetInfo.widget = it }
               d += { selectionModel.clearSelection() }
               items = APP.widgetManager.factories.getComponentFactoriesObservable().toJavaFx().sorted { a, b -> a.name.compareTo(b.name) }.asIs()
               d += { items = null }
               d += { widgetInfo.widget = null }
            }
            lay(ALWAYS) += widgetInfo
         }

      }
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

   private class WidgetInfoPane: StackPane() {

      val disposer = Disposer()
      var widget: ComponentFactory<*>? = null
         set(value) {
            field = value
            children.clear()
            disposer()
            if (value!=null) {
               alignment = TOP_LEFT

               lay += vBox {
                  lay += label(value.name.toUi()) {
                     styleClass += listOf("h4", "h4p", "text-weight-bold")
                  }
                  lay += textColon("Name", value.name)
                  when (value) {
                     is WidgetFactory<*> -> {
                        lay += textColon("Id", value.id)
                        lay += textColon("Supported", value.isSupported)
                        lay += textColon("Version", value.version)
                        lay += textColon("Year", value.year)
                        lay += textColon("Author", value.author)
                        lay += textColon("Contributor", value.contributor)
                        lay += textColon("Location", value.location)
                        lay += textColon("Location (user data)", value.userLocation)
                        lay += textFlow {
                           styleClass += "h4p"
                           lay += text(value.descriptionLong.toUi())
                           lay += text {
                              val fs = value.features
                              "Features: " + (if (fs.isEmpty()) "none" else fs.joinToString("\n") { "\t${it.name} - ${it.description}" })
                           }
                        }
                     }
                     is DeserializingFactory -> {
                        lay += textColon("File", value.launcher)
                     }
                  }
               }
               lay(TOP_RIGHT) += Icon(value.uiIcon, 128.0).apply {
                  isFocusTraversable = false
                  onClickDo(2) { APP.windowManager.showWindow(value.create()) }
               }
            }
         }
   }

   companion object {
      val ComponentFactory<*>?.uiIcon: GlyphIcons
         get() = when (this) {
            is WidgetFactory<*> -> this.icon ?: IconOC.PLUG
            else -> IconOC.PACKAGE
         }
   }
}

class ConfigurableCE(c: Config<Configurable<*>?>): ConfigEditor<Configurable<*>?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = ConfigPane<Any>()

   init {
      editor.onChange = onChange
      editor.onChangeOrConstraint = onChangeOrConstraint
      editor.configure(c.value)
      v?.attach { editor.configure(it) }.orEmpty() on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
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
      configPane.onChangeOrConstraint = onChangeOrConstraint
      next()

      // readonly
      isEditable syncTo configPane.editable on disposer
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
   override val editor = SpitTextField()
   val obv = getObservableValue(c)
   private val converter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: ::toS
   private val isObservable = obv!=null
   private val isNullEvent = Suppressor()
   private var isNull = config.value==null
   private val isValueRefreshing = Suppressor()
   private val warnI = lazy {
      Icon().apply {
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
         tooltip(warnTooltip)
      }
   }

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.promptText = c.nameUi

      // value
      editor.text = converter(config.value)
      obv?.attach { refreshValue() }.orEmpty() on disposer
      obv?.syncWhile { config.value?.asIf<Observable>()?.onChange { refreshValue() }.orEmpty() }.orEmpty() on disposer
      editor.focusedProperty() attach { if (!it) refreshValue() } on disposer
      editor.onEventDown(KEY_RELEASED, ESCAPE) { refreshValue() }

      // applying value
      editor.textProperty() attach {
         isValueRefreshing.suppressed {
            isNullEvent.suppressed {
               if (isNull) isNull = false // cancel null mode on text change
               showWarnButton(getValid())
               apply()
            }
         }
      }
      editor.onEventDown(KEY_PRESSED, ENTER) { apply() }

      // null
      fun handleNullEvent(e: KeyEvent) {
         if (isEditable.value)
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
         if (isEditable.value)
            if (isNull && it.code!=BACK_SPACE && it.code!=DELETE) {
               isNullEvent.suppressing {
                  editor.text = ""
                  // not consuming handles event natively & invokes text change and cancels null mode
               }
            }
      }

      // autocomplete
      config.findConstraint<UnsealedEnumerator<T>>().ifNotNull { e ->
         // TODO support observable iterator, like EnumerableCE does
         val isSortable = config.constraints.none { it is PreserveOrder }
         @Suppress("UNCHECKED_CAST")
         val enumeration = if (isNullable) e.enumerateUnsealed() - (null as T) + (null as T) else e.enumerateUnsealed()
         val enumerationSorted = if (isSortable) enumeration.sortedBy(converter) else enumeration
         autoComplete(editor, { t -> enumerationSorted.filter { it.toUi().contains(t, true) } }, converter)
      }

      // readonly
      isEditable syncTo editor.editable on disposer
      isEditable sync { showWarnButton(getValid()) } on disposer
   }

   @Suppress("UNCHECKED_CAST")
   override fun get(): Try<T, String> = if (isNull) Try.ok(null as T) else Config.convertValueFromString(config, editor.text)

   override fun refreshValue() {
      isValueRefreshing.suppressingAlways {
         isNull = config.value==null
         editor.text = converter(config.value)
         showWarnButton(getValid())
      }
   }

   private fun showWarnButton(value: Try<*, String>) {
      val shouldBeVisible = value.isError && isEditable.value
      editor.right setTo if (shouldBeVisible) listOf(warnI.value) else listOf()
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
   override val editor = SpitTextField()
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
      editor.focusedProperty() attach { refreshValue() } on disposer
      editor.onEventDown(KEY_RELEASED) { e ->
         if (isEditable.value) {
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
         }
         e.consume()
      }
      editor.promptText = null.toUi()
      editor.text = computePromptText()
      editor.contextMenu = ContextMenu().dsl {
         item("Clear") { applyShortcut("") }
      }
      editor.left setTo listOf(
         Icon().apply {
            styleclass("config-shortcut-run-icon")
            action(config.value)
            tooltip(actTooltip)
         },
         globB.apply {
            styleclass("config-shortcut-global-icon")
            selected.value = config.value.isGlobal
            tooltip(globTooltip)
            selected attach { applyShortcut(config.value.keys) }
         }
      )

      // readonly
      isEditable syncTo globB.editable on disposer
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
      val shouldBeVisible = value.isError && isEditable.value
      editor.right setTo if (shouldBeVisible) listOf(warnI.value) else listOf()
      warnI.orNull()?.isVisible = shouldBeVisible
      warnTooltip.text = value.map { "" }.getAny()
   }

}