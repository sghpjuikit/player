package sp.it.pl.gui.itemnode

import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.SOMETIMES
import sp.it.pl.gui.objects.combobox.ImprovedComboBox
import sp.it.util.access.vn
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Config.AccessorConfig
import sp.it.util.functional.Functors
import sp.it.util.functional.Functors.PƑ
import sp.it.util.functional.Functors.Ƒ1
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import java.util.ArrayList
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Value node containing function.
 *
 * @param <I> type of function input
 * @param <O> type of function output
 */
class FItemNode<I, O>(functionPool: Supplier<PrefList<PƑ<in I, out O>>>): ValueNode<Ƒ1<in I, out O>?>(null) {
    private val root = hBox(5, CENTER_LEFT).apply { id = "fItemNodeRoot" }
    private val paramB = hBox(5, CENTER_LEFT).apply { id = "fItemNodeParamsRoot" }
    private val configs = ArrayList<ConfigField<*>>()
    private val fCB: ComboBox<PƑ<in I, out O>>
    private var inconsistentState = false

    init {
        val functions = functionPool.get()

        inconsistentState = true
        fCB = ImprovedComboBox { it.name }
        fCB.items setTo functions.asSequence().sortedBy { it.name }
        fCB.value = functions.preferredOrFirst
        fCB.valueProperty() sync { function ->
            configs.clear()
            paramB.children.clear()
            function.parameters.forEachIndexed { i, p ->
                val editor = p.toConfig { generateValue() }.toConfigField()
                configs += editor
                paramB.lay(if (i==0) ALWAYS else SOMETIMES) += editor.getNode(false)
            }
            generateValue()
        }
        inconsistentState = false
        generateValue()

        root.lay += fCB
        root.lay(ALWAYS) += paramB
    }

    override fun getVal() = super.getVal()!!

    override fun getNode() = root

    override fun focus() {
        configs.firstOrNull()?.focusEditor()
    }

    fun getTypeIn(): Class<*> = fCB.value?.`in` ?: Void::class.java

    fun getTypeOut(): Class<*> = fCB.value?.out ?: Void::class.java

    private fun generateValue() {
        if (inconsistentState) return
        val functionRaw = fCB.value
        val parameters = configs.map { it.getConfigValue() }
        val function = functionRaw.toƑ1(parameters)
        changeValue(function)
    }

    fun clear() {
        inconsistentState = true
        configs.forEach { it.setNapplyDefault() }
        inconsistentState = false
        generateValue()
    }

    companion object {

        private fun <T> Config<T>.toConfigField() = ConfigField.create(this)

        private fun <T> Functors.Parameter<T>.toConfig(onChange: (T?) -> Unit): Config<T> {
            val a = vn(defaultValue).apply { attach { onChange(it) } }
            return AccessorConfig(type, name, description, Consumer { a.value = it }, Supplier { a.value })
        }

    }
}