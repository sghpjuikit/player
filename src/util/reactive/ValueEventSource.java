package util.reactive;

import org.reactfx.EventSink;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;
import util.access.AccessibleValue;

public class ValueEventSource<T> extends EventStreamBase<T> implements EventSink<T>, AccessibleValue<T> {
	private T v;

	public ValueEventSource(T initialValue) {
		v = initialValue;
	}

	@Override
	public T getValue() {
		return v;
	}

	@Override
	public void setValue(T event) {
		push(event);
	}

	@Override
	public void push(T event) {
		v = event;
		emit(v);
	}

	@Override
	protected final Subscription observeInputs() {
		return Subscription.EMPTY;
	}
}