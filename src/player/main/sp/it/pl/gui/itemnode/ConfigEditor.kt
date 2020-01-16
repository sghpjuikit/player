package sp.it.pl.gui.itemnode

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
import sp.it.pl.gui.itemnode.textfield.EffectTextField
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.appTooltip
import sp.it.pl.plugin.PluginManager
import sp.it.util.access.not
import sp.it.util.action.Action
import sp.it.util.animation.Anim
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.ListConfig
import sp.it.util.conf.OrPropertyConfig
import sp.it.util.conf.OrPropertyConfig.OrValue
import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.asIs
import sp.it.util.functional.invoke
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.type.isSubclassOf
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
import java.util.HashMap

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
abstract class ConfigEditor<T>(@JvmField val config: Config<T>) {
   val isEditableByUser = config.isEditableByUserRightNowProperty()
   var onChange: Runnable? = null
   private var inconsistentState = false
   /** Use to get the control node for setting and displaying the value to attach it to a scene graph. */
   abstract val editor: Node

   abstract fun get(): Try<T, String>

   fun getConfigValue(): T = config.value

   fun getValid(): Try<T, String> = get().and { v ->
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
      root.styleClass.add("config-editor")
      root.setMinSize(0.0, 20.0)   // min height actually required to get consistent look
      root.setPrefSize(-1.0, -1.0) // support variable content height
      root.setMaxSize(-1.0, -1.0)  // support variable content height
      root.alignment = Pos.CENTER_LEFT

      var defB: Icon? = null
      var defBA: Anim? = null
      root.addEventFilter(MOUSE_ENTERED) {
         if (isEditableByUser.value) {
            runFX(270.millis) {
               if (root.isHover) {
                  if (defB==null && isEditableByUser.value) {
                     defB = Icon(null, -1.0, null, Runnable { this.refreshDefaultValue() })
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

                     defBA = Anim.anim(450.millis) {
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
               root.children.remove(defB!!.parent)
               defB = null
               defBA = null
               root.padding = paddingNoDefB
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
      if (isEditableByUser.value) {
         val isNew = config.value!=config.defaultValue
         if (!isNew) return
         inconsistentState = true
         config.value = config.defaultValue
         refreshValue()
         onChange?.invoke()
         inconsistentState = false
      }
   }

   protected fun apply() {
      if (inconsistentState) return
      getValid().ifOk {
         val isNew = it!=config.value
         if (!isNew) return
         inconsistentState = true
         config.value = it
         refreshValue()
         onChange?.invoke()
         inconsistentState = false
      }
   }

   companion object {

      private val CF_BUILDERS = HashMap<Class<*>, (Config<*>) -> ConfigEditor<*>>().apply {
         put(Boolean::class.javaObjectType) { BoolCE(it.asIs()) }
         put(Boolean::class.javaPrimitiveType!!) { BoolCE(it.asIs()) }
         put(String::class.java) { GeneralCE(it) }
         put(Action::class.java) { ShortcutCE(it.asIs()) }
         put(Color::class.java) { ColorCE(it.asIs()) }
         put(File::class.java) { FileCE(it.asIs()) }
         put(Font::class.java) { FontCE(it.asIs()) }
         put(OrValue::class.java) { if (it is OrPropertyConfig<*>) OrCE(it) else GeneralCE(it) }
         put(Effect::class.java) { EffectCE(it.asIs(), Effect::class.java) }
         put(Charset::class.java) { EnumerableCE(it.asIs(), listOf(ISO_8859_1, US_ASCII, UTF_8, UTF_16, UTF_16BE, UTF_16LE)) }
         put(KeyCode::class.java) { KeyCodeCE(it.asIs()) }
         put(Configurable::class.java) { ConfigurableCE(it.asIs()) }
         put(ObservableList::class.java) {
            when (it) {
               is ListConfig<*> -> when {
                  it.a.itemType.isSubclassOf<Configurable<*>>() -> PaginatedObservableListCE(it.asIs())
                  else -> ObservableListCE(it)
               }
               else -> GeneralCE(it)
            }
         }
         put(PluginManager::class.java) { PluginsCE(it.asIs()) }

         EffectTextField.EFFECT_TYPES.asSequence().mapNotNull { it.type }.forEach {
            put(it) { config -> EffectCE(config.asIs(), it) }
         }
      }

      @JvmStatic
      fun <T> create(config: Config<T>): ConfigEditor<T> {
         val cf: ConfigEditor<*> = when {
            config.isTypeEnumerable -> when {
               config.type==KeyCode::class.java -> KeyCodeCE(config.asIs())
               else -> EnumerableCE(config)
            }
            config.isMinMax() -> SliderCE(config.asIs())
            else -> {
               (CF_BUILDERS[config.type] ?: { GeneralCE(it) })(config)
            }
         }

         cf.editor.disableProperty() syncFrom cf.isEditableByUser.not() on cf.editor.onNodeDispose
         return cf.asIs()
      }

      @JvmStatic
      fun <T> createForProperty(type: Class<T>, name: String, property: Any): ConfigEditor<T> = create(Config.forProperty(type, name, property))

      private fun Config<*>.isMinMax() = type.isSubclassOf<Number>() && constraints.any { it is NumberMinMax && it.isClosed() }

   }

}