package sp.it.util.dev

/** Denotes dsl. See [DslMarker]. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Dsl

@Dsl
object DslReceiver