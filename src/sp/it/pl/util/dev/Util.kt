@file:JvmName("Util")
@file:Suppress("unused")

package sp.it.pl.util.dev

import javafx.beans.value.ObservableValue
import mu.KotlinLogging
import sp.it.pl.util.reactive.attach
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KClass

/** Prints the time it takes to execute specified block in milliseconds. */
fun <T> measureTimeMs(block: () -> T): T {
    val time = System.currentTimeMillis()
    val t = block()
    println(System.currentTimeMillis()-time)
    return t
}

/** Prints the value to console immediately and then on every change. */
fun <T> ObservableValue<T>.printOnChange(name: String = "") = attach { println("Value $name changed to=$it") }

/** @return [org.slf4j.Logger] for the class. */
fun KClass<*>.logger() = java.logger()

/** @return [org.slf4j.Logger] for the class. */
fun Class<*>.logger() = KotlinLogging.logger(simpleName)

/** @return all running threads (Incurs performance penalty, use only for debugging purposes) */
fun activeThreads() = Thread.getAllStackTraces().keys.asSequence()

/** Prints names and status of all threads. */
fun printThreads() = activeThreads().forEach { println("${it.name} ${it.state}") }

/** @return stacktrace as string, in the same format as [Exception#printStackTrace]. */
fun Exception.stackTraceAsString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    return sw.toString()
}

/** Prints this and returns it again. Debugging only. */
fun <T> T.printIt() = also { println(it) }