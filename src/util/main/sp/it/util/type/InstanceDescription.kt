package sp.it.util.type

import sp.it.util.Named
import sp.it.util.collections.map.KClassListMap
import sp.it.util.dev.fail
import sp.it.util.named
import java.util.Objects
import kotlin.reflect.KClass

private typealias Description = Named
private typealias Descriptions = ArrayList<Description>
private typealias DescriptionExtractor = (Any) -> Descriptions

/**
 * Hierarchical class - instanceDescriptor map.
 */
class InstanceDescription {
   private val names = KClassListMap<DescriptionExtractor> { fail() }

   /**
    * Initialization block.
    * All write operations can only be done within this block.
    *
    * This class is not thread-safe and should be populated before it is used.
    */
   operator fun invoke(block: InstanceDescription.() -> Unit) = this.block()

   /** Registers extracting description extractor for the specified class. */
   @Suppress("UNCHECKED_CAST")
   infix fun <T: Any> KClass<T>.describe(extractor: DescriptionBuilder.(T) -> Unit) {
      names.computeIfAbsent(this) { ArrayList() } += {
         Descriptions().apply {
            DescriptionBuilder(this).apply {
               extractor(it as T)
            }
         }
      }
   }

   /**
    * Returns name/string representation of the object instance. If none is
    * provided, [Objects.toString] is used.
    *
    * @param instance Object to get name of. Can be null, in which case its treated as of type [Void].
    * @return computed name of the object instance. Never null.
    */
   operator fun get(instance: Any?): Sequence<Description> = when (instance) {
      null -> sequenceOf()
      else -> names.getElementsOfSuper(instance::class).flatMap { it(instance).asSequence() }
   }

   /** Allows DSL for [InstanceDescription]. */
   class DescriptionBuilder(private val descriptions: Descriptions) {
      infix fun String.info(value: String) = descriptions.add(this named value)
   }

}