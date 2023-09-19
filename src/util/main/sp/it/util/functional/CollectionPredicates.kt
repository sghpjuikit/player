package sp.it.util.functional

/** Type marker for Iterable functor Any */
data class CollectionAny<T>(val seq: Sequence<T>)

/** Type marker for Iterable functor All */
data class CollectionAll<T>(val seq: Sequence<T>)

/** Type marker for Iterable functor All */
data class CollectionNon<T>(val seq: Sequence<T>)