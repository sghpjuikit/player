package gui.pane

import gui.itemnode.ConfigField
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.text.TextAlignment
import layout.widget.feature.ConfiguringFeature
import util.conf.Config
import util.conf.Configurable
import util.graphics.hBox
import util.graphics.setMinPrefMaxWidth
import java.util.stream.Stream

class ConfigPane<T: Any>: FlowPane, ConfiguringFeature {
    private var fields: List<ConfigField<T>> = listOf()
    @JvmField var onChange: Runnable? = null

    @SafeVarargs
    constructor(vararg configs: Config<T>): this(configs.asList())

    constructor(configs: Configurable<T>): this(configs.fields)

    constructor(configs: Collection<Config<T>>): super(5.0, 5.0) {
        configure(configs)
    }

    override fun configure(configs: Collection<Config<*>>?) {
        if (configs==null) return

        fields = configs.asSequence()
                .sortedBy { it.guiName.toLowerCase() }
                .map {
                    ConfigField.create(it as Config<T>).apply {
                        onChange = Runnable { this@ConfigPane.onChange?.run() }
                    }
                }
                .toList()

        children.clear()
        children += fields.map {
            hBox {
                spacing = 20.0
                alignment = CENTER_LEFT
                children += it.createLabel().apply {
                    setMinPrefMaxWidth(250.0)
                    alignment = CENTER_RIGHT
                    textAlignment = TextAlignment.RIGHT
                    padding = Insets(0.0, 0.0, 0.0, 5.0)
                }
                children += it.node
                HBox.setHgrow(it.node, ALWAYS)
            }
        }
    }

    fun getConfigFields(): List<ConfigField<T>> = fields

    fun getConfigValues(): Stream<T> = getConfigFields().stream().map { it.getValue() }

}