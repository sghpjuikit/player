package sp.it.util.functional

fun interface TriConsumer<in A, in B, in C> {
   fun accept(a: A, b: B, c: C)
}