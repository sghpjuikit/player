package sp.it.pl.layout.widget.controller

import javafx.scene.layout.StackPane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDelegator
import sp.it.util.conf.ConfigValueSource
import sp.it.util.file.div
import sp.it.util.file.toURLOrNull
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.ui.fxml.ConventionFxmlLoader
import java.util.HashMap
import kotlin.reflect.full.findAnnotation

/**
 * Base controller implementation that provides
 * - [root]
 * - support for automatic restoration of configurable properties, using delegated configurable properties, see
 * [sp.it.util.conf.cv] family of methods. This is [ConfigDelegator] and uses [widget] as [configurableValueSource].
 */
open class SimpleController(widget: Widget): Controller(widget), ConfigDelegator {

   @JvmField val root = StackPane()
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
            if (config.isEditable.isByApp) {
               val key = Widget.configToRawKeyMapper(config)
               val source = widget.fieldsRaw
               if (source.containsKey(key))
                  config.valueAsProperty = source[key]!!
            }
         }
      }
   }

   override fun loadFirstTime() = root

   override fun focus() = root.requestFocus()

   override fun close() {
      onClose()
      io.dispose()
   }

   override fun getField(name: String) = configs.initializeLegacyConfigs().values.find { it.name==name }

   override fun getFields(): Collection<Config<Any?>> = configs.initializeLegacyConfigs().values

   private fun <T> T.initializeLegacyConfigs() = apply {
      if (hasLegacyConfigs && !isLegacyConfigsInitialized) {
         isLegacyConfigsInitialized = true
         configs.putAll(super.getFields().associateBy(Widget.configToRawKeyMapper))
      }
   }


   /** Invoke [bind][Input.bind] on this input and the specified output if this widget [has never been serialized][Widget.isDeserialized]. */
   fun <T> Input<T>.bindIf1stLoad(output: Output<out T>) = if (widget.isDeserialized) Subscription() else bind(output)

   /** Invoke [bind][Input.bind] on this input and the specified output if [isBound(widget.id)][Input.isBound] is false. */
   fun <T> Input<T>.bindDefault(output: Output<out T>) = if (isBound(widget.id)) Subscription() else bind(output)

   /** Invoke [bind][Input.bindDefault] on this input and the specified output if both [Input.isBound(widget.id)][Input.isBound] and [Widget.isDeserialized] is false. */
   fun <T> Input<T>.bindDefaultIf1stLoad(output: Output<out T>) = if (isBound(widget.id)) Subscription() else bindIf1stLoad(output)

}

/** Denotes [Controller] that requires manual config initialization. */
annotation class LegacyController

/** @return [ConventionFxmlLoader] with root [SimpleController.root], specified controller and location of fxml in widget's directory */
fun fxmlLoaderForController(controller: SimpleController) = ConventionFxmlLoader(controller.root, controller).apply {
   location = (controller.location/"${controller.javaClass.simpleName}.fxml").toURLOrNull()!!
}
