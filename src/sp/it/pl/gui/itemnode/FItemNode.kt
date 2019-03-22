package sp.it.pl.gui.itemnode

import javafx.scene.control.ComboBox
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.gui.objects.combobox.ImprovedComboBox
import sp.it.pl.util.access.initAttach
import sp.it.pl.util.access.vn
import sp.it.pl.util.collections.list.PrefList
import sp.it.pl.util.collections.setTo
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Config.AccessorConfig
import sp.it.pl.util.functional.Functors
import sp.it.pl.util.functional.Functors.PƑ
import sp.it.pl.util.functional.Functors.Ƒ1
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.reactive.sync
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
    private val root = hBox(5)
    private val paramB = hBox(5)
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
            function.parameters.forEach { p ->
                val editor = p.toConfig { generateValue() }.toConfigField()
                configs += editor
                paramB.children += editor.getNode()
            }
            if (!configs.isEmpty()) HBox.setHgrow(configs[configs.size-1].getNode(), ALWAYS)
            generateValue()
        }
        inconsistentState = false
        generateValue()

        root.children += listOf(fCB, paramB)
    }

    override fun getVal() = super.getVal()!!

    override fun getNode() = root

    override fun focus() {
        configs.firstOrNull()?.focus()
    }

    fun getTypeIn(): Class<*> = fCB.value?.`in` ?: Void::class.java

    fun getTypeOut(): Class<*> = fCB.value?.out ?: Void::class.java

    private fun generateValue() {
        if (inconsistentState) return
        val functionRaw = fCB.value
        val parameters = configs.map { it.getVal() }
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
            val a = vn(defaultValue).initAttach { onChange(it) }
            return AccessorConfig(type, name, description, Consumer { a.value = it }, Supplier { a.value })
        }

    }
}