package sp.it.util.math

val <T: Comparable<T>> ClosedRange<T>.min: T get() = start

val <T: Comparable<T>> ClosedRange<T>.max: T get() = endInclusive

infix fun <T: Comparable<T>> ClosedRange<T>.isBelow(o: ClosedRange<T>): Boolean = endInclusive < o.start

infix fun <T: Comparable<T>> ClosedRange<T>.isAbove(o: ClosedRange<T>): Boolean = start < o.endInclusive

infix operator fun <T: Comparable<T>> ClosedRange<T>.contains(o: ClosedRange<T>): Boolean = start<=o.start && endInclusive>=o.endInclusive

infix fun <T: Comparable<T>> ClosedRange<T>.intersectsWith(o: ClosedRange<T>): Boolean = start <= o.endInclusive && endInclusive >= o.start