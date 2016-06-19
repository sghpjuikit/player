/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

/**
 * Extends behavior for {@link Enum} types to include {@link SequentialValue
 *
 * @param <E> {@link Enum} type. It must be the same type as the extending enum.
 * For example: {@code enum MyEnum implements CyclicEnum<MyEnum>}
 *
 * @author Martin Polakovic
 */
public interface CyclicEnum<E extends Enum<E>> extends SequentialValue<E> {

    /**
     * Returns cyclically next enum constant value from list of all values.
     * <p/>
     * {@inheritDoc}
     *
     * @return next cyclical enum constant according to its ordinal number.
     */
    @Override
    default E next() {
        return SequentialValue.next((E)this);
    }

    /**
     * Returns cyclically previous enum constant value from list of all values.
     * <p/>
     * {@inheritDoc}
     *
     * @return previous cyclical enum constant according to its ordinal number.
     */
    @Override
    default E previous() {
        return SequentialValue.previous((E)this);
    }

}
