package sp.it.util.functional

import java.util.Comparator.nullsFirst
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import sp.it.util.collections.list.PrefList
import sp.it.util.dev.failIf
import sp.it.util.functional.Functors.F1
import sp.it.util.functional.Functors.F2
import sp.it.util.functional.Functors.F3
import sp.it.util.functional.Functors.F4
import sp.it.util.functional.Util.EQ
import sp.it.util.functional.Util.IDENTITY
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.enumValues
import sp.it.util.type.isEnum
import sp.it.util.type.isSubclassOf
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.isSupertypeOf
import sp.it.util.type.notnull
import sp.it.util.type.nullable
import sp.it.util.type.raw
import sp.it.util.type.traverseToSuper
import sp.it.util.type.type
import sp.it.util.type.typeOrAny

@Suppress("RemoveExplicitTypeArguments")
class FunctorPool {
   private val asSelfName = "As Self"
   private val fsAll = hashSetOf<PPF>()

   fun <I, O> add(name: String, i: VType<I>, o: VType<O>, f: F1<in I, out O>) =
      addF(PF0(name, i, o, f))

   fun <I, P1, O> add(name: String, i: VType<I>, o: VType<O>, p1: Parameter<P1>, f: F2<in I, in P1, out O>) =
      addF(PF1(name, i, o, p1, f))

   fun <I, P1, P2, O> add(name: String, i: VType<I>, o: VType<O>, p1: Parameter<P1>, p2: Parameter<P2>, f: F3<in I, in P1, in P2, out O>) =
      addF(PF2(name, i, o, p1, p2, f))

   fun <I, P1, P2, P3, O> add(name: String, i: VType<I>, o: VType<O>, p1: Parameter<P1>, p2: Parameter<P2>, p3: Parameter<P3>, f: F4<in I, P1, in P2, in P3, out O>) =
      addF(PF3(name, i, o, p1, p2, p3, f))

   fun <I, O> add(name: String, i: VType<I>, o: VType<O>, pi: Boolean, po: Boolean, pio: Boolean, f: F1<in I, out O>) =
      addF(PF0(name, i, o, f), pi, po, pio)

   fun <I, P1, O> add(name: String, i: VType<I>, o: VType<O>, p1: Parameter<P1>, pi: Boolean, po: Boolean, pio: Boolean, f: F2<in I, in P1, out O>) =
      addF(PF1(name, i, o, p1, f), pi, po, pio)

   fun <I, P1, P2, O> add(name: String, i: VType<I>, o: VType<O>, p1: Parameter<P1>, p2: Parameter<P2>, pi: Boolean, po: Boolean, pio: Boolean, f: F3<in I, in P1, in P2, out O>) =
      addF(PF2(name, i, o, p1, p2, f), pi, po, pio)

   fun <I, P1, P2, P3, O> add(name: String, i: VType<I>, o: VType<O>, p1: Parameter<P1>, p2: Parameter<P2>, p3: Parameter<P3>, pi: Boolean, po: Boolean, pio: Boolean, f: F4<in I, in P1, in P2, in P3, out O>) =
      addF(PF3(name, i, o, p1, p2, p3, f), pi, po, pio)

   // TODO: add automatically in getters, this causes duplication because Long is Number
   fun <C: Comparable<C>> addComparisons(c: VType<C>, defaultValue: C?) {
      val cn = c.nullable()
      val comparator = nullsFirst { a: C, b: C -> a compareTo b }
      add("< Less", cn, type<Boolean>(), Parameter(cn, defaultValue)) { x, y -> comparator.compare(x, y)<0 }
      add("≤ Less or same", cn, type<Boolean>(), Parameter(cn, defaultValue)) { x, y -> comparator.compare(x, y)<=0 }
      add("= Same", cn, type<Boolean>(), Parameter(cn, defaultValue)) { x, y -> comparator.compare(x, y)==0 }
      add("≥ More or same", cn, type<Boolean>(), Parameter(cn, defaultValue)) { x, y -> comparator.compare(x, y)>=0 }
      add("> More", cn, type<Boolean>(), Parameter(cn, defaultValue)) { x, y -> comparator.compare(x, y)>0 }
   }

   /** Add function to the pool and sets as preferred according to parameters. */
   fun addF(f: PF<*, *>, i: Boolean = false, o: Boolean = false, io: Boolean = false) {
      failIf(f.name.equals(asSelfName, ignoreCase = true)) { "Name '$asSelfName' reserved for identity function" }
      fsAll += PPF(f, i, o, io)
   }

   /** Remove function from the pool. */
   fun remF(f: PF<*, *>?) {
      fsAll.removeIf { it.f==f }
   }

   private fun <T> preProcessVirtual(c: VType<T>): PrefList<PF<*, *>> {
      val fs = PrefList<PF<*, *>>()

      // add is predicate
      if (c.raw.isEnum) {
         fs.addPreferred(PF1("Is", c.nullable(), type<Boolean>(), Parameter(c.nullable(), c.raw.enumValues[0])) { a, b -> a==b })
      } else {
         fs.add(PF1("Is", c.nullable(), type<Boolean>(), Parameter(c.nullable(), null), EQ))
      }

      // add collection predicates
      if (c.raw.isSubclassOf<Iterable<*>>()) {
         val listType = c.type.argOf(List::class, 0)
         val anyType = VType<CollectionAny<Any?>>(CollectionAny::class.createType(listOf(listType), false, listOf()))
         val allType = VType<CollectionAll<Any?>>(CollectionAll::class.createType(listOf(listType), false, listOf()))
         val nonType = VType<CollectionNon<Any?>>(CollectionNon::class.createType(listOf(listType), false, listOf()))
         fs.add(PF0("Any", c, anyType) { a -> CollectionAny(a.asIs<Iterable<Any?>>().asSequence()) })
         fs.add(PF0("All", c, allType) { a -> CollectionAll(a.asIs<Iterable<Any?>>().asSequence()) })
         fs.add(PF0("None", c, nonType) { a -> CollectionNon(a.asIs<Iterable<Any?>>().asSequence()) })
      }

      return fs
   }

   fun <T> getCollectionMappers(c: VType<T>): PrefList<PF<*, *>> {
      if (c.raw.isSubclassOf<CollectionAny<*>>()) {
         val itemTypeRaw = c.type.argOf(CollectionAny::class, 0).typeOrAny
         val itemType = VType<Any?>(itemTypeRaw)
         return getI(itemType.notnull()).map { f ->
            if (f.out.raw==Boolean::class)
               PFN(f.name, f.`in`, type<Boolean>(), f.parameters.toTypedArray()) { a, args -> a.asIs<CollectionAny<Any?>>().seq.any { it?.let { f.apply(it, args).asIs<Boolean>() } ?: false } }
            else
               PFN(f.name, f.`in`, VType(CollectionAny::class.createType(listOf(KTypeProjection.covariant(f.out.type)), false, listOf())), f.parameters.toTypedArray()) { a, args -> CollectionAny(a.asIs<CollectionAny<Any?>>().seq.map { it?.let { f.apply(it, args) } }) }
         }
      }
      if (c.raw.isSubclassOf<CollectionAll<*>>()) {
         val itemTypeRaw = c.type.argOf(CollectionAll::class, 0).typeOrAny
         val itemType = VType<Any?>(itemTypeRaw)
         return getI(itemType.notnull()).map { f ->
            if (f.out.raw==Boolean::class)
               PFN(f.name, f.`in`, type<Boolean>(), f.parameters.toTypedArray()) { a, args -> a.asIs<CollectionAll<Any?>>().seq.all { it?.let { f.apply(it, args).asIs<Boolean>() } ?: false } }
            else
               PFN(f.name, f.`in`, VType(CollectionAll::class.createType(listOf(KTypeProjection.covariant(f.out.type)), false, listOf())), f.parameters.toTypedArray()) { a, args -> CollectionAll(a.asIs<CollectionAll<Any?>>().seq.map { it?.let { f.apply(it, args) } }) }
         }
      }
      if (c.raw.isSubclassOf<CollectionNon<*>>()) {
         val itemTypeRaw = c.type.argOf(CollectionNon::class, 0).typeOrAny
         val itemType = VType<Any?>(itemTypeRaw)
         return getI(itemType.notnull()).map { f ->
            if (f.out.raw==Boolean::class)
               PFN(f.name, f.`in`, type<Boolean>(), f.parameters.toTypedArray()) { a, args -> a.asIs<CollectionNon<Any?>>().seq.none { it?.let { f.apply(it, args).asIs<Boolean>() } ?: false } }
            else
               PFN(f.name, f.`in`, VType(CollectionNon::class.createType(listOf(KTypeProjection.covariant(f.out.type)), false, listOf())), f.parameters.toTypedArray()) { a, args -> CollectionNon(a.asIs<CollectionNon<Any?>>().seq.map { it?.let { f.apply(it, args) } }) }
         }
      }
      return PrefList()
   }

   /** Returns all functions taking input I. */
   fun <I> getI(type: VType<I>): PrefList<PF<I, *>> {
      val selfF = PF0<I, I>(asSelfName, type, type, IDENTITY.asIs())
      val sameF = preProcessVirtual(type)
      val normF = fsAll.asSequence().filter { it.f.`in` isSupertypeOf type }
         .sortedByDescending { it.f.`in`.type.raw.traverseToSuper(Any::class).size }.toList()

      return PrefList<PF<*, *>>(
         listOf(selfF) + normF.map { it.f } + sameF,
         sameF.preferred ?: normF.find { it.i }?.f ?: selfF
      ).asIs()
   }

   /** Returns all functions producing output O. */
   fun <O> getO(type: VType<O>): PrefList<PF<*, O>> {
      val selfF = PF0<O, O>(asSelfName, type, type, IDENTITY.asIs())
      val sameF = preProcessVirtual(type)
      val normF = fsAll.asSequence().filter { it.f.out isSubtypeOf type }
         .sortedByDescending { it.f.out.type.raw.traverseToSuper(Any::class).size }.toList()

      return PrefList<PF<*, *>>(
         listOf(selfF) + normF.map { it.f } + sameF,
         sameF.preferred ?: normF.find { it.o }?.f ?: selfF
      ).asIs()
   }

   /** Returns all functions taking input I and producing output O. */
   fun <I, O> getIO(i: VType<I>, o: VType<O>): PrefList<PF<I, O>> {
      val selfF = if (i isSupertypeOf o) PF0<I, I>(asSelfName, i, i, IDENTITY.asIs()).asIs<PF<I, O>>() else null
      val sameFi = preProcessVirtual(i)
      val sameFo = if (i.raw==o.raw) PrefList() else preProcessVirtual(o)
      val normF = fsAll.asSequence().filter { it.f.`in` isSupertypeOf i && it.f.out isSubtypeOf o }
         .sortedByDescending { it.f.`in`.type.raw.traverseToSuper(Any::class).size }.toList()

      return PrefList<PF<*, *>>(
         listOfNotNull(selfF) + normF.map { it.f } + sameFi + sameFo,
         sameFi.preferred ?: sameFo.preferred ?: normF.find { it.io }?.f ?: selfF
      ).asIs()
   }

   fun <I, O> getPF(name: String, i: VType<I>, o: VType<O>): PF<I, O>? {
      return if (name==asSelfName) {
         if (o isSubtypeOf i) PF0(asSelfName, i, o, IDENTITY.asIs())
         else null
      } else {
         getIO(i, o).find { it.name==name }
      }
   }

   fun <I> getPrefI(i: VType<I>): PF<I, *> = getI(i).preferredOrFirst

   fun <O> getPrefO(o: VType<O>): PF<*, O> = getO(o).preferredOrFirst

   fun <I, O> getPrefIO(i: VType<I>, o: VType<O>): PF<I, O>? = getIO(i, o).preferredOrFirst

   fun <IO> getPrefIO(io: VType<IO>): PF<IO, IO>? = getPrefIO(io, io)

   private data class PPF(val f: PF<*, *>, val i: Boolean, val o: Boolean, val io: Boolean)

}