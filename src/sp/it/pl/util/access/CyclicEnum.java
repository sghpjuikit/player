package sp.it.pl.util.access;

/**
 * Extends behavior for {@link Enum} types to include {@link SequentialValue
 *
 * @param <E> {@link Enum} type. It must be the same type as the extending enum. For example: {@code enum MyEnum
 * implements CyclicEnum<MyEnum>}
 */
public interface CyclicEnum<E extends Enum<E>> extends SequentialValue<E> {

	/**
	 * Returns cyclically next enum constant value from list of all values.
	 * <p/>
	 * {@inheritDoc}
	 *
	 * @return next cyclical enum constant according to its ordinal number.
	 */
	@SuppressWarnings("unchecked")
	@Override
	default E next() {
		return SequentialValue.next((E) this);
	}

	/**
	 * Returns cyclically previous enum constant value from list of all values.
	 * <p/>
	 * {@inheritDoc}
	 *
	 * @return previous cyclical enum constant according to its ordinal number.
	 */
	@SuppressWarnings("unchecked")
	@Override
	default E previous() {
		return SequentialValue.previous((E) this);
	}

}