package sp.it.util.async.future

import javafx.application.Platform
import sp.it.util.async.FX
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.Try

/**
 * Blocks curent [FX] execution after this future completes and returns result or throws exception.
 * The [FX] is not actually blocked, see [Platform.enterNestedEventLoop] and [Platform.exitNestedEventLoop].
 */
@Suppress("UNCHECKED_CAST")
fun <T> Fut<T>.awaitFx(): T {
   failIfNotFxThread()
   val k = object: Any() {}
   onDone(FX) { Platform.exitNestedEventLoop(k, it.toTryRaw()) }
   return (Platform.enterNestedEventLoop(k) as Try<T, Throwable>).orThrow
}


/** [awaitFX] or [blockAndGetOrThrow] depending on which thread this is called. */
@Suppress("UNCHECKED_CAST")
fun <T> Fut<T>.awaitFxOrBlock(): T =
   if (Platform.isFxApplicationThread()) awaitFx() else blockAndGetOrThrow()