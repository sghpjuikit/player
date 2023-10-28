package sp.it.util.math

val <T: Comparable<T>> ClosedRange<T>.min: T get() = start

val <T: Comparable<T>> ClosedRange<T>.max: T get() = endInclusive

infix fun <T: Comparable<T>> ClosedFloatingPointRange<T>.isBelow(o: ClosedFloatingPointRange<T>): Boolean = endInclusive < o.start

infix fun <T: Comparable<T>> ClosedFloatingPointRange<T>.isAbove(o: ClosedFloatingPointRange<T>): Boolean = start < o.endInclusive

infix operator fun <T: Comparable<T>> ClosedFloatingPointRange<T>.contains(o: ClosedFloatingPointRange<T>): Boolean = start<=o.start && endInclusive>=o.endInclusive

infix fun <T: Comparable<T>> ClosedFloatingPointRange<T>.intersectsWith(o: ClosedFloatingPointRange<T>): Boolean = start in o || endInclusive in o