package sp.it.util.dev

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/** Denotes dependence relation to highlight contracts that are not clear on their own. */
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, CONSTRUCTOR, FIELD, CLASS, FILE)
annotation class Dependency(val value: String)

/**
 * Denotes whether an operation (with a side effect) is idempotent - invoking it subsequently has the same effect as
 * invoking it once, i.e., any subsequent invokes have no (side) effect at all.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class Idempotent

/** Denotes unstable API of an experimental feature */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(CLASS, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class Experimental(val reason: String)

/** Denotes method or class that is thread-safe, i.e, can be called from any thread. */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(CLASS, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class ThreadSafe

/** Denotes whether invocation of the method is blocking. Usually useful for ui frameworks. */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class Blocks(val value: Boolean = true)