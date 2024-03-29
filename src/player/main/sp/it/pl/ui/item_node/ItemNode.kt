package sp.it.pl.ui.item_node

import java.util.function.Consumer
import javafx.scene.Node
import sp.it.util.functional.invoke

/** Graphics with a value, usually an ui editor. */
interface ItemNode<out T> {

   /** @return current value */
   val value: T

   /** @return the root node */
   fun getNode(): Node

   /** Focuses this node's content, usually a primary input field. */
   fun focus() {}
}

/** Item node which directly holds the value */
abstract class ValueNode<T>(initialValue: T): ItemNode<T> {
   override var value: T = initialValue
      protected set

   /** Value change handler invoked when value changes, consuming the new value. */
   @JvmField var onItemChange: Consumer<T> = Consumer {}

   /** Sets value if not same as current & fires itemChange if available. Internal use only. */
   protected open fun changeValue(nv: T) {
      if (value===nv) return
      value = nv
      onItemChange(nv)
   }

   /** Sets value & fires itemChange if available. Internal use only. */
   protected open fun changeValueAlways(nv: T) {
      value = nv
      onItemChange(nv)
   }
}