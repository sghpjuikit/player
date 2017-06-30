package util.dev

/**
 * Denotes whether operation (with a side effect) is idempotent - invoking it subsequently has the same effect as
 * invoking it once, i.e., any subsequent invokes have no (side) effect at all.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Idempotent