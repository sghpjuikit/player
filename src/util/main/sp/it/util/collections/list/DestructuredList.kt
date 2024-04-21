package sp.it.util.collections.list

import sp.it.util.dev.fail

@JvmInline value class DestructuredList<T>(private val list: List<T>): List<T> by list {
   operator fun component1(): T = list.getOrNull(0) ?: throw IndexOutOfBoundsException("Out of bounds 0/${list.size}")
   operator fun component2(): T = list.getOrNull(1) ?: throw IndexOutOfBoundsException("Out of bounds 1/${list.size}")
   operator fun component3(): T = list.getOrNull(2) ?: throw IndexOutOfBoundsException("Out of bounds 2/${list.size}")
   operator fun component4(): T = list.getOrNull(3) ?: throw IndexOutOfBoundsException("Out of bounds 3/${list.size}")
   operator fun component5(): T = list.getOrNull(4) ?: throw IndexOutOfBoundsException("Out of bounds 4/${list.size}")
   operator fun component6(): T = list.getOrNull(5) ?: throw IndexOutOfBoundsException("Out of bounds 5/${list.size}")
   operator fun component7(): T = list.getOrNull(6) ?: throw IndexOutOfBoundsException("Out of bounds 6/${list.size}")
   operator fun component8(): T = list.getOrNull(7) ?: throw IndexOutOfBoundsException("Out of bounds 7/${list.size}")
   operator fun component9(): T = list.getOrNull(8) ?: throw IndexOutOfBoundsException("Out of bounds 8/${list.size}")
   operator fun component10(): T = list.getOrNull(9) ?: throw IndexOutOfBoundsException("Out of bounds 9/${list.size}")
   operator fun component11(): T = list.getOrNull(10) ?: throw IndexOutOfBoundsException("Out of bounds 10/${list.size}")
   operator fun component12(): T = list.getOrNull(11) ?: throw IndexOutOfBoundsException("Out of bounds 11/${list.size}")
}