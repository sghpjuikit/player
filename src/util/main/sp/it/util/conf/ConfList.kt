package sp.it.util.conf

import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import sp.it.util.conf.ConfList.Companion.FailFactory
import sp.it.util.dev.fail
import sp.it.util.type.VType

private typealias ItemToConf<T> = (T) -> Configurable<*>
private typealias ItemFac<T> = () -> T

/** [ObservableList] wrapper that provides element type and factory information for user editable lists. */
open class ConfList<T> private constructor(
   val itemType: VType<T>,
   val itemFactory: ItemFac<T>?,
   val itemToConfigurable: ItemToConf<T?>,
   val list: ObservableList<T>
) {
   val isSimpleItemType = itemFactory==null

   constructor(itemType: VType<T>, items: ObservableList<T> = observableArrayList()):
      this(itemType, null, computeSimpleItemToConfigurable<T>(itemType), items)

   @Suppress("UNCHECKED_CAST")
   constructor(itemType: VType<T>, itemFactory: ItemFac<T>?, itemToConfigurable: ItemToConf<T>, vararg items: T):
      this(itemType, itemFactory, { if (!itemType.isNullable && it==null) fail { "Must not be null" }; itemToConfigurable(it as T) }, observableArrayList<T>(*items))

   companion object {

      object FailFactory: () -> Nothing {
         override fun invoke() = fail { "Marking factory. Must not be invoked" }
      }

      private fun <T> computeSimpleItemToConfigurable(itemType: VType<T>): ItemToConf<T?> = { Config.forValue(itemType, "Item", it) }
   }
}

/** [ConfList] with fixed size, but editable elements. */
class FixedConfList<T: Configurable<*>>(itemType: Class<T>, isNullable: Boolean, vararg items: T): ConfList<T>(VType(itemType, isNullable), FailFactory, { it }, *items)
