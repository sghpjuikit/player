package sp.it.pl.util.dev

import javafx.application.Platform
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/** Throw runtime exception with the specified message. */
inline fun fail(message: () -> String = { "" }): Nothing = throw AssertionError(message())

/** Throw runtime exception with the specified message if specified condition is true. */
@JvmOverloads
inline fun failIf(condition: Boolean, message: () -> String = { "" }) {
    if (condition) fail { "Requirement condition not met: ${message()}" }
}

/** Throw runtime exception with the specified message if specified condition is false. */
inline fun failIfNot(condition: Boolean, message: () -> String) = failIf(!condition, message)

/** Throw runtime exception with the specified message if specified object is null. */
@JvmOverloads
inline fun <T> noNull(o: T?, message: () -> String = { "" }): T = o ?: fail { "Null forbidden: ${message()}" }

/** Throw runtime exception if the current thread is [Platform.isFxApplicationThread]. */
fun failIfFxThread() = failIf(Platform.isFxApplicationThread()) { "Must not be invoked on FX application thread!" }

/** Throw runtime exception if the current thread is not [Platform.isFxApplicationThread]. */
fun failIfNotFxThread() = failIf(!Platform.isFxApplicationThread()) { "Must be invoked on FX application thread!" }

/** Throw runtime exception if this field is final. */
fun Field.failIfFinal() = apply {
    failIf(Modifier.isFinal(modifiers)) { "Final field forbidden. Field=$declaringClass.$name" }
}

/** Throw runtime exception if this field is not final. */
fun Field.failIfNotFinal() = apply {
    failIf(!Modifier.isFinal(modifiers)) { "Non final field forbidden. Field=$declaringClass.$name" }
}