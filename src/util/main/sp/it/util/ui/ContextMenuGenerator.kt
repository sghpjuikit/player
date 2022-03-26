package sp.it.util.ui

import javafx.scene.control.MenuItem
import kotlin.reflect.KClass
import sp.it.util.collections.collectionUnwrap
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementClass
import sp.it.util.collections.insertEvery
import sp.it.util.collections.map.KClassListMap
import sp.it.util.dev.fail
import sp.it.util.functional.net
import sp.it.util.reactive.Subscription
import sp.it.util.type.superKClassesInc

private typealias MenuOwner = Any
private typealias ListMenuOwner<T> = MenuBuilder<List<MenuItem>, T>
private typealias ListMenuOwnerBlock<T> = ListMenuOwner<T>.() -> Unit
private typealias Builder = (Any?) -> Sequence<MenuItem>

/** Context menu generator with per type generator registry */
class ContextMenuGenerator {

   private val mNull = ArrayList<Builder>()
   private val mSingle = KClassListMap<Builder> { fail() }
   private var mSingleCustom: (KClass<*>) -> Builder = { { sequenceOf() } }
   private val mMany = KClassListMap<Builder> { fail() }

   operator fun invoke(block: ContextMenuGenerator.() -> Unit) = block()

   /** Sets menu dynamic per class menu builder. Useful to generate menu by some class-based mechanism. */
   fun addCustom(block: (KClass<*>) -> Builder) {
      mSingleCustom = block
   }

   /** Adds menu builder for `null`. */
   @Suppress("UNCHECKED_CAST")
   fun addNull(block: ListMenuOwnerBlock<Nothing?>): Subscription {
      val b: Builder = { value -> ListMenuOwner(mutableListOf(), value as Nothing?).apply(block).owner.asSequence() }
      mNull += b
      return Subscription { mNull -= b }
   }

   @Suppress("UNCHECKED_CAST")
   fun <T: Any> add(type: KClass<T>, block: ListMenuOwnerBlock<T>): Subscription {
      val b: Builder = { value -> ListMenuOwner(mutableListOf(), collectionUnwrap(value) as T).apply(block).owner.asSequence() }
      mSingle.accumulate(type, b)
      return Subscription { mSingle[type]?.remove(b) }
   }

   @Suppress("UNCHECKED_CAST")
   fun <T: Any> addMany(type: KClass<T>, block: ListMenuOwnerBlock<Collection<T>>): Subscription {
      val b: Builder = { value -> ListMenuOwner(mutableListOf(), collectionWrap(value) as Collection<T>).apply(block).owner.asSequence() }
      mMany.accumulate(type, b)
      return Subscription { mMany[type]?.remove(b) }
   }

   inline fun <reified T: Any> add(noinline items: ListMenuOwnerBlock<T>) = add(T::class, items)

   inline fun <reified T: Any> addMany(noinline items: ListMenuOwnerBlock<Collection<T>>) = addMany(T::class, items)

   operator fun get(value: Any?): Sequence<MenuItem> = when (value) {
      null -> {
         mNull.asSequence().flatMap { it(null) }
      }
      else -> {
         val valueSingle = collectionUnwrap(value)
         val valueMulti = collectionWrap(value).takeUnless { it.isEmpty() }

         val items1 = valueSingle
            ?.net { it::class.superKClassesInc().associateWith { mSingle[it].orEmpty() + mSingleCustom(it) } }
            .orEmpty()
         val itemsNType = valueMulti?.getElementClass()?.kotlin
         val itemsN = itemsNType
            ?.net {
               it.superKClassesInc().associateWith { mMany[it].orEmpty() }
            }
            .orEmpty()

         (items1.keys + itemsN.keys).asSequence()
            .map { items1[it].orEmpty().flatMap { it(valueSingle) } + itemsN[it].orEmpty().flatMap { it(valueMulti) } }.filter { it.isNotEmpty() }
            .map { it.sortedBy { it.text }.toList() }.filter { it.isNotEmpty() }
            .insertEvery(1) { listOf(menuSeparator()) }
            .flatten()
      }
   }

}