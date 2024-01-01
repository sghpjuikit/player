package sp.it.pl.ui.pane

import kotlin.reflect.KClass
import sp.it.util.async.future.Fut
import sp.it.util.collections.getElementClass
import sp.it.util.dev.fail

fun getUnwrappedType(d: Any?): KClass<*> = when (d) {
   null -> Nothing::class
   is Collection<*> -> d.getElementClass().kotlin
   else -> d::class
}

fun futureUnwrap(o: Any?): Any? = when (o) {
   is Fut<*> -> when {
      o.isDone() -> {
         when (val result = o.getDone()) {
            is Fut.Result.ResultOk<*> -> result.value
            else -> null
         }
      }
      else -> o
   }
   else -> o
}

fun futureUnwrapOrThrow(o: Any?): Any? = when (o) {
   is Fut<*> -> when {
      o.isDone() -> {
         when (val result = o.getDone()) {
            is Fut.Result.ResultOk<*> -> result.value
            else -> null
         }
      }
      else -> fail { "Future not done yet" }
   }
   else -> o
}

data class NoUnwrap<T>(val collection: Collection<T>)

fun nounwrapWrap(data: Any?): Any? = if (data is Collection<*>) NoUnwrap<Any?>(data) else data

fun nounwrapUnWrap(data: Any?): Any? = if (data is NoUnwrap<*>) data.collection else data