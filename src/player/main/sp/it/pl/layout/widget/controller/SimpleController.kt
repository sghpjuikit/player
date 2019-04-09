package sp.it.pl.layout.widget.controller

import javafx.scene.layout.StackPane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.Inputs
import sp.it.pl.layout.widget.controller.io.Outputs
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigValueSource
import sp.it.util.conf.MultiConfigurable
import sp.it.util.file.div
import sp.it.util.file.toURLOrNull
import sp.it.util.reactive.Disposer
import sp.it.util.ui.fxml.ConventionFxmlLoader
import java.util.HashMap

/**
 * Base controller implementation that provides
 * - [root]
 * - support for automatic restoration of configurable properties, using delegated configurable properties, see
 * [sp.it.util.conf.cv] family of methods. This is [MultiConfigurable] and uses [widget] as [configurableValueStore].
 */
open class SimpleController(widget: Widget): Controller(widget), MultiConfigurable {

    @JvmField val root = StackPane()
    @JvmField val onClose = Disposer()
    @JvmField val outputs = Outputs()
    @JvmField val inputs = Inputs()
    override val ownedOutputs = outputs
    override val ownedInputs = inputs
    private val configs = HashMap<String, Config<*>>()
    override val configurableDiscriminant = null as String?
    override val configurableValueStore: ConfigValueSource by lazy {
        object: ConfigValueSource {

            override fun register(config: Config<*>) {
                val key = Widget.configToRawKeyMapper(config)
                configs[key] = config
            }

            @Suppress("UNCHECKED_CAST")
            override fun initialize(config: Config<*>) {
                if (config.isEditable.isByApp) {
                    val source: Map<String, String> = widget.properties["configs"] as Map<String, String>? ?: mapOf()
                    val key = Widget.configToRawKeyMapper(config)
                    if (source.containsKey(key))
                        config.valueS = source[key]
                }
            }
        }
    }

    override fun loadFirstTime() = root

    override fun focus() = root.requestFocus()

    override fun close() {
        onClose()
        inputs.getInputs().forEach { it.unbindAll() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getFieldsMap(): Map<String, Config<Any>> = configs as Map<String, Config<Any>>

}

/** Denotes [Controller] that requires manual config initialization. */
annotation class LegacyController

/** @return [ConventionFxmlLoader] with root [SimpleController.root], specified controller and location of fxml in widget's directory */
fun fxmlLoaderForController(controller: SimpleController) = ConventionFxmlLoader(controller.root, controller).apply {
    location = (controller.location/"${controller.javaClass.simpleName}.fxml").toURLOrNull()!!
}