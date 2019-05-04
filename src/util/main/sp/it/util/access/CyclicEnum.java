package sp.it.util.access;

/**
 * Extends behavior for {@link Enum} types to include {@link Values
 *
 * @param <E> {@link Enum} type. It must be the same type as the extending enum. For example: {@code enum MyEnum
 * implements CyclicEnum<MyEnum>}
 */
public interface CyclicEnum<E extends Enum<E>> {

	/**
	 * Returns cyclically next enum constant value from list of all values.
	 * <p/>
	 * {@inheritDoc}
	 *
	 * @return next cyclical enum constant according to its ordinal number.
	 */
	@SuppressWarnings("unchecked")
	default E next() {
		return Values.next((E) this);
	}

	/**
	 * Returns cyclically previous enum constant value from list of all values.
	 * <p/>
	 * {@inheritDoc}
	 *
	 * @return previous cyclical enum constant according to its ordinal number.
	 */
	@SuppressWarnings("unchecked")
	default E previous() {
		return Values.previous((E) this);
	}

}