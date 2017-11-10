@file:JvmName("Util")
@file:Suppress("unused")

package util.dev

import javafx.application.Platform
import mu.KotlinLogging
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

fun <T> measureTime(block: () -> T): T {
    val time = System.currentTimeMillis()
    val t = block()
    println(System.currentTimeMillis()-time)
    return t
}

/** @return [org.slf4j.Logger] for the class of this object or this object if it is of type class. */
fun Any.log() = (when {
        this is KClass<*> -> this
        this is Class<*> -> this.kotlin
        else -> this::class
    }).log()

/** @return [org.slf4j.Logger] for the class. */
fun KClass<*>.log() = KotlinLogging.logger(this.java.simpleName)

/** @return all running threads (Incurs performance penalty, use only for debugging purposes) */
fun activeThreads() = Thread.getAllStackTraces().keys.asSequence()

/** Prints names of all currently running non daemon threads. */
fun printNonDaemonThreads() = activeThreads().filter { !it.isDaemon }.forEach { println(it.name) }