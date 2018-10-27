package sp.it.pl.util.collections

/** @return new list containing elements of this list, i.g. for safe iteration */
fun <T> List<T>.materialize() = toList()

/** @return new set containing elements of this set, i.g. for safe iteration */
fun <T> Set<T>.materialize() = toSet()