package sp.it.pl.ui.itemnode

import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.SOMETIMES
import sp.it.pl.main.appTooltip
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.util.access.v
import sp.it.util.access.vx
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.setTo
import sp.it.util.conf.AccessConfig
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.dev.fail
import sp.it.util.functional.PF
import sp.it.util.functional.Parameter
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.sizes
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.suppressingAlways
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo
import sp.it.util.type.VType
import sp.it.util.ui.hBox
import sp.it.util.ui.install
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import java.util.ArrayList

/**
 * Value node containing function.
 *
 * @param <I> type of function input
 * @param <O> type of function output
 */
class FItemNode<I, O>(functions: PrefList<PF<I, O>>): ValueNode<(I) -> O>(throwingF()) {
   private val root = hBox(5, CENTER_LEFT).apply { id = "fItemNodeRoot" }
   private val paramB = hBox(5, CENTER_LEFT).apply { id = "fItemNodeParamsRoot" }
   private val editors = ArrayList<ConfigEditor<*>>()
   private val fCB: ComboBox<PF<I, O>>
   private var avoidGenerateValue = Suppressor(false)
   var isEditableRawFunction = v(true)
   var onRawFunctionChange = { _: PF<I, O>? -> }

   init {
      avoidGenerateValue.suppressingAlways {
         fCB = SpitComboBox({ it.name })
         fCB.items setTo functions.sortedBy { it.name }
         fCB.value = functions.preferredOrFirst
         // display non-editable as label
         syncTo(isEditableRawFunction, fCB.items.sizes()) { editable, itemCount ->
            fCB.pseudoClassChanged("editable", editable && itemCount.toInt()>1)
         }
         fCB.valueProperty() sync { function ->
            editors.clear()
            paramB.children.clear()
            function?.parameters.orEmpty().forEachIndexed { i, p ->
               val editor = p.toConfig { generateValue() }.createEditor().apply {
                  if (p.description.isNotBlank())
                     editor install appTooltip(p.description)
               }
               editors += editor
               paramB.lay(if (i==0) ALWAYS else SOMETIMES) += editor.buildNode(false)
            }
            onRawFunctionChange(function)
            generateValue()
         }
      }
      generateValue()

      root.lay += fCB
      root.lay(ALWAYS) += paramB
   }

   override fun getNode() = root

   override fun focus() = editors.firstOrNull()?.focusEditor().toUnit()

   fun getTypeIn(): VType<*>? = fCB.value?.`in`

   fun getTypeOut(): VType<*>? = fCB.value?.out

   private fun generateValue() {
      avoidGenerateValue.suppressed {
         val functionRaw = fCB.value ?: null
         val parameters = editors.map { it.config.value }
         val function = functionRaw?.realize(parameters)
         if (function!=null) changeValue(function)
      }
   }

   fun clear() {
      avoidGenerateValue.suppressing {
         editors.forEach { it.refreshDefaultValue() }
      }
      generateValue()
   }

   companion object {

      private fun throwingF() = { _: Any? -> fail { "Initial function value. Must not be invoked" } }

      private fun <T> Config<T>.createEditor() = ConfigEditor.create(this)

      private fun <T> Parameter<T>.toConfig(onChange: (T?) -> Unit): Config<T> {
         val a = vx(defaultValue).apply { attach { onChange(it) } }
         return AccessConfig(type, name, name, { a.value = it }, { a.value }, "", description, EditMode.USER).addConstraints(constraints)
      }

   }
}