@file:Suppress("unused")

package sp.it.pl.util.validation

import javafx.beans.InvalidationListener
import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.singletonObservableList
import javafx.util.Duration
import sp.it.pl.util.collections.map.ClassListMap
import sp.it.pl.util.collections.map.ClassMap
import sp.it.pl.util.dev.failIfNot
import sp.it.pl.util.functional.Try
import sp.it.pl.util.text.Password
import sp.it.pl.util.type.Util.getGenericInterface
import sp.it.pl.util.type.Util.instantiateOrThrow
import sp.it.pl.util.validation.Constraint.Declaration.EXPLICIT
import sp.it.pl.util.validation.Constraint.Declaration.IMPLICIT
import java.io.File
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

interface Constraint<in T> {

    fun isValid(value: T?): Boolean

    fun message(): String

    fun validate(value: T?): Try<Nothing?, String> = if (isValid(value)) Try.ok() else Try.error(message())

    @MustBeDocumented
    @Retention(RUNTIME)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
    annotation class DeclarationType(val value: Declaration = EXPLICIT)

    enum class Declaration {
        IMPLICIT, EXPLICIT
    }


/* ---------- ANNOTATIONS ------------------------------------------------------------------------------------------- */

    @MustBeDocumented
    @Retention(RUNTIME)
    @Target(AnnotationTarget.ANNOTATION_CLASS)
    annotation class IsConstraint(val value: KClass<*>)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(Any::class)
    annotation class ConstraintBy(val value: KClass<out Constraint<*>>)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(File::class)
    annotation class FileType(val value: FileActor = FileActor.ANY)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(Number::class)
    annotation class Min(val value: Double)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(Number::class)
    annotation class Max(val value: Double)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(Number::class)
    annotation class MinMax(val min: Double, val max: Double)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(String::class)
    annotation class NonEmpty

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(String::class)
    annotation class Length(val min: Int, val max: Int)

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(Any::class)
    annotation class NonNull

    @MustBeDocumented
    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(RUNTIME)
    @IsConstraint(Collection::class)
    annotation class NonNullElements

/* ---------- IMPLEMENTATIONS --------------------------------------------------------------------------------------- */

    /** Denotes type of [java.io.File]. For example to decide between file and directory chooser. */
    enum class FileActor constructor(private val condition: (File) -> Boolean, private val message: String): Constraint<File?> {
        FILE( { it.isFile }, "File must not be directory"),
        DIRECTORY( { it.isDirectory }, "File must be directory"),
        ANY({ true }, "");

        override fun isValid(value: File?) = value==null || condition(value)
        override fun message() =  message

    }

    class NumberMinMax(val min: Double, val max: Double): Constraint<Number> {

        init {
            failIfNot(max>min) { "Max value must be greater than min value" }
        }

        override fun isValid(value: Number?) = value == null || value.toDouble() in min..max
        override fun message() = "Number must be in range $min - $max"

    }

    class StringNonEmpty: Constraint<String> {
        override fun isValid(value: String?) = value!=null && !value.isEmpty()
        override fun message() = "String must not be empty"
    }

    class PasswordNonEmpty: Constraint<Password> {
        override fun isValid(value: Password?) = value!=null && !value.value.isEmpty()
        override fun message() = "Password must not be empty"
    }

    class StringLength(val min: Int, val max: Int): Constraint<String> {

        init {
            failIfNot(max>min) { "Max value must be greater than min value" }
        }

        override fun isValid(value: String?) = value==null || value.length in min..max
        override fun message() = "Text must be at least $min and at most$max characters long"
    }

    @Constraint.DeclarationType(IMPLICIT)
    class DurationNonNegative: Constraint<Duration> {
        override fun isValid(value: Duration?): Boolean {
            return value==null || value.greaterThanOrEqualTo(Duration.ZERO)
        }

        override fun message(): String {
            return "Duration can not be negative"
        }
    }

    class HasNonNullElements: Constraint<Collection<*>> {
        override fun isValid(value: Collection<*>?) = value==null || value.all { it!=null }
        override fun message() = "All items of the list must be non null"
    }

    class ObjectNonNull: Constraint<Any> {
        override fun isValid(value: Any?) = value!=null
        override fun message() = "Value must not be null"
    }

    class PreserveOrder: Constraint<Any> {
        override fun isValid(value: Any?) = true
        override fun message() = "Items must preserve original order"
    }

    class ReadOnlyIf(val condition: ObservableBooleanValue): Constraint<Any> {
        constructor(condition: ObservableValue<Boolean>, unless: Boolean): this(
                object: BooleanBinding() {
                    init {
                        super.bind(condition)
                    }
                    override fun dispose() = super.unbind(condition)
                    override fun computeValue() = if (unless) !condition.value else condition.value
                    override fun getDependencies() = singletonObservableList(condition)
                }
        )
        constructor(condition: Boolean): this(
                object: ObservableBooleanValue {
                    override fun removeListener(listener: ChangeListener<in Boolean>) {}
                    override fun removeListener(listener: InvalidationListener) {}
                    override fun addListener(listener: ChangeListener<in Boolean>) {}
                    override fun addListener(listener: InvalidationListener) {}
                    override fun getValue() = condition
                    override fun get() = condition
                }
        )
        override fun isValid(value: Any?) = true
        override fun message() = "Is disabled"
    }
}

/* ---------- ANNOTATION -> IMPLEMENTATION MAPPING ------------------------------------------------------------------ */

class Constraints {


    companion object {
        private val MAPPER = ClassMap<(Annotation) -> Constraint<*>>()
        @JvmField val IMPLICIT_CONSTRAINTS: ClassListMap<Constraint<*>> = ClassListMap({ o -> getGenericInterface(o.javaClass, 0, 0) })

        private val INIT = object {
            init {
                register<Constraint.FileType> { it.value }
                register<Constraint.Min> { Constraint.NumberMinMax(it.value, Double.MAX_VALUE) }
                register<Constraint.Max> { Constraint.NumberMinMax(Double.MIN_VALUE, it.value) }
                register<Constraint.MinMax> { Constraint.NumberMinMax(it.min, it.max) }
                register<Constraint.NonEmpty> { Constraint.StringNonEmpty() }
                register<Constraint.Length> { Constraint.StringLength(it.min, it.max) }
                register<Constraint.NonNullElements> { Constraint.HasNonNullElements() }
                register<Constraint.NonNull> { Constraint.ObjectNonNull() }
                register<Constraint.ConstraintBy> { instantiateOrThrow(it.value.java) }
                registerByType<Duration, Constraint.DurationNonNegative>()
            }
        }

        @Suppress("UNCHECKED_CAST")
        private inline fun <reified CA: Annotation> register(noinline constraintFactory: (CA) -> Constraint<*>) {
            val type = CA::class.java

            if (CA::class.findAnnotation<Constraint.IsConstraint>() == null)
                throw RuntimeException("${Constraint::class} must be annotated by ${Constraint.IsConstraint::class}")

            MAPPER[type] = (constraintFactory as (Annotation) -> Constraint<*>)
        }

        private inline fun <reified T, reified C: Constraint<T>> registerByType() {
            C::class.findAnnotation<Constraint.DeclarationType>()?.let {
                if (it.value==IMPLICIT) {
                    IMPLICIT_CONSTRAINTS.accumulate(T::class.java, instantiateOrThrow(C::class.java))
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic fun <X> toConstraint(a: Annotation): Constraint<X> {
            return MAPPER[a.annotationClass.java]!!.invoke(a) as Constraint<X>
        }

    }
}