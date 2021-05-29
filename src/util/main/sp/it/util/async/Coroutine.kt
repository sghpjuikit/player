package sp.it.util.async

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

fun CoroutineContext.launch(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(this).launch(start = start, block = block)