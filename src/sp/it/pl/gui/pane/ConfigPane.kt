package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.reactive.sync
import java.util.stream.Stream

class ConfigPane<T: Any>: VBox, ConfiguringFeature<T> {
    private var fields: List<ConfigField<out T>> = listOf()
    var onChange: Runnable? = null
    var labelWidth = v(250.0)

    @SafeVarargs
    constructor(vararg configs: Config<T>): this(configs.asList())

    constructor(configs: Configurable<T>): this(configs.fields)

    constructor(configs: Collection<Config<T>>): super(5.0) {
        configure(configs)
    }

    override fun configure(configurable: Collection<Config<out T>>) {
        fields = configurable.asSequence()
                .sortedBy { it.guiName.toLowerCase() }
                .map {
                    ConfigField.create(it).apply {
                        onChange = this@ConfigPane.onChange
                    }
                }
                .toList()

        children.clear()
        children += fields.map {
            hBox {
                spacing = 20.0
                alignment = CENTER_LEFT
                children += it.createLabel().apply {
                    labelWidth sync ::setPrefWidth
                    alignment = CENTER_RIGHT
                    textAlignment = TextAlignment.RIGHT
                    padding = Insets(0.0, 0.0, 0.0, 5.0)
                }
                children += it.getNode()
                HBox.setHgrow(it.getNode(), ALWAYS)
            }
        }
    }

    fun getConfigFields(): List<ConfigField<out T>> = fields

    fun getConfigValues(): Stream<T> = getConfigFields().stream().map { it.getVal() }

}