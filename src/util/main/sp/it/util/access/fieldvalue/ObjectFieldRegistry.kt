package sp.it.util.access.fieldvalue

import sp.it.util.collections.mapset.MapSet
import sp.it.util.functional.Functors
import sp.it.util.functional.PF0
import sp.it.util.functional.Util.IDENTITY
import sp.it.util.type.ObjectFieldMap
import sp.it.util.type.rawJ
import kotlin.reflect.KClass

/**
 * [ObjectField] per type manager.
 *
 * [ObjectField] instances are often defined in sets per type they are targeting. It is often desired to make this set
 * enum-like (i.e., add management like values(), valueOf(text), etc). A companion is a good target for this.
 *
 * So the structure ends up like this:
 * ```
 * MyType {
 *    MyTypeField: ObjectField<MyType, T> {
 *       companion object {
 *          val FIELD_1 = ...
 *          val FIELD_2 = ...
 *          val FIELD_N = ...
 *
 *          // management code
 *       }
 *    }
 * }
 * ```
 *
 * Making the field companion inherit from this type will
 * * provide all management for free
 * * registers fields instances to [ObjectFieldMap.DEFAULT] using [ObjectFieldMap.add]
 * * registers fields instances to [Functors.pool] using [sp.it.util.functional.FunctorPool.add]
 *
 * Usage:
 * ```
 * MyType {
 *    MyTypeField: ObjectField<MyType, T> {
 *       companion object: ObjectFieldRegistry<MyType, MyTypeField>(MyType::class) {
 *          val FIELD_1 = this + ...
 *          val FIELD_2 = this + ...
 *          val FIELD_N = this + ...
 *       }
 *    }
 * }
 * ```
 */
abstract class ObjectFieldRegistry<V: Any, F: ObjectField<V, *>>(private val type: KClass<V>) {

   private val allImpl = MapSet<String, F> { it.name() }
   val all: Set<F> = allImpl

   /** Registers the specified field and returns it. */
   infix operator fun <X: F> plus(field: X) = field.also { f ->
      allImpl += f

      val canBePreferred = run {
         // checks if there is a preferred function for this exact type, if not we set this as one
         Functors.pool.getI(type.java)?.preferred.let { it==null || it.`in`!=type.java || (it is PF0<*, *> && it.f==IDENTITY) }
      }
      ObjectFieldMap.DEFAULT.add(type, all)
      Functors.pool.add(f.name(), type.java, f.type.rawJ, canBePreferred, false, false, f::getOf)
   }

   /** @return field with the specified [ObjectField.name] or null if none */
   open fun valueOf(text: String): F? = allImpl[text]

}