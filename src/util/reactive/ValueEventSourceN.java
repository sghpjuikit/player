package util.reactive;

/**
 * <p/>
 *
 * @author Martin Polakovic
 */
public class ValueEventSourceN<T> extends ValueEventSource<T> {
	private T v;
	private T empty_val;

	public ValueEventSourceN(T initialValue, T emptyValue) {
		super(initialValue);
		empty_val = emptyValue;
	}

	public ValueEventSourceN(T initialValue) {
		this(initialValue, initialValue);
	}

	@Override
	public void push(T event) {
		super.push(event==null ? empty_val : event);
	}
}