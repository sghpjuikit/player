package util.reactive;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;

import util.access.AccessibleValue;

import static org.reactfx.EventStreams.merge;

/**
 * @param <T>
 */
public class ValueStream<T> extends EventStreamBase<T> implements AccessibleValue<T>{
	private T v;
	private final EventStream<T> source;

	@SuppressWarnings("unchecked")
	public ValueStream(T initialValue, EventStream<? extends T>... sources) {
		v = initialValue;
		this.source = merge((EventStream[])sources);
	}

	@Override
	public T getValue() {
		return v;
	}

	@Override
	public void setValue(T event) {
		emit(event);
	}

	@Override
	public void emit(T value) {
		v = value;
		super.emit(value);
	}

	@Override
	protected Subscription observeInputs() {
		return source.subscribe(this::emit);
	}
}