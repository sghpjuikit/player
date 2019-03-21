package sp.it.pl.util.dev

/** Denotes dependence relation to highlight contracts that are not clear on their own. */
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class Dependency(val value: String)

/**
 * Denotes whether an operation (with a side effect) is idempotent - invoking it subsequently has the same effect as
 * invoking it once, i.e., any subsequent invokes have no (side) effect at all.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Idempotent

/** Denotes unstable API of an experimental feature */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Experimental(val reason: String)

/** Denotes method or class that is thread-safe, i.e, can be called from any thread. */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class ThreadSafe

/** Denotes whether invocation of the method is blocking. Usually useful for ui frameworks. */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Blocks(val value: Boolean = true)