package sp.it.util.conf

import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import sp.it.util.conf.ConfList.Companion.FailFactory
import sp.it.util.dev.fail

private typealias ItemToConf<T> = (T) -> Configurable<*>
private typealias ItemFac<T> = () -> T

/** [ObservableList] wrapper that provides element type and factory information for user editable lists. */
open class ConfList<T> private constructor(
   @JvmField val itemType: Class<T>,
   @JvmField val itemFactory: ItemFac<T>?,
   @JvmField val itemToConfigurable: ItemToConf<T?>,
   @JvmField val isNullable: Boolean,
   @JvmField val list: ObservableList<T>
) {
   val isSimpleItemType: Boolean = itemFactory==null

   constructor(itemType: Class<T>, isNullable: Boolean, items: ObservableList<T> = observableArrayList<T>()):
      this(itemType, null, computeSimpleItemToConfigurable<T>(itemType), isNullable, items)

   @Suppress("UNCHECKED_CAST")
   constructor(itemType: Class<T>, itemFactory: ItemFac<T>?, itemToConfigurable: ItemToConf<T>, isNullable: Boolean, vararg items: T):
      this(itemType, itemFactory, { if (!isNullable && it==null) fail { "Must not be null" }; itemToConfigurable(it as T) }, isNullable, observableArrayList<T>(*items))

   companion object {

      object FailFactory: () -> Nothing {
         override fun invoke() = fail { "Marking factory. Must not be invoked" }
      }

      private fun <T> computeSimpleItemToConfigurable(itemType: Class<T>): ItemToConf<T?> = { Config.forValue<T>(itemType, "Item", it) }
   }
}

/** [ConfList] with fixed size, but editable elements. */
class FixedConfList<T: Configurable<*>>(itemType: Class<T>, vararg items: T): ConfList<T>(itemType, FailFactory, { it }, false, *items)
