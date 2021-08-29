package sp.it.pl.ui.itemnode

import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.SOMETIMES
import sp.it.pl.main.F
import sp.it.pl.main.appTooltip
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.util.access.v
import sp.it.util.access.vx
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.setTo
import sp.it.util.conf.AccessConfig
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.functional.PF
import sp.it.util.functional.Parameter
import sp.it.util.functional.asIs
import sp.it.util.functional.compose
import sp.it.util.functional.ifNotNull
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.sizes
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.type.VType
import sp.it.util.type.isSubtypeOf
import sp.it.util.ui.hBox
import sp.it.util.ui.install
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged

/**
 * Value node containing function.
 *
 * @param <I> type of function input
 * @param <O> type of function output
 */
class FItemNode<I, O>(typeIn: VType<I>, typeOutTargeted: VType<O>, functionPool: (VType<*>) -> PrefList<PF<I, *>>, initialValue: F<I,O>? = null): ValueNode<F<I,O>?>(initialValue) {
   val typeIn: VType<I> = typeIn
   val typeOutTargeted: VType<O> = typeOutTargeted
   val typeOut: VType<O>? get() = fs.lastOrNull()?.takeIf { it.isTerminal() }?.fCB?.value?.out?.asIs()
   private val functionPool: (VType<*>) -> PrefList<PF<I, *>> = functionPool
   private val root = hBox(5, CENTER_LEFT).apply { id = "fItemNodeRoot" }
   private val mapperNodes = hBox(5, CENTER_LEFT).apply { id = "fItemNodeMappersRoot" }
   private val editorsNodes = hBox(5, CENTER_LEFT).apply { id = "fItemNodeParamsRoot" }
   private val fs = ArrayList<FItem>()

   private var avoidGenerateValue = Suppressor(false)
   var isEditableRawFunction = v(true)
   var onRawFunctionChange = { }

   init {
      root.lay += mapperNodes
      root.lay(ALWAYS) += editorsNodes
      newFItem()
   }

   override fun getNode() = root

   override fun focus() {
      fs.lastOrNull().ifNotNull {
         if (it.editors.isNotEmpty()) it.editors.firstOrNull()?.focusEditor()
         else it.fCB.requestFocus()
      }
   }

   private fun newFItem() {
      avoidGenerateValue.suppressing {
         var isInitializing = true
         FItem().apply {

            val functions = functionPool(fs.lastOrNull()?.fCB?.value?.out ?: typeIn)

            fs += this
            mapperNodes.children += fCB

            fCB.items setTo functions.sortedBy { it.name }
            fCB.value = functions.preferredOrFirst
            // display non-editable as label
            isEditableRawFunction zip fCB.items.sizes() sync { (editable, itemCount) ->
               fCB.pseudoClassChanged("editable", editable && itemCount.toInt()>1)
            }
            fCB.valueProperty() sync { function ->
               val isTerminal = isTerminal()
               editorsNodes.children.clear()
               editors.clear()
               if (isTerminal) {
                  function?.parameters.orEmpty().forEach { p ->
                     val editor = p.toConfig { generateValue() }.createEditor().apply {
                        if (p.description.isNotBlank())
                           editor install appTooltip(p.description)
                     }
                     editors += editor
                     editorsNodes.lay(SOMETIMES) += editor.buildNode(false)
                  }
               } else {
                  val i = mapperNodes.children.indexOf(fCB) + 1
                  fs setTo fs.take(i)
                  mapperNodes.children setTo mapperNodes.children.take(i)
                  if (!isInitializing && function!=null) newFItem()
               }
               if (fCB.isFocused) focus()
               onRawFunctionChange()
               generateValue()
            }
         }
         isInitializing = false
      }
      generateValue()
   }

   private fun FItem.isTerminal() = fCB.value?.out?.isSubtypeOf(typeOutTargeted)==true

   private fun generateValue() {
      avoidGenerateValue.suppressed {
         val isTerminal = fs.lastOrNull()?.isTerminal() == true
         val functions = fs.mapNotNull { it.realize() }
         val isNonNull = functions.size == fs.size
         val function = functions.reduceOrNull { mapper, getter -> mapper compose getter }
         if (isTerminal && isNonNull && function!=null) changeValue(function.asIs())
      }
   }

   fun clear() {
      avoidGenerateValue.suppressing {
         fs setTo fs.take(1)
         fs.forEach { it.editors.forEach { it.refreshDefaultValue() } }
      }
      generateValue()
   }

   data class FItem(
      val editors: MutableList<ConfigEditor<*>> = ArrayList(),
      val fCB: ComboBox<PF<*, *>> = SpitComboBox({ it.name })
   ) {
      fun realize(): F<Any?, Any?>? {
         val functionRaw = fCB.value ?: null
         val parameters = editors.map { it.config.value }
         return functionRaw?.realize(parameters).asIs()
      }
   }

   companion object {

      private fun <T> Config<T>.createEditor() = ConfigEditor.create(this)

      private fun <T> Parameter<T>.toConfig(onChange: (T?) -> Unit): Config<T> {
         val a = vx(defaultValue).apply { attach { onChange(it) } }
         return AccessConfig(type, name, name, { a.value = it }, { a.value }, "", description, EditMode.USER).addConstraints(constraints)
      }

   }
}