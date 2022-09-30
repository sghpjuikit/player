package sp.it.util.reactive

/** Object that requires any kind of disposal. Disposable. */
fun interface Unsubscribable {
   /** Disposes pf this unsubscribable. One-time and irreversible. */
   fun unsubscribe()
}

/**
 * Sets this [Unsubscribable] to be [Unsubscribable.unsubscribe]d according to the specified [Unsubscriber].
 * Equivalent to: `this.apply(disposer)`.
 * Basically the `disposer.register(disposable)`.
 * @return this
 */
infix fun <T: Unsubscribable> T.on(disposer: Unsubscriber) = apply(disposer)

/**
 * Lambda consuming [Unsubscribable], used for setting up calling [Unsubscribable.unsubscribe] in the future.
 * Basically the `disposer.register(disposable)`.
 */
typealias Unsubscriber = (Unsubscribable) -> Unit