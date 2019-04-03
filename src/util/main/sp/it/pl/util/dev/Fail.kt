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
fun failIfFinal(field: Field) = failIf(Modifier.isFinal(field.modifiers)) { "Final field forbidden. Field=${field.declaringClass}.${field.name}" }

/** Throw runtime exception if this field is not final. */
fun failIfNotFinal(field: Field) = failIf(!Modifier.isFinal(field.modifiers)) { "Non final field forbidden. Field=${field.declaringClass}.${field.name}" }

/** Invokes [Thread.interrupted] and if the result is true, throws [InterruptedException]. */
@Throws(InterruptedException::class)
fun failIfInterrupted() {
    if (Thread.interrupted()) throw InterruptedException()
}

/**
 * Throw runtime exception for if or when case branch, that represents programming error and must never execute.
 * This is a convenient assertion with developer friendly message.
 *
 * @param value illegal value that caused entering of the illegal case branch
 */
fun failCase(value: Any?): Nothing = fail { "Illegal if/when case with value: $value" }