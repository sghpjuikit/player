package sp.it.pl.ui.item_node

import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.core.CoreFunctors.pool
import sp.it.pl.main.AppTexts
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.setTo
import sp.it.util.functional.PF
import sp.it.util.functional.TypeAwareF
import sp.it.util.functional.Util
import sp.it.util.functional.Util.IS
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.type.VType
import sp.it.util.type.notnull
import sp.it.util.type.type
import sp.it.util.ui.lay

/** Filter node producing [sp.it.util.access.fieldvalue.ObjectField] predicate. */
class FieldedPredicateItemNode<V, F: ObjectField<V, *>?>: ValueNode<Predicate<V>?> {

   private val typeCB: ComboBox<PredicateData<F>?>
   private var config: FItemNode<Any?, Boolean>? = null
   private val negB: CheckIcon
   private val root: HBox
   private var inconsistentState = false

   @JvmOverloads
   constructor(
      predicatePool: (VType<*>) -> PrefList<PF<Any?, *>> = { typeInRaw ->
         val typeB = VType(Boolean::class.java, false)
         val typeIn = typeInRaw.notnull()
         val fsI = pool.getI(typeIn)
         val fsIO = pool.getIO(typeIn, VType(Boolean::class.java, false))
         fsI.removeIf { !(it.out==typeB || it.parameters.isEmpty()) }
         if (fsI has fsIO.preferred) fsI.preferred = fsIO.preferred
         fsI.asIs()
      }
   ): super(IS.asIs()) {
      this.typeCB = SpitComboBox({ it.name }, AppTexts.textNoVal)
      this.negB = CheckIcon(false).styleclass("filter-negate-icon") as CheckIcon
      this.root = HBox(5.0, negB, typeCB)
      root.alignment = Pos.CENTER_LEFT
      typeCB.visibleRowCount = 25
      typeCB.valueProperty().attach {
         if (inconsistentState) return@attach
         root.lay -= config?.getNode()
         config = FItemNode(it!!.type, type<Boolean>(), predicatePool, null).apply {
            onItemChange = Consumer { generatePredicate() }
         }
         root.lay(ALWAYS) += config!!.getNode()
         generatePredicate()
      }
      negB.selected.attach { generatePredicate() }
      negB.tooltip(Tooltip("Negate"))
   }

   /** Initially selected value. Supplier can be null or return null, in which case 1st value is selected. Default null  */
   var prefTypeSupplier: Supplier<PredicateData<F>>? = null

   var isEmpty = true
      private set

   /**
    * Sets combo box data specifying what filter can be generated in form of list
    * of tri-tuples : displayed name, class, passed object.
    *
    * The name is what will be displayed in the combobox to choose from
    * The class specifies the type of object the filter is generated for.
    * The passed object's purpose is to be returned along with the filter, mostly to be used in the generated filter
    *
    * If there is no object to pass, use null.
    */
   fun setData(classes: List<PredicateData<F>>) {
      val cs = classes.asSequence().sortedBy { it.name }.toList()
      inconsistentState = true
      typeCB.items setTo cs
      inconsistentState = false
      val v = prefTypeSupplier?.get()?.let { (_, _, value) -> cs.find { it.value==value } } ?: cs.firstOrNull()
      typeCB.setValue(v)
      isEmpty = true // we can do this since we know we are in 'default' state
   }

   /** Focuses the filter's first parameter's config field if any. */
   override fun focus() = config?.focus().toUnit()

   fun clear() {
      inconsistentState = true
      config?.clear()
      inconsistentState = false
      changeValue(IS)
      isEmpty = true
   }

   private fun generatePredicate() {
      if (inconsistentState) return
      isEmpty = config==null
      if (config==null) {
         changeValue(IS)
      } else {
         val p = config!!.value
         val o = typeCB.value?.value
         if (p!=null && o!=null) {
            var pr = predicate(o, p)
            if (negB.selected.value) pr = pr.negate()
            changeValue(pr)
         }
      }
   }

   override fun getNode() = root

   private fun <V, T> predicate(field: ObjectField<V, T>, f: (T) -> Boolean): Predicate<V> =
      when {
         f===Util.IS0 || f===Util.ISNT0 || f===IS || f===Util.ISNT || (f is TypeAwareF<*, *> && f.typeIn.isNullable) -> Predicate { f(field.getOf(it)) }
         else -> Predicate { element: V ->
            val o = field.getOf(element)
            o!=null && f(o)
         }
      }

   @JvmRecord
   data class PredicateData<T>(val name: String, val type: VType<*>, val value: T) {
      companion object {
         fun <V, T> ofField(field: ObjectField<V, T>): PredicateData<ObjectField<V, T>> = PredicateData(field.name(), field.type, field)
      }
   }

}