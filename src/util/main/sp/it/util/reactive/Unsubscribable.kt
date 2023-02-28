package sp.it.util.reactive

import java.util.concurrent.CompletableFuture
import javafx.animation.Transition
import kotlinx.coroutines.Job
import sp.it.util.async.future.Fut

/** Object that requires any kind of disposal. Disposable. */
fun interface Unsubscribable {
   /** Disposes pf this unsubscribable. One-time and irreversible. */
   fun unsubscribe()
}

/**
 * Sets this [Unsubscribable] to be [Unsubscribable.unsubscribe]d according to the specified [Unsubscriber].
 * Does nothing if null.
 * Equivalent to: `this.apply(disposer)`.
 * Basically the `disposer.register(disposable)`.
 * @return this
 */
infix fun <T: Unsubscribable?> T.on(disposer: Unsubscriber): T = if (this==null) this else apply(disposer)

/**
 * Sets this [Unsubscribable] to be [Unsubscribable.unsubscribe]d according to the specified [Unsubscriber].
 * Does nothing if null.
 * Equivalent to: `this.apply { disposer += ::unsubscribe }`.
 * Basically the `disposer.register(disposable)`.
 * @return this
 */
infix fun <T: Unsubscribable?> T.on(disposer: Handler0): T = if (this==null) this else apply { disposer += ::unsubscribe }

/** Converts [Fut.cancel] to [Unsubscribable] and calls [on] */
infix fun <T: Any?> Fut<T>.on(disposer: Unsubscriber): Fut<T> = apply { Unsubscribable { cancel() } on disposer }

/** Converts [CompletableFuture.cancel] to [Unsubscribable] and calls [on] */
infix fun <T: Any?> CompletableFuture<T>.on(disposer: Unsubscriber): CompletableFuture<T> = apply { Unsubscribable { cancel(true) } on disposer }

/** Converts [Job.cancel] to [Unsubscribable] and calls [on] */
infix fun Job.on(disposer: Unsubscriber): Job = apply { Unsubscribable { cancel() } on disposer }

/** Converts [Transition.stop] to [Unsubscribable] and calls [on] */
infix fun Transition.on(disposer: Unsubscriber): Transition = apply { Unsubscribable { stop() } on disposer }

/**
 * Lambda consuming [Unsubscribable], used for setting up calling [Unsubscribable.unsubscribe] in the future.
 * Basically the `disposer.register(disposable)`.
 */
typealias Unsubscriber = (Unsubscribable) -> Unit