@file:JvmName("Util")
@file:Suppress("unused")

package sp.it.pl.util.dev

import javafx.application.Platform
import javafx.beans.value.ObservableValue
import mu.KotlinLogging
import sp.it.pl.util.reactive.maintain
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.Consumer
import kotlin.reflect.KClass

fun throwIf(v: Boolean) {
    if (v) throw IllegalStateException("Requirement condition not met")
}

fun throwIf(v: Boolean, s: String) {
    if (v) throw IllegalStateException("Requirement condition not met: $s")
}

fun throwIfNot(v: Boolean) {
    if (!v) throw IllegalStateException("Requirement condition not met")
}

fun throwIfNot(v: Boolean, s: String) {
    if (!v) throw IllegalStateException("Requirement condition not met: $s")
}

fun <T> noNull(o: T?): T {
    if (o==null) throw IllegalStateException("Null forbidden")
    return o
}

fun <T> noNull(o: T?, message: String): T {
    if (o==null) throw IllegalStateException("Null forbidden: $message")
    return o
}

fun noNull(o1: Any?, o2: Any?) {
    if (o1==null || o2==null) throw IllegalStateException("Null forbidden")
}

fun noNull(vararg objects: Any?) {
    if (objects.any { it==null }) throw IllegalStateException("Null forbidden")
}

fun throwIfFxThread() {
    if (Platform.isFxApplicationThread())
        throw IllegalStateException("Must not be invoked on FX application thread!")
}

fun throwIfNotFxThread() {
    if (!Platform.isFxApplicationThread())
        throw IllegalStateException("Must be invoked on FX application thread!")
}

fun Field.throwIfFinal(): Field {
    if (Modifier.isFinal(modifiers))
        throw IllegalStateException("Final field forbidden. Field=$declaringClass.$name")
    return this
}

fun Field.throwIfNotFinal(): Field {
    if (!Modifier.isFinal(modifiers))
        throw IllegalStateException("Non final field forbidden. Field=$declaringClass.$name")
    return this
}

/** Prints the time it takes to execute specified block in milliseconds. Debugging only. */
fun <T> measureTimeMs(block: () -> T): T {
    val time = System.currentTimeMillis()
    val t = block()
    println(System.currentTimeMillis()-time)
    return t
}

/** Prints the value to console immediately and then on every change. */
fun <T> ObservableValue<T>.printOnChange(name: String = "") = maintain(Consumer { println("Value $name changed to=$it") })

/** @return [org.slf4j.Logger] for the class. */
fun KClass<*>.logger() = java.logger()

/** @return [org.slf4j.Logger] for the class. */
fun Class<*>.logger() = KotlinLogging.logger(simpleName)

/** @return all running threads (Incurs performance penalty, use only for debugging purposes) */
fun activeThreads() = Thread.getAllStackTraces().keys.asSequence()

/** Prints names and status of all threads. Debugging only. */
fun printThreads() = activeThreads().forEach { println("${it.name} ${it.state}") }