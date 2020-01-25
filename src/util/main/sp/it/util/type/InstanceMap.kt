package sp.it.util.type

import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import sp.it.util.collections.ObservableListRO
import sp.it.util.collections.readOnly
import sp.it.util.collections.setTo
import java.lang.reflect.Type

/** Map of instances per type. Useful for customization by pluggable & extensible behaviors. */
open class InstanceMap {
   private val m = HashMap<List<Class<*>>, ObservableList<Any?>>()

   @Suppress("UNCHECKED_CAST")
   private fun <T> at(type: List<Class<*>>): ObservableList<T> = m.computeIfAbsent(type) { observableArrayList(listOf()) } as ObservableList<T>

   /** Add instances of the specified type. */
   fun <T: Any> addInstances(type: List<Class<*>>, instances: Collection<T>) = at<T>(type).addAll(instances)

   /** Add instances of the type represented by the flattened list of specified classes. */
   fun <T: Any> addInstances(type: Type, instances: Collection<T>) = addInstances(type.flattenToRawTypes().toList(), instances)

   /** Add instances of the type represented by the specified generic type argument. */
   inline fun <reified T: Any> addInstances(vararg instances: T) = addInstances(typeLiteral<T>(), instances.toList())

   /** @return read only observable list of instances of the type represented by the flattened list of specified classes */
   fun <T> getInstances(type: List<Class<*>>): ObservableListRO<T> = at<T>(type).readOnly()

   /** @return read only observable list of instances of the specified type */
   fun <T> getInstances(type: Type): ObservableListRO<T> = getInstances(type.flattenToRawTypes().toList())

   /**
    * Nullability:
    *
    * The nullability of the specified generic type argument will be respected, thus if it is nullable, the returned
    * list will contain null.
    *
    * @return read only observable list of instances of the type represented by the specified generic type argument
    */
   @Suppress("UNCHECKED_CAST")
   inline fun <reified T: Any?> getInstances(): ObservableListRO<T> {
      val list = getInstances<T>(typeLiteral<T>())
      val isNullable = null is T
      return if (isNullable) {
         val out = observableArrayList<T>(list + (null as T))
         list.addListener { out setTo (list + (null as T)) }   // TODO: provide disposing mechanism or use WeakListener
         out.readOnly()
      } else {
         list
      }
   }

}