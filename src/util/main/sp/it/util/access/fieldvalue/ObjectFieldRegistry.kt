package sp.it.util.access.fieldvalue

import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import sp.it.util.collections.mapset.MapSet
import sp.it.util.dev.failIf
import sp.it.util.functional.Functors
import sp.it.util.functional.Try
import sp.it.util.functional.net
import sp.it.util.parsing.ConverterString
import sp.it.util.type.ObjectFieldMap
import sp.it.util.type.VType
import sp.it.util.type.createTypeStar

/**
 * [ObjectField] per type manager.
 *
 * [ObjectField] instances are often defined in sets per type they are targeting. It is often desired to make this set
 * enum-like (i.e., add management like values(), valueOf(text), etc.). A companion is a good target for this.
 *
 * So the structure ends up like this:
 *```
 *     MyType {
 *        MyTypeField: ObjectField<MyType, T> {
 *           companion object {
 *              val FIELD_1 = ...
 *              val FIELD_2 = ...
 *              val FIELD_N = ...
 *
 *              // management code
 *           }
 *        }
 *     }
 *```
 * Making the field companion inherit from this type will
 * * provide all management for free
 * * registers fields instances to [ObjectFieldMap.DEFAULT] using [ObjectFieldMap.add]
 * * registers fields instances to [Functors.pool] using [sp.it.util.functional.FunctorPool.add]
 *
 * Usage:
 *```
 *     MyType {
 *        MyTypeField: ObjectField<MyType, T> {
 *           companion object: ObjectFieldRegistry<MyType, MyTypeField>(MyType::class) {
 *              val FIELD_1 = this + ...
 *              val FIELD_2 = this + ...
 *              val FIELD_N = this + ...
 *           }
 *        }
 *     }
 *```
 */
abstract class ObjectFieldRegistry<V: Any, F: ObjectField<V, *>>(private val type: KClass<V>): ConverterString<F> {

   private val typeFull = type.let {
      failIf(it.typeParameters.isNotEmpty()) { "Classes with type parameters not yet supported: $it" }
      VType<V>(it.createTypeStar(false))
   }
   private val allImpl = MapSet<String, F> { it.name() }
   val all: Set<F> = allImpl

   /** Registers the specified fields. */
   fun <X: F> register(vararg field: X) = field.forEach { this + it }

   /** Registers fields declared in this or enclosing class as Kotlin objects. */
   @Suppress("UNCHECKED_CAST")
   fun registerDeclared() = (this::class.nestedClasses + this::class.java.enclosingClass?.kotlin?.nestedClasses.orEmpty())
      .mapNotNull { it.objectInstance }.filterIsInstance<ObjectField<V,*>>()
      .forEach { register(it as F) }

   /** Registers the specified field and returns it. */
   infix operator fun <X: F> plus(field: X): X = field.also { f ->
      allImpl += f

      ObjectFieldMap.DEFAULT.add(type, all)
      Functors.pool.add(f.name(), typeFull, f.type, false, false, false, f::getOf)
   }

   /** @return field with the specified [ObjectField.name] or null if none */
   open fun valueOf(text: String): F? = allImpl[text]

   override fun toS(o: F) = o.name()

   override fun ofS(s: String) = valueOf(s)?.net { Try.ok(it) } ?: Try.error("Not a recognized field: '$s'")

}