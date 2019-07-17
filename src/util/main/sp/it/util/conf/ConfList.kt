package sp.it.util.conf

import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import sp.it.util.type.isSuperclassOf
import java.util.function.Supplier

private typealias ConfFactory<T> = (T) -> Configurable<*>

/** [ObservableList] wrapper that provides element type and factory information for user editable lists. */
open class ConfList<T>(
   @JvmField val itemType: Class<T>,
   @JvmField val factory: Supplier<out T?>,
   @JvmField val toConfigurable: ConfFactory<T>,
   @JvmField val isNullable: Boolean,
   @JvmField val list: ObservableList<T>
) {

   constructor(itemType: Class<T>, isNullable: Boolean): this(itemType, NullFactory, computeDefaultToConfigurable<T>(itemType), isNullable)

   constructor(itemType: Class<T>, isNullable: Boolean, items: ObservableList<T>): this(itemType, NullFactory, computeDefaultToConfigurable<T>(itemType), isNullable, items)

   @SafeVarargs
   constructor(itemType: Class<T>, factory: Supplier<out T?>, toConfigurable: ConfFactory<T>, isNullable: Boolean, vararg items: T): this(itemType, factory, toConfigurable, isNullable, observableArrayList<T>(*items))

   @Suppress("ObjectLiteralToLambda")
   companion object {

      @JvmField val FailFactory = object: Supplier<Nothing?> {
         override fun get() = null
      }
      @JvmField val NullFactory = object: Supplier<Nothing?> {
         override fun get() = null
      }

      private fun <T> computeDefaultToConfigurable(itemType: Class<T>): ConfFactory<T> = when {
         Configurable::class.isSuperclassOf(itemType) -> { it -> it as Configurable<*> }
         else -> { it -> Config.forValue<T>(itemType, "Item", it as Any?) }
      }
   }
}

/** [ConfList] with fixed size, but editable elements. */
class FixedConfList<T: Configurable<*>>(itemType: Class<T>, vararg items: T): ConfList<T>(itemType, FailFactory, { it }, false, *items)
