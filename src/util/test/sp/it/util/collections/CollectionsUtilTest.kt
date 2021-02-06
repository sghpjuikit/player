package sp.it.util.collections

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import sp.it.util.functional.Try
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

   Collection<*>::estimateRuntimeType.name - {
      listOf<Int>().estimateRuntimeType() shouldBe typeNothingNonNull()
      listOf(null, null).estimateRuntimeType() shouldBe typeNothingNullable()
      listOf("", null).estimateRuntimeType() shouldBe type<String?>()
      listOf("", " ").estimateRuntimeType() shouldBe type<String>()

      // same superclass
      listOf(0.toByte(), 10L).estimateRuntimeType() shouldBe type<Number>()
      listOf(0.toByte(), 10L, null).estimateRuntimeType() shouldBe type<Number?>()
      listOf("", Try.ok()).estimateRuntimeType() shouldBe type<Any>()
      listOf("", Try.ok(), null).estimateRuntimeType() shouldBe type<Any?>()

      // same interface
      listOf("", 10L).estimateRuntimeType() shouldBe type<Comparable<*>>()
      listOf("", 10L, null).estimateRuntimeType() shouldBe type<Comparable<*>?>()

      // generic types (same class, same element class)
      listOf(listOf<Int>(1), listOf<Int>(1)).estimateRuntimeType().raw shouldBe listOf<Int>(1)::class
      listOf(listOf<Int>(1), listOf<Int>(1), null).estimateRuntimeType().isNullable shouldBe true
      listOf(listOf<Int>(), listOf<Int>()).estimateRuntimeType().raw shouldBe listOf<Int>()::class
      listOf(listOf<Int>(), listOf<Int>(), null).estimateRuntimeType().isNullable shouldBe true

      // generic types (same superclass, same element class)
      listOf(listOf<Int>(1, 1), setOf<Int>(1, 1)).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<*>>()
      listOf(listOf<Int>(1, 1), setOf<Int>(1, 1), null).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<*>?>()

      // generic types (same superclass, same element superclass)
      listOf(listOf<Byte>(0.toByte(), 0.toByte()), setOf<Int>(1, 1)).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<Number>>()
      listOf(listOf<Byte>(0.toByte(), 0.toByte()), setOf<Int?>(1, 1, null)).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<Number?>>()

      // generic types (same superclass, same element interface)
      listOf(listOf<String>("", ""), setOf<Int>(1, 1)).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<Comparable<*>>>()
      listOf(listOf<String>("", ""), setOf<Int?>(1, 1, null)).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<Comparable<*>?>>()
   }
})
