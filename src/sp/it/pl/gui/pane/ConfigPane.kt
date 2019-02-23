package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.Label
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.functional.supplyIf
import sp.it.pl.util.graphics.Util.computeFontWidth
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.lay

class ConfigPane<T: Any?>: VBox, ConfiguringFeature {
    private var fields: List<ConfigField<*>> = listOf()
    private val labels = ArrayList<Label>()
    private var needsLabel: Boolean = true
    var onChange: Runnable? = null
    val configOrder = compareBy<Config<*>> { 0 }
            .thenBy { it.group.toLowerCase() }
            .thenBy { if (it.type==Action::javaClass) 1.0 else -1.0 }
            .thenBy { it.guiName.toLowerCase() }

    constructor(): super(5.0)

    constructor(configs: Configurable<T>): this() {
        configure(configs)
    }

    override fun configure(configurable: Configurable<*>?) {
        needsLabel = configurable !is Config<*>
        fields = configurable?.fields.orEmpty().asSequence()
                .sortedWith(configOrder)
                .map {
                    ConfigField.create(it).apply {
                        onChange = this@ConfigPane.onChange
                    }
                }
                .toList()

        alignment = CENTER
        labels.clear()
        children setTo fields.map {
            hBox(20, CENTER_LEFT) {
                lay += supplyIf(needsLabel) {
                    it.createLabel().apply {
                        labels += this
                        alignment = CENTER_RIGHT
                        textAlignment = TextAlignment.RIGHT
                        padding = Insets(0.0, 0.0, 0.0, 5.0)
                    }
                }

                lay(ALWAYS) += it.getNode()
            }
        }
    }

    private var inLayout = false

    @Suppress("SENSELESS_COMPARISON")
    override fun requestLayout() {
        if (!inLayout && labels!=null) {
            inLayout = true
            setTightLabelWidth()
            inLayout = false
        }
        super.requestLayout()
    }

    private fun setTightLabelWidth() {
        val labelWidth = labels.asSequence().map {  it.snappedLeftInset() + computeFontWidth(it.font, it.text) + it.snappedRightInset() }.max() ?: 0.0
        labels.forEach { it.prefWidth = labelWidth }
    }

    @Suppress("UNCHECKED_CAST")
    fun getConfigFields(): List<ConfigField<T>> = fields as List<ConfigField<T>>

    fun getConfigValues(): List<T> = getConfigFields().map { it.getVal() }

    fun focusFirstConfigField() = fields.firstOrNull()?.focus()
}