package sp.it.pl.ui.item_node

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javafx.beans.binding.BooleanBinding
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.effect.Effect
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import kotlin.reflect.KClass
import sp.it.pl.core.CoreMenus
import sp.it.pl.core.UiStringHelper
import sp.it.pl.layout.WidgetManager
import sp.it.pl.main.AppOsMenuIntegrator
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.appTooltip
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginManager
import sp.it.pl.ui.objects.autocomplete.AutoCompletion
import sp.it.pl.ui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.textfield.EffectTextField.Companion.EFFECT_TYPES
import sp.it.util.access.OrV.OrValue
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runFX
import sp.it.util.collections.map.KClassMap
import sp.it.util.conf.CheckList
import sp.it.util.conf.CheckListConfig
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.ConfigurationContext
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.ValueSealedRadio
import sp.it.util.conf.Constraint.ValueSealedSet
import sp.it.util.conf.Constraint.ValueSealedToggle
import sp.it.util.conf.Constraint.ValueUnsealedSet
import sp.it.util.conf.ListConfig
import sp.it.util.conf.OrPropertyConfig
import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.andAlso
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.map
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.zip
import sp.it.util.type.VType
import sp.it.util.type.enumValues
import sp.it.util.type.isObject
import sp.it.util.type.isSubclassOf
import sp.it.util.type.raw
import sp.it.util.type.rawJ
import sp.it.util.ui.dsl
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.textIcon
import sp.it.util.units.millis

private const val caretLayoutSize = 15.0
private const val configRootSpacing = 5.0
private val paddingNoCaret = Insets(0.0, caretLayoutSize + configRootSpacing, 0.0, 0.0)
private val paddingWithCaret = Insets.EMPTY

/**
 * Editable and settable graphic control for configuring [sp.it.util.conf.Config].
 *
 * Convenient way to create wide and diverse property sheets, that take
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 */
abstract class ConfigEditor<T>(val config: Config<T>) {
   /** Whether this editor allows editing. Affects [isEditable]. Default true. */
   val isEditableAllowed = v(true)
   /** Whether this editor is editable. Takes into account [Config.isEditable], [Config.constraints], [isEditableAllowed]  */
   val isEditable = config.isEditableByUserRightNowProperty().net {
      object: BooleanBinding() {
         init { bind(isEditableAllowed, it) }
         override fun computeValue() = isEditableAllowed.value && it.value
         override fun dispose() = unbind(isEditableAllowed, it)
      }.run {
         readOnly()
      }
   }
   /** Whether config value type is nullable, convenience for: [config].[Config.type].[VType.isNullable] */
   val isNullable = config.type.isNullable
   /** Invoked when value changes */
   var onChange: (() -> Unit)? = null
   /** Invoked when value changes or constraint warning changes */
   var onChangeOrConstraint: (() -> Unit)? = null
   /** The node setting and displaying the value */
   abstract val editor: Node
   /** Disposer, convenience for: [editor].[Node.onNodeDispose] . */
   protected val disposer = Disposer()

   private var inconsistentState = false

   abstract fun get(): Try<T, String>

   fun getValid(): Try<T, String> = get().andAlso { validate(it) }

   fun validate(value: T): Try<T, String> = Try.ok(value).and { v ->
      if (!config.type.isNullable) ObjectNonNull.validate(v) else Try.ok()
   }.and { v ->
      config.constraints.map { it.validate(v) }.find { it.isError } ?: Try.ok()
   }

   /**
    * Creates label with config field name and tooltip with config field description.
    *
    * @return label describing this field
    */
   fun buildLabel(): Label {
      val l = Label(config.nameUi)

      val tooltipText = getTooltipText()
      if (tooltipText.isNotEmpty()) {
         val t = appTooltip(tooltipText)
         t.isWrapText = true
         t.maxWidth = 300.0
         l.tooltip = t
      }

      return l
   }

   /**
    * Create the editor node for setting and displaying the value to attach it to a scene graph.
    *
    * @param managedControl default true, set false to avoid controls affecting size measurement (particularly helpful
    * with text fields, which can 'expand' layout beyond expected width due to
    * [javafx.scene.control.TextField.prefColumnCountProperty]).
    * I.e., use true to expand and false to shrink.
    *
    * @return setter control for this field
    */
   @JvmOverloads
   fun buildNode(managedControl: Boolean = true): HBox {
      val root = HBox(configRootSpacing)
      root.styleClass += "config-editor"
      root.setMinSize(0.0, 0.0)
      root.setPrefSize(-1.0, -1.0) // support variable content height
      root.setMaxSize(-1.0, -1.0)  // support variable content height
      root.alignment = Pos.CENTER_LEFT

      var caretB: Icon? = null
      var caretA: Anim? = null
      val isCaretSupported = !config.type.type.raw.isObject && !config.hasConstraint<Constraint.NoUiDefaultButton>()
      if (isCaretSupported) {
         val configHover = v(false)
         val configMenuVisible = v(false)
         val caretVisible = configHover zip configMenuVisible map { (a,b) -> a || b }
         caretVisible attach {
            if (caretA!=null) {
               caretA!!.playCloseDo {
                  if (caretB!!.isFocused) editor.requestFocus()
                  root.children.remove(caretB!!.parent)
                  caretB = null
                  caretA = null
                  root.padding = paddingNoCaret
               }
            }
         }

         fun configHoverFalse() { configHover.value = root.isHover || root.isFocusWithin }
         fun configHoverTrue() {
            runFX(270.millis) {
               if (root.isHover || root.isFocusWithin) {
                  configHover.value = true
                  if (caretB==null) {
                     caretB = Icon(null, -1.0).onClickDo { i ->
                        ContextMenu().dsl {
                           if (isEditable.value) {
                              item("Set to default") {
                                 if (isEditable.value)
                                    this@ConfigEditor.refreshDefaultValue()
                              }
                              if (config.type.isNullable) item("Set to ${null.toUi()}") {
                                 if (isEditable.value)
                                    this@ConfigEditor.config.asIs<Config<Nothing?>>().value = null
                              }
                           }
                           menu("Value") {
                              items {
                                 CoreMenus.menuItemBuilders[value]
                              }
                              if (this@ConfigEditor is ComplexCE<*>) {
                                 item("Copy as text", keys = "CTRL + C") { editor.copyValueAsText() }
                                 item("Paste as text", keys = "CTRL + V") { editor.pasteValueAsText() }.apply { if (!editor.pasteValueAsTextPossible()) isDisable = true }
                              }
                           }
                        }.apply {
                           configMenuVisible.value = true
                           onEventDown(WINDOW_HIDDEN) { configMenuVisible.value = false }
                           show(i, Side.BOTTOM, 0.0, 0.0)
                        }
                        Unit // TODO: remove workaround; Kotlin K2 compiler bug
                     }
                     caretB!!.styleclass("config-editor-caret")
                     caretB!!.isManaged = false
                     caretB!!.opacity = 0.0

                     val caretRoot = object: StackPane(caretB) {
                        override fun layoutChildren() {
                           caretB!!.relocate(
                              width/2.0 - caretB!!.layoutBounds.width/2,
                              height/2.0 - caretB!!.layoutBounds.height/2
                           )
                        }
                     }
                     caretRoot.setPrefSize(caretLayoutSize, caretLayoutSize)
                     root.children.add(caretRoot)
                     root.padding = paddingWithCaret

                     caretA = anim(450.millis) {
                        if (caretB!=null)
                           caretB!!.opacity = it*it
                     }
                  }
                  caretA?.playOpenDo(null)
               }
            }
         }
         root.focusWithinProperty() attach { if (it) configHoverTrue() else configHoverFalse() }
         root.addEventFilter(MOUSE_ENTERED) { configHoverTrue() }
         root.addEventFilter(MOUSE_EXITED) { configHoverFalse() }
      }

      val isHardToAutoResize = editor is TextField
      val config = if (!isHardToAutoResize)
         editor
      else
         object: StackPane(editor) {
            init {
               editor.isManaged = managedControl
            }

            override fun layoutChildren() {
               children[0].resizeRelocate(0.0, 0.0, width, height)
            }
         }
      root.children.add(0, config)
      root.padding = paddingNoCaret
      HBox.setHgrow(config, Priority.ALWAYS)
      HBox.setHgrow(config.parent, Priority.ALWAYS)

      return root
   }

   fun focusEditor() = editor.requestFocus()

   protected fun getTooltipText() = config.info

   /**
    * Refreshes the content of this config field. The content is read from the
    * Config and as such reflects the real value. Using this method after the
    * applying the new value will confirm the success visually to the user.
    */
   abstract fun refreshValue()

   /** Sets and applies default value of the config if it has different value set and if editable by user.  */
   fun refreshDefaultValue() {
      if (inconsistentState) return
      if (isEditable.value) {
         val isNew = config.value!=config.defaultValue
         if (!isNew) return
         inconsistentState = true
         config.value = config.defaultValue
         refreshValue()
         onChange?.invoke()
         onChangeOrConstraint?.invoke()
         inconsistentState = false
      }
   }

   protected fun apply() {
      if (inconsistentState) return
      getValid().ifOk {
         val isNew = it!=config.value
         if (isNew) {
            inconsistentState = true
            config.value = it
            refreshValue()
            onChange?.invoke()
            onChangeOrConstraint?.invoke()
            inconsistentState = false
         } else {
            onChangeOrConstraint?.invoke()
         }
      }.ifError {
         onChangeOrConstraint?.invoke()
      }
   }

   companion object {

      private val editorBuilders = KClassMap<(Config<*>) -> ConfigEditor<*>?>().apply {
         put<Boolean> {
            fun values() = listOf(true, false).run { if (it.type.isNullable) this+null else this }
            when {
               it.hasConstraint<ValueSealedToggle>() -> ValueToggleButtonGroupCE(it.asIs(), values(), {})
               it.hasConstraint<ValueSealedRadio>() -> ValueRadioButtonGroupCE(it.asIs(), values(), {})
               else -> BoolCE(it.asIs())
            }
         }
         put<Action> { ShortcutCE(it.asIs()) }
         put<Color> { ColorCE(it.asIs()) }
         put<File> { FileCE(it.asIs()) }
         put<Font> { FontCE(it.asIs()) }
         put<GlyphIcons> { IconCE(it.asIs()) }
         put<TextAlignment> {
            ValueToggleButtonGroupCE(it.asIs(), it.asIs<Config<TextAlignment?>>().type.enumValues(), {
                  textIcon(
                     when (it) {
                        null -> IconMA.DO_NOT_DISTURB
                        TextAlignment.CENTER -> IconMD.FORMAT_ALIGN_CENTER
                        TextAlignment.LEFT -> IconMD.FORMAT_ALIGN_LEFT
                        TextAlignment.RIGHT -> IconMD.FORMAT_ALIGN_RIGHT
                        TextAlignment.JUSTIFY -> IconMD.FORMAT_ALIGN_JUSTIFY
                     }
                  )
               }
            )
         }
         put<Orientation> {
            ValueToggleButtonGroupCE(it.asIs(), it.asIs<Config<Orientation?>>().type.enumValues(), {
                  textIcon(
                     when (it) {
                        null -> IconMA.DO_NOT_DISTURB
                        Orientation.HORIZONTAL -> IconMD.DOTS_HORIZONTAL
                        Orientation.VERTICAL -> IconMD.DOTS_VERTICAL
                     }
                  )
               }
            )
         }
         put<LocalTime> { LocalTimeCE(it.asIs()) }
         put<LocalDate> { LocalDateCE(it.asIs()) }
         put<LocalDateTime> { LocalDateTimeCE(it.asIs()) }
         put<OrValue<*>> {
            when (it) {
               is OrPropertyConfig<*> -> OrCE(it)
               else -> null
            }
         }
         put<Charset> {
            val cs = Charset.availableCharsets().values.toList().run { if (it.type.isNullable) this+null else this }
            EnumerableCE(it.asIs(), cs)
         }
         put<KeyCode> { KeyCodeCE(it.asIs()) }
         put<Configurable<*>> { ConfigurableCE(it.asIs()) }
         put<ObservableList<*>> {
            when (it) {
               is ListConfig<*> -> when {
                  it.a.itemType.isSubclassOf<Configurable<*>>() -> PaginatedObservableListCE(it.asIs())
                  else -> ObservableListCE(it)
               }
               else -> null
            }
         }
         put<CheckList<*, *>> {
            when (it) {
               is CheckListConfig<*, *> -> CheckListCE<Any?, Boolean?>(it.asIs())
               else -> null
            }
         }
         put<PluginManager> {
            if (it.type.isNullable) GeneralCE(it)
            else PluginsCE(it.asIs())
         }
         put<WidgetManager.Widgets> {
            if (it.type.isNullable) GeneralCE(it)
            else WidgetsCE(it.asIs())
         }
         put<AppOsMenuIntegrator> {
            if (it.type.isNullable) GeneralCE(it)
            else AppOsMenuIntegratorCE(it.asIs())
         }

         EFFECT_TYPES.map { it.type ?: Effect::class }.forEach {
            put(it) { config -> EffectCE(config.asIs(), it) }
         }
      }

      @JvmStatic
      fun <T> create(config: Config<T>): ConfigEditor<T> {
         fun Config<*>.isMinMax() = !type.isNullable && type.raw in listOf<KClass<*>>(Int::class, Double::class, Float::class, Long::class, Short::class) && constraints.any { it is NumberMinMax && it.isClosed() }
         fun Config<*>.isComplex() = constraints.any { it is UiStringHelper<*> }
         fun Config<*>.isConfigurable() = type.raw.isSubclassOf<Configurable<*>>()

         return when {
            config.isComplex() -> ComplexCE(config.asIs())
            config.isMinMax() -> SliderCE(config.asIs())
            else -> null
               ?: editorBuilders[config.type.raw]?.invoke(config)
               ?: if (config.isEnumerable) when {
                     config.hasConstraint<ValueSealedToggle>() -> ValueToggleButtonGroupCE(config.asIs(), config.enumerateValues().toList(), {})
                     config.hasConstraint<ValueSealedRadio>() -> ValueRadioButtonGroupCE(config.asIs(), config.enumerateValues().toList(), {})
                     else -> EnumerableCE(config)
                  }
                  else null
               ?: if (config.isConfigurable()) ConfigurableCE(config.asIs()) else null
               ?: GeneralCE(config).apply {
                  if (!config.hasConstraint<ValueSealedSet<*>>() && !config.hasConstraint<ValueUnsealedSet<*>>() && AutoCompletion.of<Any?>(editor)==null) {
                     when (config.type.rawJ) {
                        Class::class.java -> autoComplete(editor) { text -> ConfigurationContext.unsealedEnumeratorClasses.filter { it.contains(text, true) } }
                        KClass::class.java -> autoComplete(editor) { text -> ConfigurationContext.unsealedEnumeratorClasses.filter { it.contains(text, true) } }
                     }
                  }
               }
         }.apply {
            editor.onNodeDispose += { disposer() }
         }.asIs()
      }

   }

}