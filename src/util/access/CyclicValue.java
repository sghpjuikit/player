package util.access;

/**
 * @param <E> type of value
 */
public interface CyclicValue<E> {

	/**
	 * Returns cycled value as defined by the implementation. The cycling might
	 * not traverse all (even if finite amount of) values, it can skip or randomly
	 * select value.
	 *
	 * @return next value
	 */
	E cycle();

}
