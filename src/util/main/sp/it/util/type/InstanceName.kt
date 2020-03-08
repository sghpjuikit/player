package sp.it.util.type

import sp.it.util.collections.map.KClassMap
import sp.it.util.functional.asIs
import java.util.Objects
import kotlin.reflect.KClass

class InstanceName {
   private val names = KClassMap<(Any?) -> String>()

   /**
    * Registers name function for specified class and all its subclasses that do not have any name registered.
    *
    * @param c type to add name function to. Use [Nothing] class to handle null (since only null can be [Nothing]).
    * @param parser instance to instance name transformer function
    */
   fun <T: Any> add(c: KClass<T>, parser: (T) -> String) {
      names[c] = parser.asIs()
   }

   /**
    * Returns name/string representation of the object instance. If none is provided, [Objects.toString] is used.
    *
    * @param instance Object to get name of. Can be null, in which case its treated as of type [Nothing].
    * @return computed name of the object instance. Never null.
    */
   operator fun get(instance: Any?): String {
      val c = instance?.let { it::class } ?: Nothing::class
      val f = names.getElementOfSuper(c) ?: Any?::toString
      return f(instance)
   }

}