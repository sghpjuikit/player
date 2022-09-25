package sp.it.util.async.coroutine

import java.util.concurrent.CompletionStage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.invoke
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.JavaFxDispatcher
import kotlinx.coroutines.launch
import sp.it.util.async.NewThreadExecutor
import sp.it.util.async.future.Fut
import sp.it.util.reactive.Subscription

val FX = Dispatchers.JavaFx

val CPU = Dispatchers.Default

val NEW = NewThreadExecutor().asCoroutineDispatcher()

val Dispatchers.FX: JavaFxDispatcher get() = sp.it.util.async.coroutine.FX

val Dispatchers.CPU: CoroutineDispatcher get() = sp.it.util.async.coroutine.CPU

val Dispatchers.NEW: CoroutineDispatcher get() = sp.it.util.async.coroutine.NEW

fun launch(dispatcher: CoroutineDispatcher, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend (CoroutineScope) -> Unit) = CoroutineScope(dispatcher).launch(start = start, block = { block(this) })

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
suspend fun <T> Fut<T>.await(): T = asDeferred().await()

/** @return this job as [Fut] - see [Job.asCompletableFuture] */
fun Job.asFut(): Fut<Unit> = Fut(asCompletableFuture())

/** @return this job as [Subscription] that [Job.cancel] this job */
fun Job.toSubscription() = Subscription { cancel() }