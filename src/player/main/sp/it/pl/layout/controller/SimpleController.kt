package sp.it.pl.layout.controller

import kotlin.reflect.full.findAnnotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import sp.it.pl.layout.Widget
import sp.it.pl.layout.controller.io.Input
import sp.it.pl.layout.controller.io.Output
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDelegator
import sp.it.util.conf.ConfigValueSource
import sp.it.util.conf.collectActionsOf
import sp.it.util.conf.toConfigurableByReflect
import sp.it.util.file.json.JsNull
import sp.it.util.functional.asIs
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.ui.stackPane

/**
 * Base controller implementation that provides
 * - [root]
 * - support for automatic restoration of configurable properties, using delegated configurable properties, see
 * [sp.it.util.conf.cv] family of methods. This is [ConfigDelegator] and uses [widget] as [configurableValueSource].
 */
open class SimpleController(widget: Widget): Controller(widget), ConfigDelegator {

   /** The ui root that attaches this widget to the scene graph */
   @JvmField val root = stackPane {
      id = "controller-root"
   }
   /** The coroutine scope partaking in the life-cycle of this widget. Cancelled on [close]. [MainScope]. Use to launch coroutines. */
   @JvmField val scope: CoroutineScope = MainScope()
   /** The disposer partaking in the life-cycle of this widget. Called on [close]. Use to dispose resources. */
   @JvmField val onClose = Disposer()
   private var isLegacyConfigsInitialized = false
   private val hasLegacyConfigs = this::class.findAnnotation<LegacyController>()!=null
   private val configs = HashMap<String, Config<Any?>>()
   override val configurableGroupPrefix: String? = null
   override val configurableValueSource by lazy {
      object: ConfigValueSource {

         @Suppress("UNCHECKED_CAST")
         override fun register(config: Config<*>) {
            val key = Widget.configToRawKeyMapper(config)
            configs[key] = config as Config<Any?>
         }

         override fun initialize(config: Config<*>) {
            if (config.isPersistable()) {
               val key = Widget.configToRawKeyMapper(config)
               val source = widget.fieldsRaw
               if (source.containsKey(key))
                  config.valueAsJson = source[key] ?: JsNull
            }
         }
      }
   }

   override fun uiRoot() = root

   override fun focus() = root.requestFocus()

   override fun close() {
      scope.cancel()
      onClose()
      io.dispose()
   }

   override fun getConfig(name: String) = configs.initializeLegacyConfigs().values.find { it.name==name }

   override fun getConfigs(): Collection<Config<Any?>> = configs.initializeLegacyConfigs().values

   private fun <T> T.initializeLegacyConfigs() = apply {
      if (!isLegacyConfigsInitialized) {
         isLegacyConfigsInitialized = true
         collectActionsOf(this@SimpleController)
         if (hasLegacyConfigs) {
            configs.putAll(this@SimpleController.toConfigurableByReflect().getConfigs().associateBy(Widget.configToRawKeyMapper).asIs())
         }
      }
   }


   /** Invoke [bind][Input.bind] on this input and the specified output if this widget [has never been serialized][Widget.isDeserialized]. */
   fun <T> Input<T>.bindIf1stLoad(output: Output<out T>) = if (widget.isDeserialized) Subscription() else bind(output)

   /** Invoke [bind][Input.bind] on this input and the specified output if [isBound(widget.id)][Input.isBoundUnless] is false. */
   fun <T> Input<T>.bindDefault(output: Output<out T>) = if (isBoundUnless(widget.id) || value!==initialValue) Subscription() else bind(output)

   /** Invoke [bind][Input.bindDefault] on this input and the specified output if both [Input.isBound(widget.id)][Input.isBoundUnless] and [Widget.isDeserialized] is false. */
   fun <T> Input<T>.bindDefaultIf1stLoad(output: Output<out T>) = if (isBoundUnless(widget.id) || value!==initialValue) Subscription() else bindIf1stLoad(output)

}

/** Denotes [Controller] that requires manual config initialization. */
annotation class LegacyController