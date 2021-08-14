package sp.it.pl.ui.itemnode

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
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
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.ui.objects.textfield.EffectTextField.Companion.EFFECT_TYPES
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.main.appTooltip
import sp.it.pl.plugin.PluginManager
import sp.it.util.action.Action
import sp.it.util.animation.Anim
import sp.it.util.async.runFX
import sp.it.util.collections.map.KClassMap
import sp.it.util.conf.CheckList
import sp.it.util.conf.CheckListConfig
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.ListConfig
import sp.it.util.conf.OrPropertyConfig
import sp.it.util.access.OrV.OrValue
import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.asIs
import sp.it.util.functional.invoke
import sp.it.util.type.isSubclassOf
import sp.it.util.type.jvmErasure
import sp.it.util.ui.onNodeDispose
import sp.it.util.units.millis
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.ISO_8859_1
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.charset.StandardCharsets.UTF_16
import java.nio.charset.StandardCharsets.UTF_16BE
import java.nio.charset.StandardCharsets.UTF_16LE
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javafx.beans.binding.BooleanBinding
import kotlin.reflect.jvm.jvmErasure
import sp.it.pl.core.UiStringHelper
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.functional.andAlso
import sp.it.util.functional.net
import sp.it.util.reactive.Disposer
import sp.it.util.type.VType

private val defTooltip = appTooltip("Default value")
private const val defBLayoutSize = 15.0
private const val configRootSpacing = 5.0
private val paddingNoDefB = Insets(0.0, defBLayoutSize + configRootSpacing, 0.0, 0.0)
private val paddingWithDefB = Insets.EMPTY

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
   var onChange: Runnable? = null
   /** Invoked when value changes or constraint warning changes */
   var onChangeOrConstraint: Runnable? = null
   private var inconsistentState = false
   /** The node setting and displaying the value */
   abstract val editor: Node
   /** Disposer, convenience for: [editor].[Node.onNodeDispose] . */
   protected val disposer = Disposer()

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
    * Use to get the control node for setting and displaying the value to
    * attach it to a scene graph.
    *
    * @param managedControl default true, set false to avoid controls affecting size measurement (particularly helpful
    * with text fields, which can 'expand' layout beyond expected width due to
    * [javafx.scene.control.TextField.prefColumnCountProperty]. I.e., use true to expand and false to shrink.
    *
    * @return setter control for this field
    */
   @JvmOverloads
   fun buildNode(managedControl: Boolean = true): HBox {
      val root = HBox(configRootSpacing)
      root.styleClass += "config-editor"
      root.setMinSize(0.0, 20.0)   // min height actually required to get consistent look
      root.setPrefSize(-1.0, -1.0) // support variable content height
      root.setMaxSize(-1.0, -1.0)  // support variable content height
      root.alignment = Pos.CENTER_LEFT

      var defB: Icon? = null
      var defBA: Anim? = null
      val isTypeSingleton = config.type.type.jvmErasure.objectInstance != null || config.hasConstraint<Constraint.NoUiDefaultButton>()
      val isDefBSupported = config.isEditable.isByUser && !isTypeSingleton
      if (isDefBSupported) {
         root.addEventFilter(MOUSE_ENTERED) {
            if (isEditable.value) {
               runFX(270.millis) {
                  if (root.isHover) {
                     val isDefBNeeded = defB==null && isEditable.value
                     if (isDefBNeeded) {
                        defB = Icon(null, -1.0, null, { this.refreshDefaultValue() })
                        defB!!.tooltip(defTooltip)
                        defB!!.styleclass("config-editor-default-button")
                        defB!!.isManaged = false
                        defB!!.opacity = 0.0

                        val defBRoot = object: StackPane(defB) {
                           override fun layoutChildren() {
                              defB!!.relocate(
                                 width/2.0 - defB!!.layoutBounds.width/2,
                                 height/2.0 - defB!!.layoutBounds.height/2
                              )
                           }
                        }
                        defBRoot.setPrefSize(defBLayoutSize, defBLayoutSize)
                        root.children.add(defBRoot)
                        root.padding = paddingWithDefB

                        defBA = anim(450.millis) {
                           if (defB!=null)
                              defB!!.opacity = it*it
                        }
                     }
                     if (defBA!=null)
                        defBA!!.playOpenDo(null)
                  }
               }
            }
         }
         root.addEventFilter(MOUSE_EXITED) {
            if (defBA!=null)
               defBA!!.playCloseDo {
                  if (defB!!.isFocused) editor.requestFocus()
                  root.children.remove(defB!!.parent)
                  defB = null
                  defBA = null
                  root.padding = paddingNoDefB
               }
         }
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
      root.padding = paddingNoDefB
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
         put<Boolean> { BoolCE(it.asIs()) }
         put<String> { GeneralCE(it) }
         put<Action> { ShortcutCE(it.asIs()) }
         put<Color> { ColorCE(it.asIs()) }
         put<File> { FileCE(it.asIs()) }
         put<Font> { FontCE(it.asIs()) }
         put<LocalTime> { LocalTimeCE(it.asIs()) }
         put<LocalDate> { LocalDateCE(it.asIs()) }
         put<LocalDateTime> { LocalDateTimeCE(it.asIs()) }
         put<OrValue<*>> {
            when (it) {
               is OrPropertyConfig<*> -> OrCE(it)
               else -> null
            }
         }
         put<Charset> { EnumerableCE(it.asIs(), listOf(ISO_8859_1, US_ASCII, UTF_8, UTF_16, UTF_16BE, UTF_16LE)) }
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

         EFFECT_TYPES.map { it.type ?: Effect::class }.forEach {
            put(it) { config -> EffectCE(config.asIs(), it) }
         }
      }

      @JvmStatic
      fun <T> create(config: Config<T>): ConfigEditor<T> {
         fun Config<*>.isMinMax() = type.isSubclassOf<Number>() && !type.isNullable && constraints.any { it is NumberMinMax && it.isClosed() }
         fun Config<*>.isComplex() = constraints.any { it is UiStringHelper<*> }

         return when {
            config.isEnumerable -> when (config.type.jvmErasure) {
               KeyCode::class -> KeyCodeCE(config.asIs())
               else -> EnumerableCE(config)
            }
            config.isComplex() -> ComplexCE(config.asIs())
            config.isMinMax() -> SliderCE(config.asIs())
            else -> editorBuilders[config.type.jvmErasure]?.invoke(config) ?: GeneralCE(config)
         }.apply {
            editor.onNodeDispose += { disposer() }
         }.asIs()
      }

   }

}