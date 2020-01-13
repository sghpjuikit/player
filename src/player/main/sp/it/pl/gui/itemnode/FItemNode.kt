package sp.it.pl.gui.itemnode

import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.SOMETIMES
import sp.it.pl.gui.objects.combobox.ImprovedComboBox
import sp.it.util.access.vx
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.setTo
import sp.it.util.conf.AccessConfig
import sp.it.util.conf.Config
import sp.it.util.dev.fail
import sp.it.util.functional.Functors
import sp.it.util.functional.Functors.F1
import sp.it.util.functional.Functors.PF
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import java.util.ArrayList
import java.util.function.Supplier

/**
 * Value node containing function.
 *
 * @param <I> type of function input
 * @param <O> type of function output
 */
class FItemNode<I, O>(functionPool: Supplier<PrefList<PF<in I, out O>>>): ValueNode<F1<in I, out O>>(throwingF()) {
   private val root = hBox(5, CENTER_LEFT).apply { id = "fItemNodeRoot" }
   private val paramB = hBox(5, CENTER_LEFT).apply { id = "fItemNodeParamsRoot" }
   private val editors = ArrayList<ConfigEditor<*>>()
   private val fCB: ComboBox<PF<in I, out O>>
   private var inconsistentState = false

   init {
      val functions = functionPool.get()

      inconsistentState = true
      fCB = ImprovedComboBox { it.name }
      fCB.items setTo functions.asSequence().sortedBy { it.name }
      fCB.value = functions.preferredOrFirst
      fCB.valueProperty() sync { function ->
         editors.clear()
         paramB.children.clear()
         function.parameters.forEachIndexed { i, p ->
            val editor = p.toConfig { generateValue() }.createEditor()
            editors += editor
            paramB.lay(if (i==0) ALWAYS else SOMETIMES) += editor.buildNode(false)
         }
         generateValue()
      }
      inconsistentState = false
      generateValue()

      root.lay += fCB
      root.lay(ALWAYS) += paramB
   }

   override fun getNode() = root

   override fun focus() = editors.firstOrNull()?.focusEditor().toUnit()

   fun getTypeIn(): Class<*> = fCB.value?.`in` ?: Void::class.java

   fun getTypeOut(): Class<*> = fCB.value?.out ?: Void::class.java

   private fun generateValue() {
      if (inconsistentState) return
      val functionRaw = fCB.value
      val parameters = editors.map { it.config.value }
      val function = functionRaw.toF1(parameters)
      changeValue(function)
   }

   fun clear() {
      inconsistentState = true
      editors.forEach { it.refreshDefaultValue() }
      inconsistentState = false
      generateValue()
   }

   companion object {

      private fun throwingF() = F1<Any?, Nothing> { fail { "Initial function value. Must not be invoked" } }

      private fun <T> Config<T>.createEditor() = ConfigEditor.create(this)

      private fun <T> Functors.Parameter<T>.toConfig(onChange: (T?) -> Unit): Config<T> {
         val a = vx(defaultValue).apply { attach { onChange(it) } }
         return AccessConfig<T>(type, name, description, { a.value = it }, { a.value })
      }

   }
}