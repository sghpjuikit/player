package sp.it.util.async

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

fun CoroutineContext.launch(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(this).launch(start = start, block = block)

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