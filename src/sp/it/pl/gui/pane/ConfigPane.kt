package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.util.access.v
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.reactive.sync

class ConfigPane<T: Any>: VBox, ConfiguringFeature {
    private var fields: List<ConfigField<*>> = listOf()
    var onChange: Runnable? = null
    var labelWidth = v(250.0)
    val configOrder = compareBy<Config<*>> { 0 }
            .thenBy { if (it.type==Action::javaClass) 1.0 else -1.0 }
            .thenBy { it.guiName.toLowerCase() }

    @SafeVarargs
    constructor(vararg configs: Config<T>): this(configs.asList())

    constructor(configs: Configurable<T>): this(configs.fields)

    constructor(configs: Collection<Config<T>>): super(5.0) {
        configure(configs)
    }

    override fun configure(configurable: Configurable<*>?) {
        fields = configurable?.fields.orEmpty().asSequence()
                .sortedWith(configOrder)
                .map {
                    ConfigField.create(it).apply {
                        onChange = this@ConfigPane.onChange
                    }
                }
                .toList()

        alignment = CENTER
        children setTo fields.map {
            hBox(20, CENTER_LEFT) {
                lay += it.createLabel().apply {
                    labelWidth sync ::setPrefWidth
                    alignment = CENTER_RIGHT
                    textAlignment = TextAlignment.RIGHT
                    padding = Insets(0.0, 0.0, 0.0, 5.0)
                }

                lay(ALWAYS) += it.getNode()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getConfigFields(): List<ConfigField<T>> = fields as List<ConfigField<T>>

    fun getConfigValues(): List<T> = getConfigFields().map { it.getVal() }

}