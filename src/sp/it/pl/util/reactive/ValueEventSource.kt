package sp.it.pl.util.reactive

import org.reactfx.EventSink
import org.reactfx.EventStreamBase
import org.reactfx.Subscription
import sp.it.pl.util.access.AccessibleValue

class ValueEventSource<T>(private var v: T?): EventStreamBase<T>(), EventSink<T>, AccessibleValue<T> {

    override fun getValue(): T? = v

    override fun setValue(event: T) = push(event)

    override fun push(event: T) {
        v = event
        emit(v)
    }

    override fun observeInputs() = Subscription.EMPTY!!
}