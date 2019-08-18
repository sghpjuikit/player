package sp.it.util.ui

import javafx.scene.control.MenuItem
import sp.it.util.collections.collectionUnwrap
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementType
import sp.it.util.collections.map.KClassListMap
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import kotlin.reflect.KClass

private typealias MenuOwner = Any
private typealias ListMenuOwner<T> = MenuBuilder<List<MenuItem>, T>
private typealias ListMenuOwnerBlock<T> = ListMenuOwner<T>.() -> Unit

/** Context menu generator with per type generator registry */
class ContextMenuGenerator {

   private val mNull = ArrayList<(Any?) -> Sequence<MenuItem>>()
   private val mSingle = KClassListMap<(Any?) -> Sequence<MenuItem>> { fail() }
   private val mMany = KClassListMap<(Any?) -> Sequence<MenuItem>> { fail() }

   operator fun invoke(block: ContextMenuGenerator.() -> Unit) = this.block()

   @Suppress("UNCHECKED_CAST")
   fun addNull(block: ListMenuOwnerBlock<Nothing?>) {
      mNull += { value ->
         ListMenuOwner(mutableListOf(), value as Nothing?).apply(block).owner.asSequence()
      }
   }

   @Suppress("UNCHECKED_CAST")
   fun <T: Any> add(type: KClass<T>, block: ListMenuOwnerBlock<T>) {
      mSingle.accumulate(type) { value ->
         ListMenuOwner(mutableListOf(), collectionUnwrap(value) as T).apply(block).owner.asSequence()
      }
   }

   @Suppress("UNCHECKED_CAST")
   fun <T: Any> addMany(type: KClass<T>, block: ListMenuOwnerBlock<Collection<T>>) {
      mMany.accumulate(type) { value ->
         ListMenuOwner(mutableListOf(), collectionWrap(value) as Collection<T>).apply(block).owner.asSequence()
      }
   }

   inline fun <reified T: Any> add(noinline items: ListMenuOwnerBlock<T>) = add(T::class, items)

   inline fun <reified T: Any> addMany(noinline items: ListMenuOwnerBlock<Collection<T>>) = addMany(T::class, items)

   operator fun get(value: Any?): Sequence<MenuItem> {
      val valueSingle = value?.let { collectionUnwrap(it) }
      val valueMulti = value?.let { collectionWrap(value) }?.takeUnless { it.isEmpty() }

      val items1 = valueSingle?.net { mSingle.getElementsOfSuperV(it::class) } ?: mNull.asSequence()

      val itemsNType = valueMulti.asIf<Collection<*>>()?.getElementType()?.kotlin
      val itemsN = itemsNType?.net { mMany.getElementsOfSuperV(it) } ?: sequenceOf()

      return (items1.asSequence() + itemsN.asSequence())
         .map { it(value) }
         .flatMap { sequenceOf(menuSeparator()) + it }
         .drop(1)
   }

}