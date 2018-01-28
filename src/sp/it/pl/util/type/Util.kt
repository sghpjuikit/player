package sp.it.pl.util.type

fun <T,U> Class<T>.isSuperclassOf(type: Class<U>) = isAssignableFrom(type)

fun <T,U> Class<T>.isSubclassOf(type: Class<U>) = type.isSuperclassOf(this)