package sp.it.util.async.coroutine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sp.it.util.functional.Option
import sp.it.util.functional.Option.None
import sp.it.util.functional.Option.Some
import sp.it.util.functional.ifSome

/** @return flow that emits windows of size 2 with step 1 */
fun <T> Flow<T>.pairs(): Flow<Pair<T, T>> = flow {
   var e1: Option<T> = None
   collect { e2: T ->
      e1.ifSome { emit(it to e2) }
      e1 = Some(e2)
   }
}