package sp.it.util.reactive

import javafx.beans.value.ObservableValue
import javafx.util.Duration
import sp.it.util.access.vx
import sp.it.util.async.executor.EventReducer

/** @return observable value that holds last value of this observable value the specified time period after it changes */
infix fun <T> ObservableValue<T>.throttleToLast(period: Duration): ObservableValue<T> {
   val v = vx<T>(value)
   val r = EventReducer.toLast<T>(period.toMillis()) { v.value = it }
   attach(r::push)
   return v
}