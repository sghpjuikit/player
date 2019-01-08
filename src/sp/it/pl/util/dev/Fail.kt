@file:JvmName("Fail")
@file:Suppress("unused")

package sp.it.pl.util.dev

import javafx.application.Platform
import java.lang.reflect.Field
import java.lang.reflect.Modifier


inline fun fail(message: () -> String? = { null }): Nothing = throw AssertionError(message())

@JvmOverloads
inline fun failIf(condition: Boolean, message: () -> String = { "" }) {
    if (condition) fail { "Requirement condition not met: ${message()}" }
}

inline fun failIfNot(condition: Boolean, message: () -> String) = failIf(!condition, message)

fun <T> noNull(o: T?) = o.noNull()

inline fun <T> T?.noNull(message: () -> String = { "" }): T = this ?: fail { "Null forbidden: ${message()}" }

fun failIfFxThread() {
    failIf(Platform.isFxApplicationThread()) { "Must not be invoked on FX application thread!" }
}

fun failIfNotFxThread() {
    failIf(!Platform.isFxApplicationThread()) { "Must be invoked on FX application thread!" }
}

fun Field.failIfFinal() = apply {
    failIf(Modifier.isFinal(modifiers)) { "Final field forbidden. Field=$declaringClass.$name" }
}

fun Field.failIfNotFinal() = apply {
    failIf(!Modifier.isFinal(modifiers)) { "Non final field forbidden. Field=$declaringClass.$name" }
}
