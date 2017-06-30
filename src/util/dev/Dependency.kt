package util.dev

/**
 * Annotates code that depends on or is depended on by other code. Marks and
 * documents inflexible code, that should not be changed arbitrarily.
 */
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class Dependency(
        val value: String = ""
)
