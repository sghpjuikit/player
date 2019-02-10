package sp.it.pl.layout.widget.controller

import javafx.scene.layout.AnchorPane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.Inputs
import sp.it.pl.layout.widget.controller.io.Outputs
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.ConfigValueSource
import sp.it.pl.util.conf.MultiConfigurable
import sp.it.pl.util.reactive.Disposer
import java.util.HashMap

open class SimpleController(@JvmField val widget: Widget): AnchorPane(), Controller, MultiConfigurable {

    override val ownerWidget = widget
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

    override fun loadFirstTime() = this

    override fun focus() = requestFocus()

    override fun close() {
        onClose()
        inputs.getInputs().forEach { it.unbindAll() }
    }

    @Deprecated("to be removed")
    final override fun init() {}

    @Suppress("UNCHECKED_CAST")
    override fun getFieldsMap(): Map<String, Config<Any>> = configs as Map<String, Config<Any>>

}

/** Denotes [Controller] that requires manual config initialization. By convention that applies for controller written in Java. */
annotation class LegacyController