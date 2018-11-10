package sp.it.pl.util.collections

/** @return new list containing elements of this sequence, e.g. for safe iteration */
fun <T> Sequence<T>.materialize() = toList()

/** @return new list containing elements of this list, e.g. for safe iteration */
fun <T> List<T>.materialize() = toList()

/** @return new set containing elements of this set, e.g. for safe iteration */
fun <T> Set<T>.materialize() = toSet()