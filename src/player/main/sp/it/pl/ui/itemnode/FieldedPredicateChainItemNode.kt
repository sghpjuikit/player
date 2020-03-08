package sp.it.pl.ui.itemnode

import javafx.beans.value.WritableValue
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.F
import javafx.scene.input.KeyEvent
import sp.it.pl.ui.itemnode.FieldedPredicateItemNode.PredicateData
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.functional.Util.IS
import sp.it.util.functional.asIs
import sp.it.util.ui.isAnyParentOf
import java.lang.Integer.MAX_VALUE
import java.util.ArrayList
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.streams.asSequence

/** [ObjectField] [Predicate] chain. */
open class FieldedPredicateChainItemNode<T, F: ObjectField<T, Any?>>(
   chainedFactory: (FieldedPredicateChainItemNode<T, F>) -> FieldedPredicateItemNode<T, F>
): ChainValueNode<Predicate<T>, FieldedPredicateItemNode<T, F>, Predicate<T>>(0, MAX_VALUE, IS.asIs(), null) {

   constructor(): this({
      FieldedPredicateItemNode<T, F>().apply {
         setPrefTypeSupplier(it.prefTypeSupplier)
         setData(it.data)
      }
   })

   var prefTypeSupplier: Supplier<PredicateData<F>>? = null
      set(supplier) {
         field = supplier
         chain.forEach { it.chained.setPrefTypeSupplier(supplier) }
      }

   var data: List<PredicateData<F>> = ArrayList()
      set(data) {
         inconsistentState = true
         field = data
         chain.forEach { it.chained.setData(data) }
         clear()  // bug fix, not sure if it does not cause problems
      }

   init {
      this.chainedFactory = Supplier { chainedFactory(this) }
      inconsistentState = false
      generateValue()
   }

   fun isEmpty(): Boolean = chain.all { it.chained.isEmpty }

   fun clear() {
      inconsistentState = true
      convergeTo(1);
      chain.forEach { it.chained.clear() }
      inconsistentState = false
      generateValue()
   }

   override fun reduce(values: Stream<Predicate<T>>): Predicate<T> = values.asSequence().fold(IS.asIs(), Predicate<T>::and)

   fun buildToggleOnKeyHandler(filterVisible: WritableValue<Boolean>, owner: Node) = EventHandler<KeyEvent> { e ->
      when (e.code) {
         F -> {
            if (e.isShortcutDown) {
               if (filterVisible.value) {
                  val hasFocus = root.scene?.focusOwner?.let { root.isAnyParentOf(it) }==true
                  if (hasFocus) {
                     filterVisible.value = !filterVisible.value
                     if (!filterVisible.value) owner.requestFocus()
                  } else {
                     focus()
                  }
               } else {
                  filterVisible.value = true
               }
               e.consume()
            }
         }
         ESCAPE -> {
            if (filterVisible.value) {
               if (isEmpty()) {
                  filterVisible.value = false
                  owner.requestFocus()
               } else {
                  clear()
               }
               e.consume()
            }
         }
         else -> {}
      }
   }

}