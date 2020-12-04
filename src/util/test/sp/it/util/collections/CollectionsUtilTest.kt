package sp.it.util.collections

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import sp.it.util.functional.Try
import sp.it.util.type.estimateRuntimeType
import sp.it.util.type.kType
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.type.typeNothingNonNull
import sp.it.util.type.typeNothingNullable

@Suppress("RemoveExplicitTypeArguments")
class CollectionsUtilTest: FreeSpec({
   Collection<*>::getElementType.name - {
      listOf<Int>().getElementType() shouldBe typeNothingNonNull().type
      listOf(null, null).getElementType() shouldBe typeNothingNullable().type
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
      listOf(0.toByte(), 10L).estimateRuntimeType() shouldBe type<Number>()
      listOf(0.toByte(), 10L, null).estimateRuntimeType() shouldBe type<Number?>()
      listOf("", Try.ok()).estimateRuntimeType() shouldBe type<Any>()
      listOf("", Try.ok(), null).estimateRuntimeType() shouldBe type<Any?>()
      listOf("", 10L).estimateRuntimeType() shouldBe type<Comparable<*>>()
      listOf("", 10L, null).estimateRuntimeType() shouldBe type<Comparable<*>?>()

      listOf(listOf<Int>(1, 1), setOf<Int>(1, 1)).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<*>>()
      listOf(listOf<Int>(1, 1), setOf<Int>(1, 1), null).estimateRuntimeType() shouldBe type<java.util.AbstractCollection<*>?>()

      listOf(listOf<Int>(1), listOf<Int>(1)).estimateRuntimeType().raw shouldBe listOf<Int>(1)::class
      listOf(listOf<Int>(1), listOf<Int>(1), null).estimateRuntimeType().isNullable shouldBe true

      listOf(listOf<Int>(), listOf<Int>()).estimateRuntimeType().raw shouldBe listOf<Int>()::class
      listOf(listOf<Int>(), listOf<Int>(), null).estimateRuntimeType().isNullable shouldBe true
   }
})
