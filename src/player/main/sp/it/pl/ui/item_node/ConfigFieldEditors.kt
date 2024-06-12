package sp.it.pl.ui.item_node

import com.twelvemonkeys.util.LinkedMap
import com.twelvemonkeys.util.LinkedSet
import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode.CEILING
import java.math.RoundingMode.FLOOR
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.event.Event
import javafx.geometry.Insets
import javafx.geometry.NodeOrientation.LEFT_TO_RIGHT
import javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.RadioButton
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
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
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyCode.TAB
import javafx.scene.input.KeyCode.UNDEFINED
import javafx.scene.input.KeyCombination.NO_MATCH
import javafx.scene.input.KeyCombination.keyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.util.Callback
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.reflect.KClass
import sp.it.pl.core.InfoUi
import sp.it.pl.core.UiStringHelper
import sp.it.pl.layout.ComponentFactory
import sp.it.pl.layout.DeserializingFactory
import sp.it.pl.layout.NoFactoryFactory
import sp.it.pl.layout.NodeFactory
import sp.it.pl.layout.TemplateFactory
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.WidgetManager
import sp.it.pl.layout.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.AppOsMenuIntegrator
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.appTooltip
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.emScaled
import sp.it.pl.main.ifErrorDefault
import sp.it.pl.main.textColon
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBox
import sp.it.pl.plugin.PluginManager
import sp.it.pl.ui.ValueRadioButtonGroup
import sp.it.pl.ui.ValueToggleButtonGroup
import sp.it.pl.ui.item_node.ChainValueNode.ListChainValueNode
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.pl.ui.objects.SpitComboBox.ImprovedComboBoxListCell
import sp.it.pl.ui.objects.SpitSliderSkin
import sp.it.pl.ui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.ui.objects.complexfield.ComplexTextField
import sp.it.pl.ui.objects.complexfield.TagTextField
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.NullCheckIcon
import sp.it.pl.ui.objects.textfield.ColorTextField
import sp.it.pl.ui.objects.textfield.DateTextField
import sp.it.pl.ui.objects.textfield.DateTimeTextField
import sp.it.pl.ui.objects.textfield.EffectTextField
import sp.it.pl.ui.objects.textfield.FileTextField
import sp.it.pl.ui.objects.textfield.FontTextField
import sp.it.pl.ui.objects.textfield.IconTextField
import sp.it.pl.ui.objects.textfield.SpitTextField
import sp.it.pl.ui.objects.textfield.TimeTextField
import sp.it.pl.ui.objects.textfield.ValueTextFieldBi
import sp.it.pl.ui.pane.ConfigPane
import sp.it.util.Na
import sp.it.util.access.OrV
import sp.it.util.access.Values
import sp.it.util.access.editable
import sp.it.util.access.fieldvalue.FileField
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
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.conf.Constraint.FileOut
import sp.it.util.conf.Constraint.FileRelative
import sp.it.util.conf.Constraint.IconConstraint
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.Constraint.NumberMinMax.Between
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.ReadOnlyIf
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.Constraint.UiElementConverter
import sp.it.util.conf.Constraint.UiInfoConverter
import sp.it.util.conf.Constraint.UiNoCustomUnsealedValue
import sp.it.util.conf.ListConfig
import sp.it.util.conf.OrPropertyConfig
import sp.it.util.conf.PropertyConfig
import sp.it.util.conf.PropertyConfigRO
import sp.it.util.conf.SealedEnumerator
import sp.it.util.conf.UnsealedEnumerator
import sp.it.util.dev.fail
import sp.it.util.dev.failCase
import sp.it.util.file.FilePickerType
import sp.it.util.functional.Option.Some
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.Util.by
import sp.it.util.functional.and
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.filter
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.map
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.supplyIfNotNull
import sp.it.util.functional.toOption
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.math.range
import sp.it.util.parsing.nullOf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.onItemSync
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.suppressingAlways
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncTo
import sp.it.util.reactive.syncWhile
import sp.it.util.system.Os
import sp.it.util.text.lengthInLines
import sp.it.util.text.nullIfBlank
import sp.it.util.type.isSubclassOf
import sp.it.util.type.raw
import sp.it.util.ui.dsl
import sp.it.util.ui.hBox
import sp.it.util.ui.hasFocus
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.listView
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.singLineProperty
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.text
import sp.it.util.ui.textFlow
import sp.it.util.ui.vBox

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
      v?.attach { editor.selected.value = it } on disposer

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

@Suppress("RemoveExplicitTypeArguments")
class OrCE<T>(c: OrPropertyConfig<T>): ConfigEditor<OrV.OrValue<T>>(c) {
   override val editor = FlowPane()
   private val oCE = ValueToggleButtonGroupCE(
      Config.forProperty<Boolean>("Override", c.property.override, Some(c.defaultValue.override)).addConstraints(ReadOnlyIf(isEditable, true)),
      listOf(false, true), { text = if (it) "Override" else "Inherit" }
   )
   private val vCE = create(
      Config.forProperty<T>(c.valueType, "", c.property.real, Some(c.defaultValue.value)).addConstraints(ReadOnlyIf(isEditable, true), *c.elementConstraints.toTypedArray())
   )

   init {
      editor.styleClass += "override-config-editor"
      oCE.editor.styleClass += "override-config-editor-override-editor"
      vCE.editor.styleClass += "override-config-editor-value-editor"
      editor.lay += listOf(oCE.editor, vCE.editor)
   }

   override fun get() = oCE.get().and(vCE.get()).map { config.value }

   override fun refreshValue() {
      oCE.refreshValue()
      vCE.refreshValue()
   }

}

class ComplexCE<T>(c: Config<T>): ConfigEditor<T>(c) {
   private val v = getObservableValue(c)
   private val isObservable = v!=null
   private val parser = c.findConstraint<UiStringHelper<T>>()!!
   private val valueChanging = Suppressor()
   override val editor = ComplexTextField(parser)

   init {
      editor.updateValue(c.value)
      editor.onValueChange attach { valueChanging.suppressing { apply() } } on disposer
      v?.attach { valueChanging.suppressed { editor.updateValue(it) } } on disposer

      // readonly
      isEditable syncTo editor.isEditable on disposer
   }

   override fun get() = editor.computeValue()

   override fun refreshValue() {
      if (!isObservable)
         valueChanging.suppressed {
            editor.updateValue(config.value)
         }
   }
}

class SliderCE(c: Config<Number>): ConfigEditor<Number>(c) {
   private val v = getObservableValue(c)
   private val isObservable = v!=null
   private val isInteger = config.type.raw in setOf<Any>(Byte::class, Short::class, Int::class, Long::class)
   private val range = c.findConstraint<Between>()!!
   private val labelFormatter = SpitSliderSkin.labelFormatter(isInteger, range.min, range.max)
   private val min = Label(labelFormatter.toString(range.min))
   private val max = Label(labelFormatter.toString(range.max))
   private val slider = Slider(range.min, range.max, config.value.toDouble())
   override val editor = HBox(min, slider, max)

   init {
      slider.styleClass += "slider-config-editor"
      slider.setOnMouseReleased { apply() }
      slider.min = range.min
      slider.max = range.max
      slider.blockIncrement = (range.max - range.min)/20
      slider.minPrefMaxWidth = -1.0

      editor.alignment = CENTER_LEFT
      editor.spacing = 5.0
      if (isInteger) {
         slider.majorTickUnit = 1.0
         slider.minorTickCount = 0
         slider.isSnapToTicks = true
         slider.labelFormatter = labelFormatter
      }

      // value
      slider.value = config.value.toDouble()
      slider.valueProperty() attach { if (!slider.isValueChanging) apply() } on disposer
      v?.attach { slider.value = it.toDouble() } on disposer

      // increment/decrement
      val scrollHandler = GeneralCE.onNumberScrolledHandler(this)
      if (scrollHandler!=null) {
         slider.onEventDown(SCROLL, scrollHandler)
      }

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
      ?: if (c.type.raw.isSubclassOf<InfoUi>()) { it -> it.asIs<InfoUi>().infoUi } else null

   final override val editor = SpitComboBox<T>(uiConverter)
   private var suppressChanges = false

   init {
      editor.styleClass += STYLECLASS_COMBOBOX_CONFIG_EDITOR
      v?.attach { editor.value = c.value } on disposer
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
                  infoIcon.tooltip(if (item==null && !isNullable) null.toUi() else item.net(converter))
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
         // KEY_PRESSED event. Hence, set the KeyEvent.ANY. Causes the value to be set twice, but that's idempotent
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
   override var editor = FileTextField(config.value, fileType, relativeTo, pickerType)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onEventDown(KEY_PRESSED, ENTER) { it.consume() }

      // value
      editor.onValueChange attach { apply() } on disposer
      v?.attach { editor.value = it } on disposer

      // readonly
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = if (config.type.isNullable || editor.value!=null) Try.ok(editor.value) else Try.error(ObjectNonNull.message())

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }
}

class ValueToggleButtonGroupCE<T>(c: Config<T>, val values: List<T>, customizer: ToggleButton.(T) -> Unit): ConfigEditor<T>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private val uiConverter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: { it.toUi() }
   override val editor = ValueToggleButtonGroup.ofValue(config.value, values) {
      text = uiConverter(it)
      customizer(it)
   }

   init {
      editor.styleClass += "toggle-button-group-config-editor"
      editor.value attach { apply() } on disposer
      v?.attach { editor.value.value = it } on disposer

      // readonly
      isEditable syncTo editor.isEditable on disposer
   }

   override fun get() = Try.ok(editor.value.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value.value = config.value
   }
}

class ValueRadioButtonGroupCE<T>(c: Config<T>, val values: List<T>, customizer: RadioButton.(T) -> Unit): ConfigEditor<T>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private val uiConverter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: { it.toUi() }
   override val editor = ValueRadioButtonGroup(config.value, values) {
      text = uiConverter(it)
      customizer(it)
   }

   init {
      editor.styleClass += "toggle-radio-group-config-editor"
      editor.value attach { apply() } on disposer
      v?.attach { editor.value.value = it } on disposer

      // readonly
      isEditable syncTo editor.isEditable on disposer
   }

   override fun get() = Try.ok(editor.value.value)

   override fun refreshValue() {
      if (!isObservable)
         editor.value.value = config.value
   }
}

open class ValueTextFieldBasedCE<T>(c: Config<T>, editorBuilder: (T) -> ValueTextFieldBi<T & Any>): ConfigEditor<T>(c) {
   final override val editor = editorBuilder(config.value)
   private val valueConverter = editor.valueConverter.nullOf(config.type)
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   private val warnI = lazy {
      Icon().apply {
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
      }
   }

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onValueChange attach { apply() } on disposer
      v?.attach { editor.value = it } on disposer

      editor.textProperty() sync { showWarnButton(getValid()) } on disposer

      // readonly
      editor.selection
      isEditable syncTo editor.editable on disposer
   }

   override fun get() = valueConverter.ofS(editor.text)

   override fun refreshValue() {
      if (!isObservable)
         editor.value = config.value
   }

   private fun showWarnButton(value: Try<*, String>) {
      val shouldBeVisible = value.isError && isEditable.value
      editor.right setTo editor.right.filter { it !== warnI.value }.toMutableList().apply { if (shouldBeVisible) add(0, warnI.value) }
      warnI.orNull()?.isVisible = shouldBeVisible
      warnI.orNull()?.tooltip(value.switch().map { appTooltip(it) }.orNull())
   }
}

class FontCE(c: Config<Font?>): ValueTextFieldBasedCE<Font?>(c, ::FontTextField)

class IconCE(c: Config<GlyphIcons?>): ValueTextFieldBasedCE<GlyphIcons?>(c, ::IconTextField)

class ColorCE(c: Config<Color?>): ConfigEditor<Color?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = ColorTextField(config.value)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onValueChange attach { apply() } on disposer
      v?.attach { editor.value = it } on disposer

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
   override val editor = TimeTextField(config.value, APP.converter.timeFormatter)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onValueChange attach { apply() } on disposer
      v?.attach { editor.value = it } on disposer

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
   override val editor = DateTextField(config.value, APP.locale.value, APP.converter.dateFormatter)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onValueChange attach { apply() } on disposer
      v?.attach { editor.value = it } on disposer

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
   override val editor = DateTimeTextField(config.value, APP.locale.value, APP.converter.dateTimeFormatter)

   init {
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.onValueChange attach { apply() } on disposer
      v?.attach { editor.value = it } on disposer

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
      editor.editors.ui syncFrom APP.ui.formLayout on disposer
      v?.attach { editor.value.value = it } on disposer

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
   private val editorBuilder: (T?) -> ValueNode<T?> = if (lc.a.isSimpleItemType) { it -> ItemSimpleCE(it) } else { it -> ItemComplexCE(it) }
   private val chain = ListChainValueNode<T?, ValueNode<T?>>(0) { editorBuilder(lc.a.itemFactory?.invoke()) }
   private var isSyntheticLinkEvent = false
   private var isSyntheticListEvent = false
   private var isSyntheticSetEvent = false
   override val editor = chain.getNode()

   init {
      chain.isHeaderVisible = true

      // readonly
      chain.editable syncFrom when (lc.a.itemFactory) { is FailFactory -> vAlways(false) else -> isEditable } on disposer

      // bind list to chain
      chain.onUserItemAdded += {
         isSyntheticLinkEvent = true
         if (isNullableOk(it.chained.value)) list += it.chained.value
         isSyntheticLinkEvent = false
      }
      chain.onUserItemRemoved += {
         isSyntheticLinkEvent = true
         list -= it.chained.value
         isSyntheticLinkEvent = false
      }
      chain.onUserItemsCleared += {
         isSyntheticLinkEvent = true
         list.clear()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemEnabled += {
         isSyntheticLinkEvent = true
         if (isNullableOk(it.chained.value)) list += it.chained.value
         isSyntheticLinkEvent = false
      }
      chain.onUserItemDisabled += {
         isSyntheticLinkEvent = true
         list -= it.chained.value
         isSyntheticLinkEvent = false
      }

      // bind chain to list
      disposer += list.onItemSync { item ->
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.addChained(editorBuilder(item))
      }
      disposer += list.onItemRemoved { item ->
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.chain.find { it.chained.value==item }?.let { chain.chain.remove(it) }
      }

   }

   private fun isNullableOk(it: T?) = lc.a.itemType.isNullable || it!=null

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

   private inner class ItemSimpleCE(initialValue: T?): ValueNode<T?>(initialValue) {
      private val pane = create(lc.a.itemToConfigurable(initialValue).asIs<Config<T?>>())
      private val node = pane.buildNode(true)

      init {
         pane.isEditableAllowed syncFrom isEditable on disposer
         pane.onChange = {
            if (!isSyntheticListEvent && !isSyntheticLinkEvent) {
               changeValue(pane.config.value)
               isSyntheticSetEvent = true
               list setTo chain.chain.map { it.chained.value }.filter { isNullableOk(it) }
               isSyntheticSetEvent = false
               this@ObservableListCE.onChange?.invoke()
               this@ObservableListCE.onChangeOrConstraint?.invoke()
            }
         }
      }
      override fun getNode() = node
   }

   private inner class ItemComplexCE(initialValue: T?): ValueNode<T?>(initialValue) {
      private val pane = ConfigPane<T?>()

      init {
         pane.editable syncFrom isEditable on disposer
         pane.ui syncFrom APP.ui.formLayout on disposer
         pane.editorOrder = ConfigPane.compareByDeclaration
         pane.onChange = {}
         pane.configure(lc.toConfigurable(value))
      }
      override fun getNode() = pane
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
         editable syncFrom isEditable on disposer
         styleclass("boolean-config-editor")
         icons(IconMA.CHECK_BOX, IconMA.CHECK_BOX_OUTLINE_BLANK, IconMA.DO_NOT_DISTURB)
         onClickDo {
            toggle()
            onChange?.invoke()
            onChangeOrConstraint?.invoke()
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
            onChange?.invoke()
            onChangeOrConstraint?.invoke()
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
      lay += hBox(10.emScaled) {
         isFillHeight = true
         lay += listView<PluginBox<*>> {
            pseudoClassChanged("no-fixed-cell-size", true)
            nodeOrientation = RIGHT_TO_LEFT
            minPrefMaxWidth = 250.emScaled
            fixedCellSize = 48.emScaled
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
                  val root = hBox(alignment = CENTER_LEFT, spacing = 5.emScaled) {
                     icon.focusOwner.value = this

                     lay += icon
                     lay(ALWAYS) += vBox(alignment = CENTER_LEFT) {
                        lay += label1
                        lay += label2
                     }
                  }

                  override fun updateItem(item: PluginBox<*>?, empty: Boolean) {
                     super.updateItem(item, empty)
                     graphic = item?.let { root }
                     nodeOrientation = LEFT_TO_RIGHT
                     icon.icon(IconOC.PLUG)
                     label1.text = item?.info?.name?.toS()
                     label2.text = item?.let { if (it.isBundled) "bundled" else it.info.version.toS() + "\t" + it.info.author.toS() }
                  }
               }
            }
            selectionModel.selectionMode = SINGLE
            selectionModel.selectedItemProperty() sync { pluginInfo.plugin = it }
            d += { selectionModel.clearSelection() }
            items = APP.plugins.pluginsObservable.toJavaFx().sorted { a, b -> a.info.name compareTo b.info.name }
            d += { items = null }
            d += { pluginInfo.plugin = null }
         }
         lay(ALWAYS) += pluginInfo.apply {
            minWidth = 0.0
            minWidth = 250.emScaled
         }
      }
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

   private class PluginInfoPane: StackPane() {
      val disposer = Disposer()
      var plugin: PluginBox<*>? = null
         set(p) {
            field = p
            children.clear()
            disposer()
            if (p!=null) {
               alignment = TOP_LEFT

               lay += vBox {
                  lay += hBox(0.0, CENTER_LEFT) {
                     lay += label(p.info.name.toUi()) {
                        styleClass += listOf("h4", "h4p", "text-weight-bold")
                     }
                     lay += Icon(IconFA.CARET_DOWN, -1.0, "${p.info.name} Menu").styleclass("header-icon").onClickDo {
                        contextMenuFor(p).show(it, Side.BOTTOM, 0.0, 0.0)
                     }
                  }
                  lay += textColon("Name", p.info.name)
                  lay += textColon("Supported", p.info.isSupported)
                  lay += hBox(0, CENTER_LEFT) {
                     lay += label("Enabled: ")
                     lay += CheckIcon().apply {
                        gap(0)
                        selected syncFrom p.enabled on disposer
                        onClickDo { p.enabled.toggle() }
                     }
                  }
                  if (p.isBundled) {
                     lay += text("Version: " + p.info.version.toUi() + " (bundled)")
                  } else {
                     lay += textColon("Version", p.info.version)
                     lay += textColon("Author", p.info.author)
                  }
                  lay += textColon("Location", p.location)
                  lay += textColon("Location (user data)", p.userLocation)
                  lay += textColon("Enabled by default", p.info.isEnabledByDefault)
                  lay += textColon("Runs in SLAVE application", p.info.isSingleton.not())
                  lay(ALWAYS) += MdNode().apply { readText(p.info.description) }
               }
               lay(TOP_RIGHT) += Icon(p.info.icon ?: IconFA.PLUG, 128.0).apply {
                  isFocusTraversable = false
                  isMouseTransparent = true
               }
            }
         }
   }
}

class WidgetsCE(c: Config<WidgetManager.Widgets>): ConfigEditor<WidgetManager.Widgets>(c) {
   private val widgetInfo = WidgetInfoPane()
   override val editor = stackPane {
      val d = onNodeDispose
      lay += hBox(10.emScaled) {
         isFillHeight = true
         lay += listView<ComponentFactory<*>> {
            pseudoClassChanged("no-fixed-cell-size", true)
            minPrefMaxWidth = 250.emScaled
            nodeOrientation = RIGHT_TO_LEFT
            fixedCellSize = 48.emScaled
            cellFactory = Callback {
               object: ListCell<ComponentFactory<*>>() {
                  val icon = Icon(null, 48.0).apply {
                     isFocusTraversable = false
                     isMouseTransparent = true
                  }
                  val label1 = label("") {
                     styleClass += "text-weight-bold"
                  }
                  val label2 = label("")
                  val root = hBox(alignment = CENTER_LEFT, spacing = 5.emScaled) {
                     icon.focusOwner.value = this

                     lay += icon
                     lay(ALWAYS) += vBox(alignment = CENTER_LEFT) {
                        lay += label1
                        lay += label2
                     }
                  }

                  override fun updateItem(item: ComponentFactory<*>?, empty: Boolean) {
                     super.updateItem(item, empty)
                     nodeOrientation = LEFT_TO_RIGHT
                     graphic = item?.let { root }
                     icon.icon(item.uiIcon)
                     label1.text = item?.name?.toS()
                     label2.text = when (item) {
                        is WidgetFactory<*> -> item.version.toS() + " | " + item.author.toS()
                        is DeserializingFactory -> FileField.TIME_MODIFIED.getOfS(item.launcher, "")
                        is NodeFactory<*> -> "bundled"
                        else -> null
                     }
                  }
               }
            }
            selectionModel.selectionMode = SINGLE
            selectionModel.selectedItemProperty() sync { widgetInfo.widget = it }
            d += { selectionModel.clearSelection() }
            items = APP.widgetManager.factories.getComponentFactoriesObservable().toJavaFx().sorted { a, b -> a.name compareTo b.name }.asIs()
            d += { items = null }
            d += { widgetInfo.widget = null }
         }
         lay(ALWAYS) += widgetInfo.apply {
            minWidth = 0.0
            prefWidth = 250.emScaled
         }
      }
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {}

   class WidgetInfoPane(widget: ComponentFactory<*>? = null): StackPane() {

      val disposer = Disposer()
      var widget: ComponentFactory<*>? = null
         set(f) {
            field = f
            children.clear()
            disposer()
            if (f!=null) {
               alignment = TOP_LEFT

               lay += vBox {
                  lay += hBox(0.0, CENTER_LEFT) {
                     lay += label(f.name.toUi()) {
                        styleClass += listOf("h4", "h4p", "text-weight-bold")
                     }
                     lay += Icon(IconFA.CARET_DOWN, -1.0, "${f.name} Menu").styleclass("header-icon").onClickDo {
                        contextMenuFor(f).show(it, Side.BOTTOM, 0.0, 0.0)
                     }
                  }
                  lay += textColon("Name", f.name)
                  when (f) {
                     is NoFactoryFactory -> {
                        lay += textColon("Type", "Substitute for 'missing component'")
                     }
                     is WidgetFactory<*> -> {
                        lay += textColon("Id", f.id)
                        lay += textColon("Supported", f.isSupported)
                        lay += textColon("Type", "Widget")
                        lay += textColon("Version", f.version)
                        lay += textColon("Year", f.year)
                        lay += textColon("Author", f.author)
                        lay += textColon("Contributor", f.contributor)
                        lay += textColon("Location", f.location)
                        lay += textColon("Location (user data)", f.userLocation)
                        lay += textColon("Features", f.features.featuresUi())
                        lay(ALWAYS) += MdNode().apply { readText(f.descriptionLong) }
                     }
                     is TemplateFactory -> {
                        lay += textColon("Type", "Predefined component")
                     }
                     is DeserializingFactory -> {
                        lay += textColon("Type", "Exported layout")
                        lay += textColon("File", f.launcher)
                     }
                     is NodeFactory<*> -> {
                        lay += textColon("Id", f.id)
                        lay += textColon("Supported", f.info?.isSupported ?: true)
                        lay += textColon("Type", "Ui component ${f.type.toUi()}")
                        lay += textColon("Version", f.info?.version ?: "bundled")
                        lay += textColon("Year", f.info?.year)
                        lay += textColon("Author", f.info?.author)
                        lay += textColon("Contributor", f.info?.contributor)
                        lay += textColon("Location (user data)", f.userLocation)
                        lay += textColon("Features", f.info?.features?.featuresUi())
                        lay(ALWAYS) += MdNode().apply { readText(f.info?.descriptionLong ?: "") }
                     }
                  }
               }
               lay(TOP_RIGHT) += Icon(f.uiIcon, 128.0).apply {
                  isFocusTraversable = false
                  isMouseTransparent = true
               }
            }
         }

      init {
         this.widget = widget
      }
   }

   companion object {
      val ComponentFactory<*>?.uiIcon: GlyphIcons
         get() = when (this) {
            is WidgetFactory<*> -> this.icon ?: IconOC.PLUG
            is NodeFactory<*> -> this.info?.icon ?: IconOC.PLUG
            else -> IconOC.PACKAGE
         }
      fun List<Feature>?.featuresUi(): Any? =
         this?.takeIf { it.isNotEmpty() }?.let { fs ->
            TagTextField<Feature>(
               converter = { fail { "Forbidden" } },
               converterToUi = { it.name },
               converterToDesc = { it.description }
            ).apply {
               pseudoClassToggle("uninteractive", true)
               isEditable.value = false
               items setTo fs
            }
         }
   }
}

class AppOsMenuIntegratorCE(c: Config<AppOsMenuIntegrator>): ConfigEditor<AppOsMenuIntegrator>(c) {
   override val editor = vBox(0.0, CENTER_LEFT) {
      lay += Icon(IconFA.PLAY).onClickDo { config.value.integrate().ifErrorDefault() }.withText(Side.RIGHT, CENTER_LEFT, "Integrate to OS menu")
      lay += Icon(IconFA.PLAY).onClickDo { config.value.disintegrate().ifErrorDefault() }.withText(Side.RIGHT, CENTER_LEFT, "Remove integration to OS menu")
   }

   init {
      // readonly
      isEditable sync { editor.isDisable = !it } on disposer
   }

   override fun get() = Try.ok(AppOsMenuIntegrator)

   override fun refreshValue() = Unit
}

class ConfigurableCE(c: Config<Configurable<*>?>): ConfigEditor<Configurable<*>?>(c) {
   private val v = getObservableValue(c)
   private var isObservable = v!=null
   override val editor = ConfigPane<Any>()

   init {
      editor.onChange = onChange
      editor.onChangeOrConstraint = onChangeOrConstraint
      editor.ui syncFrom APP.ui.formLayout on disposer
      editor.configure(c.value)
      v?.attach { editor.configure(it) } on disposer

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
   private var atL = label("0/0")
   private var nextB = Icon(FontAwesomeIcon.ANGLE_RIGHT, 16.0, "Next item").onClickDo { next() }
   private var configPane = ConfigPane<Any?>()
   override var editor: Node = vBox(10, CENTER_RIGHT) {
      lay += hBox(5, CENTER_RIGHT) {
         lay += prevB
         lay += atL
         lay += nextB
      }
      lay(ALWAYS) += configPane
   }

   init {
      configPane.ui syncFrom APP.ui.formLayout on disposer
      configPane.onChange = onChange
      configPane.onChangeOrConstraint = onChangeOrConstraint
      next()

      // readonly
      isEditable syncTo configPane.editable on disposer
   }

   private fun updateAtL() {
      atL.text = if (c.a.list.isEmpty()) "0/0" else "${at+1}/${c.a.list.size}"
   }

   private fun prev() {
      val size = c.a.list.size
      if (size<=0) at = -1
      if (size<=0) return updateAtL()

      at = if (at==-1 || at==0) size - 1 else at - 1
      configPane.configure(c.a.list[at])
      updateAtL()
   }

   private fun next() {
      atL
      val size = c.a.list.size
      if (size<=0) at = -1
      if (size<=0) return updateAtL()

      at = if (at==-1 || at==size - 1) 0 else at + 1
      configPane.configure(c.a.list[at])
      updateAtL()
   }

   override fun get() = Try.ok(config.value)

   override fun refreshValue() {
      configPane.configure(c.a.list[at])
   }
}

class GeneralCE<T>(c: Config<T>): ConfigEditor<T>(c) {
   val isCollection = c.type.raw.isSubclassOf<Collection<*>>() || c.type.raw.isSubclassOf<Map<*,*>>()
   val isPassword = c.hasConstraint<Constraint.Password>() || isCollection
   val isMultiline = c.hasConstraint<Constraint.Multiline>() || isCollection
   val isMultilineScrollToBottom = isMultiline && c.hasConstraint<Constraint.MultilineScrollToBottom>()
   val isMultilineRows = if (isMultiline) c.findConstraint<Constraint.MultilineRows>()?.rows else null
   val obv = getObservableValue(c)
   override val editor = if (isMultiline) TextArea() else SpitTextField()
   private val converterRaw: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter ?: ::toS
   private val converter: (T) -> String = { get().toOption().filter { v -> v==it }.map { editor.text }.getOrSupply { converterRaw(it) } }
   private val isObservable = obv!=null
   private val isNullEvent = Suppressor()
   private var isNull = config.value==null
   private val isValueRefreshing = Suppressor()
   private var isValueRefreshingRaw = true
   private val sealed = config.findConstraint<SealedEnumerator<T>>()
   private val unsealed = config.findConstraint<UnsealedEnumerator<T>>()
   private val isAutocomplete = sealed!=null || unsealed!=null
   private val isSealed = sealed!=null || (unsealed!=null && config.hasConstraint<UiNoCustomUnsealedValue>())
   private val warnI = lazy {
      Icon().apply {
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
      }
   }

   init {
      if (isPassword) editor.styleClass += SpitTextField.STYLECLASS_PASSWORD
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.promptText = c.nameUi

      // multiline
      editor.asIf<TextArea>().ifNotNull { area ->
         area.isWrapText = true
         area.prefRowCount = isMultilineRows ?: editor.text.lengthInLines.clip(1, 10)
         area.singLineProperty() sync {
            area.styleclassToggle("text-area-singlelined", !it)
            area.prefRowCount = if (it) isMultilineRows ?: editor.text.lengthInLines.clip(1, 10) else 1
         }
      }

      // value
      editor.text = converterRaw(config.value)
      obv?.attach { refreshValue() } on disposer
      obv?.syncWhile { config.value?.asIf<Observable>()?.onChange { refreshValue() } } on disposer
      editor.focusedProperty() attachFalse  { refreshValue() } on disposer
      editor.onEventDown(KEY_RELEASED, ESCAPE) { refreshValue() }

      // applying value
      editor.textProperty() attach {
         isValueRefreshing.suppressed {
            isNullEvent.suppressed {
               isNull = false // cancel null mode on text change
               showWarnButton(getValid())
               isValueRefreshingRaw = false
               apply()
               isValueRefreshingRaw = true
            }
         }
      }

      // null
      fun handleNullEvent(e: KeyEvent) {
         if (isEditable.value) {
            if (isNull) {
               // we can get here for non-null type in special occasions, so we allow user back out of (usually initial) null value gracefully
               editor.text = "" // invokes text change and cancels null mode
               e.consume()
            } else {
               if (isNullable && editor.text.isEmpty()) {
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

      // numbers
      val scrollHandler = onNumberScrolledHandler(this)
      if (scrollHandler!=null) {
         editor.styleClass += "value-text-field"
         editor.styleClass += "number-text-field"
         editor.onEventDown(SCROLL, scrollHandler)
         if (editor is SpitTextField) {
            editor.right += vBox(0.0, Pos.CENTER) {
               styleClass += "number-text-field-buttons"
               onEventDown(MOUSE_CLICKED, scrollHandler)
               onEventDown(SCROLL, scrollHandler)
               lay += Icon(IconFA.CARET_UP, 3.emScaled).apply { gap.value = 0.0; scale(1.5) }
               lay += Icon(IconFA.CARET_DOWN, 3.emScaled).apply { gap.value = 0.0; scale(1.5) }
            }
         }
      }

      // autocomplete
      if (isAutocomplete) {
         val isSortable = config.constraints.none { it is PreserveOrder }
         val enumerator = null
            ?: sealed?.net { { it.enumerateSealed() } }
            ?: unsealed?.net { { it.enumerateUnsealed() } }
            ?: fail { "Forbidden" }

         autoComplete(
            editor,
            { t ->
               @Suppress("UNCHECKED_CAST")
               val enumeration = if (isNullable) enumerator() - (null as T) + (null as T) else enumerator()
               val enumerationSorted = if (isSortable) enumeration.sortedBy(converterRaw) else enumeration
               enumerationSorted.filter { it.toUi().contains(t, true) }
            },
            converterRaw
         ) on disposer
      }

      // readonly
      if (isCollection) {
         editor.editable.value = false
      } else {
         isEditable syncTo editor.editable on disposer
         isEditable sync { showWarnButton(validate(config.value)) } on disposer
      }
   }

   @Suppress("UNCHECKED_CAST")
   override fun get(): Try<T, String> =
      // isSealed must only use value picked through autocomplete (autocomplete sets editor.userData & editor.text -> refresh() -> getValid() -> here)
      if (isSealed) Ok(editor.userData as T)
      else if (isNull) Ok(null as T)
      else Config.convertValueFromString(config, editor.text)

   override fun refreshValue() {
      isValueRefreshing.suppressingAlways {
         if (isSealed) editor.userData = config.value
         isNull = config.value==null
         val text = if (isValueRefreshingRaw) converterRaw(config.value) else converter(config.value)
         if (editor is TextField) {
            editor.text = text
         } else if (editor is TextArea) {
            if (editor.text != text) {
               editor.clear()
               editor.appendText(text)
               if (isMultilineScrollToBottom) editor.scrollTop = Double.MAX_VALUE
            }
         }

         showWarnButton(validate(config.value))
      }
   }

   private fun showWarnButton(value: Try<*, String>) {
      val shouldBeVisible = value.isError && isEditable.value && !isCollection
      if (editor is SpitTextField) editor.right setTo editor.right.filter { it !== warnI.value }.plus(if (shouldBeVisible) listOf(warnI.value) else listOf())
      warnI.orNull()?.isVisible = shouldBeVisible
      warnI.orNull()?.tooltip(value.switch().map { appTooltip(it) }.orNull())
   }

   private fun toS(o: Any?): String = when (o) {
      null ->
         null.toUi()
      is LinkedSet<*> ->
         if (isMultiline) o.joinToString("\n") { it.toUi() }
         else o.joinToString(", ", "[", "]") { it.toUi() }
      is Set<*> ->
         if (isMultiline) o.asSequence().map { it.toUi() }.sorted().joinToString("\n")
         else o.asSequence().map { it.toUi() }.sorted().joinToString(", ", "[", "]")
      is Collection<*> ->
         if (isMultiline) o.joinToString("\n") { it.toUi() }
         else o.joinToString(", ", "[", "]") { it.toUi() }
      is LinkedMap<*, *> ->
         if (isMultiline) o.entries.asSequence().map { it.key.toUi() + " -> " + it.value.toUi() }.joinToString("\n")
         else o.entries.asSequence().map { it.key.toUi() + " -> " + it.value.toUi() }.joinToString(", ", "[", "]")
      is Map<*, *> ->
         if (isMultiline) o.entries.asSequence().map { it.key.toUi() + " -> " + it.value.toUi() }.sorted().joinToString("\n")
         else o.entries.asSequence().map { it.key.toUi() + " -> " + it.value.toUi() }.sorted().joinToString(", ", "[", "]")
      else ->
         if (config.isEditable.isByUser) o.toS()
         else o.toUi()
   }

   companion object {
      fun onNumberScrolledHandler(editor: ConfigEditor<*>) = editor.run {
         when (config.type.raw) {
            Byte::class -> onNumberScrolled<Byte>(Byte.range, { a,b -> (a+b).toByte() }) { it.toByte() }
            UByte::class -> onNumberScrolled<UByte>(UByte.range, { a,b -> (a+b).toUByte() }) { it.toUByte() }
            Short::class -> onNumberScrolled<Short>(Short.range, { a,b -> (a+b).toShort() }) { it.toShort() }
            UShort::class -> onNumberScrolled<UShort>(UShort.range, { a,b -> (a+b).toUShort() }) { it.toUShort() }
            Int::class -> onNumberScrolled<Int>(Int.range, Int::plus) { it }
            UInt::class -> onNumberScrolled<UInt>(UInt.range, UInt::plus) { it.toUInt() }
            Float::class -> onNumberScrolled<Float>(Float.range, Float::plus) { it.toFloat() }
            Long::class -> onNumberScrolled<Long>(Long.range, Long::plus) { it.toLong() }
            Double::class -> onNumberScrolled<Double>(Double.range, Double::plus) { it.toDouble() }
            ULong::class -> onNumberScrolled<ULong>(ULong.range, ULong::plus) { it.toULong() }
            BigInteger::class -> onNumberScrolled<BigInteger>(null, BigInteger::plus) { it.toBigInteger() }
            BigDecimal::class -> onNumberScrolled<BigDecimal>(null, BigDecimal::plus) { it.toBigDecimal() }
            else -> null
         }
      }

      private inline fun <reified T: Comparable<T>> ConfigEditor<*>.onNumberScrolled(range: ClosedRange<T>?, crossinline adder: (T, T) -> T, crossinline caster: (Int) -> T): (Event) -> Unit = { it ->
         val isEditable = this.isEditable.value
         val isMouseEdit = it is MouseEvent && it.eventType==MOUSE_CLICKED && it.button==PRIMARY
         val isScrollEdit = it is ScrollEvent && editor.hasFocus()
         if (isEditable && (isMouseEdit || isScrollEdit)) {
            val dv = when (it) {
               is MouseEvent -> (if (it.source.asIs<Node>().net { it.boundsInParent.centerY <= editor.asIs<Control>().height/2.0 }) +1 else -1) * (if (it.isShortcutDown) 10 else 1)
               is ScrollEvent -> it.deltaY.sign.roundToInt() * (if (it.isShortcutDown) 10 else 1)
               else -> fail { "Illegal switch case on value $it" }
            }
            val ov: T = config.value.asIs() ?: caster(0)
            val nv: T = when {
               ov==range?.min && dv<0 -> ov
               ov==range?.max && dv>0 -> ov
               else -> {
                  val oov = when {
                     ov is Float -> (if (dv<0) ceil(ov) else floor(ov)).asIs()
                     ov is Double -> (if (dv<0) ceil(ov) else floor(ov)).asIs()
                     ov is BigDecimal -> ov.setScale(0, if (dv<0) CEILING else FLOOR).asIs()
                     else -> ov
                  }

                  adder(oov, caster(dv))
               }
            }

            if (asIs<ConfigEditor<T>>().validate(nv).isOk) {
               config.asIs<Config<T>>().value = nv
               refreshValue()
            }

            it.consume()
         }
      }
   }
}

class ShortcutCE(c: Config<Action>): ConfigEditor<Action>(c) {
   override val editor = SpitTextField()
   private var globB = CheckIcon()
   private val warnI = lazy {
      Icon().apply {
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
      }
   }

   init {
      editor.styleClass += "value-text-field"
      editor.styleClass += STYLECLASS_TEXT_CONFIG_EDITOR
      editor.styleClass += "shortcut-config-editor"
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
      isEditable syncTo editor.editable on disposer
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
      warnI.orNull()?.tooltip(value.switch().map { appTooltip(it) }.orNull())
   }

}