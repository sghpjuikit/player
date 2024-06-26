package sp.it.util.async.coroutine

import java.util.concurrent.CompletionStage
import javafx.application.Platform
import javafx.util.Duration
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineStart.DEFAULT
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.invoke
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.JavaFxDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.BlockingExecutor
import sp.it.util.async.NewThreadExecutor
import sp.it.util.async.future.Fut
import sp.it.util.async.runOn
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import sp.it.util.reactive.Subscription

/** Non-blocking dispatcher for JavaFX UI tasks. See [Dispatchers.JavaFx] */
val FX = Dispatchers.JavaFx

/** Non-blocking dispatcher for CPU tasks. See [Dispatchers.Default]. Use [Dispatchers.VT] instead. */
val CPU = Dispatchers.Default

/** Use [Dispatchers.VT] instead. */
val IO: @BlockingExecutor CoroutineDispatcher = Dispatchers.IO

/** Blocking dispatcher for tasks on new thread. Use [Dispatchers.VT] instead. */
val NEW: @BlockingExecutor CoroutineDispatcher = NewThreadExecutor().asCoroutineDispatcher()

/**
 * Blocking dispatcher for CPU tasks. Uses virtual threads and as such allows blocking call.
 * Effectively the same as [Dispatchers.Default] + [Dispatchers.IO], but without the need to switch between the two.
 * The virtual thread does the switching (called pinning) automatically for all blocking calls - optimal and convenient.
 */
val VT: @BlockingExecutor CoroutineDispatcher = sp.it.util.async.VT.asCoroutineDispatcher()

/** Non-blocking dispatcher for JavaFX UI tasks. See [Dispatchers.JavaFx] */
val Dispatchers.FX: JavaFxDispatcher get() = sp.it.util.async.coroutine.FX

/** Non-blocking dispatcher for CPU tasks. See [Dispatchers.Default]. Use [Dispatchers.VT] instead. */
val Dispatchers.CPU: CoroutineDispatcher get() = sp.it.util.async.coroutine.CPU

/** Blocking dispatcher for tasks on new thread. Use [Dispatchers.VT] instead. */
val Dispatchers.NEW: @BlockingExecutor CoroutineDispatcher get() = sp.it.util.async.coroutine.NEW

/**
 * Blocking dispatcher for CPU tasks. Uses virtual threads and as such allows blocking call.
 * Effectively the same as [Dispatchers.Default] + [Dispatchers.IO], but without the need to switch between the two.
 * The virtual thread does the switching (called pinning) automatically for all blocking calls - optimal and convenient.
 */
val Dispatchers.VT: @BlockingExecutor CoroutineDispatcher get() = sp.it.util.async.coroutine.VT

fun launch(dispatcher: CoroutineDispatcher, start: CoroutineStart = DEFAULT, block: suspend (CoroutineScope) -> Unit) = CoroutineScope(dispatcher).launch(start = start, block = { block(this) })

/**
 * @return flow that produces the first item after the given initial delay and subsequent items with the given delay between them;
 * the items begin with 1 and represent the index in the sequence
 */
@Suppress("SameParameterValue")
fun flowTimer(delayMillis: Long = 0, repeatMillis: Long): Flow<Int> = flow {
   var i = 1
   delay(delayMillis)
   while (true) {
      emit(i)
      i++
      delay(repeatMillis)
   }
}

/** [Flow.collect] on the specified [dispatcher]. */
suspend fun <T> Flow<T>.collectOn(dispatcher: CoroutineDispatcher, collector: FlowCollector<T>) = collect { value -> dispatcher { collector.emit(value) } }

/** @return deferred value - see [CompletionStage.asDeferred] */
@Suppress("DeferredIsResult")
fun <T> Fut<T>.asDeferred(): Deferred<T> = asCompletableFuture().asDeferred()

/** @return awaited value - see [Fut.asDeferred] and [Deferred.await] */
suspend fun <T> Fut<T>.await(): T = asCompletableFuture().await()

/** @return this job as [Fut] - see [Job.asCompletableFuture] */
fun <T> Deferred<T>.asFut(): Fut<T> = Fut(asCompletableFuture())

/** @return this job as [Fut] - see [Job.asCompletableFuture] */
fun Job.asFut(): Fut<Unit> = Fut(asCompletableFuture())

/** @return this job as [Subscription] that [Job.cancel] this job */
fun Job.toSubscription() = Subscription { cancel() }

/** Launch coroutine and return it as [Fut]. Canceling the future cancels the coroutine (see [future]). */
fun <T> runSuspending(dispatcher: CoroutineDispatcher, start: CoroutineStart = DEFAULT, block: suspend CoroutineScope.() -> T) : Fut<T> = Fut(CoroutineScope(dispatcher).future(EmptyCoroutineContext, start, block))

/** Launch coroutine on [FX] ([UNDISPATCHED] if on fx thread already) and return it as [Fut]. Canceling the future cancels the coroutine (see [future]). */
fun <T> runSuspendingFx(block: suspend CoroutineScope.() -> T) : Fut<T> = runSuspending(FX, if (Platform.isFxApplicationThread()) UNDISPATCHED else DEFAULT, block)

/** Launch coroutine on [FX] and return it as [Fut]. Canceling the future cancels the coroutine (see [future]). */
fun <T> runSuspendingFxLater(block: suspend CoroutineScope.() -> T) : Fut<T> = runSuspending(FX, DEFAULT, block)

/** Launch coroutine and return it as [Fut]. Canceling the future cancels the coroutine (see [future]). */
fun <T> CoroutineScope.runSuspending(start: CoroutineStart = DEFAULT, block: suspend CoroutineScope.() -> T) : Fut<T> = Fut((this + FX).future(EmptyCoroutineContext, start, block))

/** Launch coroutine on [FX] ([UNDISPATCHED] if on fx thread already) and return it as [Fut]. Canceling the future cancels the coroutine (see [future]). */
fun <T> CoroutineScope.runSuspendingFx(block: suspend CoroutineScope.() -> T) : Fut<T> = runSuspending(if (Platform.isFxApplicationThread()) UNDISPATCHED else DEFAULT, block)

/** Launch coroutine on [FX] and return it as [Fut]. Canceling the future cancels the coroutine (see [future]). */
fun <T> CoroutineScope.runSuspendingFxLater(block: suspend CoroutineScope.() -> T) : Fut<T> = runSuspending(DEFAULT, block)

/** [withContext] that uses [runTry] to never throw and return [Try] instead */
suspend inline infix fun <T> CoroutineDispatcher.invokeTry(noinline block: suspend CoroutineScope.() -> T): Try<T, Throwable> =
   runTry { withContext(this, block) }

/** [delay] that uses [Duration] */
suspend fun delay(duration: Duration) =
   delay(duration.toMillis().milliseconds)

/** [delay] until the installed block executes, e.g. `delayTill(animation::setOnDone)` */
suspend fun delayTill(installer: (Runnable) -> Unit): Unit =
   suspendCoroutine { c -> installer { c.resume(Unit) } }

/** [delay] until the installed block executes, e.g. `animation::setOnDone.delayTill()` */
@JvmName("delayTill2")
suspend fun ((Runnable) -> Unit).delayTill(): Unit =
   suspendCoroutine { c -> this { c.resume(Unit) } }