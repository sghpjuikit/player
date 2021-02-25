package sp.it.util.type

import kotlin.reflect.KProperty

/** Allows defining write-only properties by having the property return this type */
class WriteOnlyPropertyOperator<T, R>(private val invoker: (T) -> R) {

   /** Sets this property. Invoke style. */
   operator fun invoke(it: T): R = invoker(it)

   /** Sets this property. Map style. */
   operator fun get(it: T): R = invoke(it)

   /** Sets this property. Infix style. */
   infix fun setTo(value: T): R = invoke(value)

   /** Sets this property. Value style. */
   operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): R = invoke(value)

}