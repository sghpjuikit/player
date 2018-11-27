package sp.it.pl.util.async.future

import mu.KotlinLogging
import sp.it.pl.util.functional.invoke
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

private val logger = KotlinLogging.logger { }

internal fun Runnable.logging(): Runnable = Runnable {
    try {
        this()
    } catch(t: Throwable) {
        logger.error(t) { "Unhandled exception" }
        throw t
    }
}

internal fun <T> Consumer<T>.logging(): Consumer<T> = Consumer {
    try {
        this(it)
    } catch(t: Throwable) {
        logger.error(t) { "Unhandled exception" }
        throw t
    }
}

internal fun <T> Supplier<T>.logging(): Supplier<T> = Supplier {
    try {
        this()
    } catch(t: Throwable) {
        logger.error(t) { "Unhandled exception" }
        throw t
    }
}

internal fun <T,R> Function<T, R>.logging(): Function<T, R> = Function {
    try {
        this(it)
    } catch(t: Throwable) {
        logger.error(t) { "Unhandled exception" }
        throw t
    }
}