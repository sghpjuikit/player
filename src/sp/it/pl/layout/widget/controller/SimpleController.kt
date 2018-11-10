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

open class SimpleController(@JvmField val widget: Widget<*>): AnchorPane(), Controller, MultiConfigurable {

    @JvmField val onClose = Disposer()
    @JvmField val outputs = Outputs()
    @JvmField val inputs = Inputs()
    private val configs = HashMap<String, Config<*>>()
    override val configurableDiscriminant = null as String?
    override val configurableValueStore: ConfigValueSource by lazy {
        object: ConfigValueSource {
            private val configToRawKeyMapper = Config<*>::getName

            override fun register(config: Config<*>) {
                val key = configToRawKeyMapper(config)
                configs[key] = config
            }

            @Suppress("UNCHECKED_CAST")
            override fun initialize(config: Config<*>) {
                if (config.isEditable.isByApp) {
                    val source: Map<String, String> = widget.properties["configs"] as Map<String, String>? ?: mapOf()
                    val key = configToRawKeyMapper(config)
                    if (source.containsKey(key))
                        config.valueS = source[key]
                }
            }
        }
    }

    override fun getOwnerWidget(): Widget<*> = widget

    override fun refresh() {}

    override fun close() {
        onClose()
        inputs.getInputs().forEach { it.unbindAll() }
    }

    override fun getOwnedOutputs() = outputs

    override fun getOwnedInputs() = inputs

    @Suppress("UNCHECKED_CAST")
    override fun getFieldsMap(): Map<String, Config<Any>> = configs as Map<String, Config<Any>>

}