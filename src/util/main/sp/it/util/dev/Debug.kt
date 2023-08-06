package sp.it.util.dev

import javafx.beans.value.ObservableValue
import kotlin.reflect.KClass
import mu.KotlinLogging
import sp.it.util.reactive.attach
import sp.it.util.units.formatToSmallestUnit
import sp.it.util.units.millis

/** @return [org.slf4j.Logger] for the class. */
fun KClass<*>.logger() = java.logger()

/** @return [org.slf4j.Logger] for the class. */
fun Class<*>.logger() = KotlinLogging.logger(simpleName)

/** @return all running threads (Incurs performance penalty, use only for debugging purposes) */
fun activeThreads() = Thread.getAllStackTraces().keys.asSequence()

/** Prints this and returns it again. Debugging only. */
fun <T> T.printIt() = also { println(it) }

/** Prints names and status of all threads. */
fun printThreads() = activeThreads().forEach { println("${it.isDaemon} ${it.state} ${it.name}") }

/** Prints the value to console immediately and then on every change. */
fun <T> ObservableValue<T>.printOnChange(name: String = "") = attach { println("Value $name changed to=$it") }

/** Prints the time it takes to execute specified block in milliseconds. */
fun <T> printExecutionTime(name: String? = null, block: () -> T): T {
   val time = System.currentTimeMillis()
   try {
      val t = block()
      return t
   } finally {
      println("${if (name==null) "" else "$name "}${(System.currentTimeMillis() - time).millis.formatToSmallestUnit()}")
   }
}

/** Prints the current thread's stacktrace. */
fun printStacktrace() = println(Exception().stacktraceAsString)

/** @return string of printed stacktrace of this throwable. See [Throwable.stackTraceToString] */
val Throwable.stacktraceAsString: String
   get() = stackTraceToString()