package sp.it.util.collections

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.util.AbstractCollection
import java.util.Optional
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.estimateRuntimeType
import sp.it.util.type.kType
import sp.it.util.type.kTypeNothingNonNull
import sp.it.util.type.kTypeNothingNullable
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.type.typeNothingNonNull
import sp.it.util.type.typeNothingNullable

@Suppress("RemoveExplicitTypeArguments")
class CollectionsUtilTest: FreeSpec({
   Collection<*>::getElementType.name - {
      listOf<Int>().getElementType() shouldBe kTypeNothingNonNull()
      listOf(null, null).getElementType() shouldBe kTypeNothingNullable()
      listOf("", null).getElementType() shouldBe kType<String?>()
      listOf("", " ").getElementType() shouldBe kType<String>()
      listOf(0.toByte(), 10L).getElementType() shouldBe kType<Number>()
      listOf(0.toByte(), 10L, null).getElementType() shouldBe kType<Number?>()
      listOf("", Try.ok()).getElementType() shouldBe kType<Any>()
      listOf("", Try.ok(), null).getElementType() shouldBe kType<Any?>()
      listOf("", 10L).getElementType() shouldBe kType<Comparable<*>>()
      listOf("", 10L, null).getElementType() shouldBe kType<Comparable<*>?>()

      listOf(listOf<Int>(1, 1), setOf<Int>(1, 1)).getElementType() shouldBe kType<java.util.AbstractCollection<*>>()
      listOf(listOf<Int>(1, 1), setOf<Int>(1, 1), null).getElementType() shouldBe kType<java.util.AbstractCollection<*>?>()

      listOf(listOf<Int>(1), listOf<Int>(1)).getElementType().raw shouldBe listOf<Int>(1)::class
      listOf(listOf<Int>(1), listOf<Int>(1), null).getElementType().isMarkedNullable shouldBe true

      listOf(listOf<Int>(), listOf<Int>()).getElementType().raw shouldBe listOf<Int>()::class
      listOf(listOf<Int>(), listOf<Int>(), null).getElementType().isMarkedNullable shouldBe true
   }

   Any::estimateRuntimeType.name - {

      "ordinary types" - {
         null.estimateRuntimeType() shouldBe typeNothingNullable()
         1.estimateRuntimeType() shouldBe type<Int>()
         "".estimateRuntimeType() shouldBe type<String>()
      }

      "Optional" - {
         val emptyOptionalType = VType<Nothing>(Optional::class.createType(listOf(KTypeProjection.invariant(kTypeNothingNonNull()))))
         Optional.empty<Int>().estimateRuntimeType() shouldBe emptyOptionalType
         Optional.ofNullable(null).estimateRuntimeType() shouldBe emptyOptionalType
         Optional.ofNullable("").estimateRuntimeType() shouldBe type<Optional<String>>()
      }

      "Try" - {
         Try.ok("").estimateRuntimeType() shouldBe type<Try.Ok<String>>()
         Try.error("").estimateRuntimeType() shouldBe type<Try.Error<String>>()
      }

      "Collections" - {

         infix fun <T> List<T>.estimateRuntimeTypeShouldHaveElementType(type: VType<*>) {
            asIs<Any?>().estimateRuntimeType().type.argOf(List::class, 0).type shouldBe type.type
            estimateRuntimeType() shouldBe type
         }

         // correct class
         ArrayList(listOf(1, 2L)).asIs<Any>().estimateRuntimeType().raw shouldBe ArrayList::class
         listOf<Int>().asIs<Any>().estimateRuntimeType().raw shouldBe listOf<Int>()::class
         listOf<Int>(1).asIs<Any>().estimateRuntimeType().raw shouldBe listOf<Int>(1)::class
         listOf<Int>(1, 1).asIs<Any>().estimateRuntimeType().raw shouldBe listOf<Int>(1, 1)::class

         // empty
         listOf<Int>() estimateRuntimeTypeShouldHaveElementType typeNothingNonNull()
         listOf<Int?>() estimateRuntimeTypeShouldHaveElementType typeNothingNonNull()

         // same class
         listOf(null, null) estimateRuntimeTypeShouldHaveElementType typeNothingNullable()
         listOf("", null) estimateRuntimeTypeShouldHaveElementType type<String?>()
         listOf("", " ") estimateRuntimeTypeShouldHaveElementType type<String>()

         // same superclass
         listOf(0.toByte(), 10L) estimateRuntimeTypeShouldHaveElementType type<Number>()
         listOf(0.toByte(), 10L, null) estimateRuntimeTypeShouldHaveElementType type<Number?>()
         listOf("", Try.ok()) estimateRuntimeTypeShouldHaveElementType type<Any>()
         listOf("", Try.ok(), null) estimateRuntimeTypeShouldHaveElementType type<Any?>()

         // same interface
         listOf("", 10L) estimateRuntimeTypeShouldHaveElementType type<Comparable<*>>()
         listOf("", 10L, null) estimateRuntimeTypeShouldHaveElementType type<Comparable<*>?>()

         // generic types (same class, same element class)
         "!enable" {
            listOf(listOf<Int>(1), listOf<Int>(1)) estimateRuntimeTypeShouldHaveElementType type<Int>()
            listOf(listOf<Int>(1), listOf<Int>(1), null) estimateRuntimeTypeShouldHaveElementType type<Int?>()
         }

         // generic types (same class, same element class)
         listOf(Optional.of(""), Optional.empty()) estimateRuntimeTypeShouldHaveElementType type<Optional<Any>>()

         // generic types (same superclass, same element class)
         listOf(listOf<Int>(1, 1), setOf<Int>(1, 1)) estimateRuntimeTypeShouldHaveElementType type<AbstractCollection<Int>>()
         listOf(listOf<Int>(1, 1), setOf<Int?>(1, 1, null), null) estimateRuntimeTypeShouldHaveElementType type<AbstractCollection<Int?>?>()

         // generic types (same superclass, same element superclass)
         listOf(listOf<Byte>(0.toByte(), 0.toByte()), setOf<Int>(1, 1)) estimateRuntimeTypeShouldHaveElementType type<AbstractCollection<Number>>()
         listOf(listOf<Byte>(0.toByte(), 0.toByte()), setOf<Int?>(1, 1, null)) estimateRuntimeTypeShouldHaveElementType type<AbstractCollection<Number?>>()

         // generic types (same superclass, same element interface)
         listOf(listOf<String>("", ""), setOf<Int>(1, 1)) estimateRuntimeTypeShouldHaveElementType type<AbstractCollection<Comparable<*>>>()
         listOf(listOf<String>("", ""), setOf<Int?>(1, 1, null)) estimateRuntimeTypeShouldHaveElementType type<AbstractCollection<Comparable<*>?>>()
         listOf(Try.ok(""), Try.error("")) estimateRuntimeTypeShouldHaveElementType type<Try<String, String>>()
      }
   }
})
