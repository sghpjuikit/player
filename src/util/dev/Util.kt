@file:JvmName("Util")
@file:Suppress("unused")

package util.dev

import javafx.application.Platform
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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

fun <T> noØ(o: T?): T {
    if (o==null) throw IllegalStateException("Null forbidden")
    return o
}

fun <T> noØ(o: T?, message: String): T {
    if (o==null) throw IllegalStateException("Null forbidden: $message")
    return o
}

fun noØ(o1: Any?, o2: Any?) {
    if (o1==null || o2==null) throw IllegalStateException("Null forbidden")
}

fun noØ(o1: Any?, o2: Any?, o3: Any?) {
    if (o1==null || o2==null || o3==null) throw IllegalStateException("Null forbidden")
}

fun noØ(o1: Any?, o2: Any?, o3: Any?, o4: Any?) {
    if (o1==null || o2==null || o3==null || o4==null) throw IllegalStateException("Null forbidden")
}

fun noØ(vararg os: Any?) = os.forEach { if (it==null) throw IllegalStateException("Null forbidden") }

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

fun <T> measureTime(r: () -> T): T {
    val time = System.currentTimeMillis()
    val t = r()
    println(System.currentTimeMillis()-time)
    return t
}

/**
 * Returns [org.slf4j.Logger] for the class of this object or this object if it is of type class.
 *
 * Equivalent to: org.slf4j.LoggerFactory.getLogger(class);
 */
fun Any.log() = (when {
        this is KClass<*> -> this
        this is Class<*> -> this.kotlin
        else -> this::class
    }).log()

/**
 * Returns [org.slf4j.Logger] for the class.
 *
 * Equivalent to: org.slf4j.LoggerFactory.getLogger(class);
 */
fun KClass<*>.log() = LoggerFactory.getLogger(this.java)!!

/** @return all running threads. Incurs performance penalty, use for debugging purposes. */
fun activeThreads() = Thread.getAllStackTraces().keys.asSequence()

/** Prints names of all currently running non daemon threads.  */
fun printNonDaemonThreads() = activeThreads().filter { t -> !t.isDaemon }.forEach { t -> println(t.name) }