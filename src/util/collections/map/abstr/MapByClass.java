package util.collections.map.abstr;

import java.util.Collection;
import java.util.List;
import static util.type.Util.getSuperClassesInc;

/**
 * @author Martin Polakovic
 */
public interface MapByClass<E> {

	/**
	 * Multi key get returning the aggregation of results for each key.
	 *
	 * @return list of all values mapped to any of the keys (in that order).
	 */
	List<E> getElementsOf(Collection<Class> keys);

	/** @see #getElementsOf(java.util.Collection) */
	List<E> getElementsOf(Class... keys);

	/**
	 * Returns elements mapped to one of (in that order):
	 * <ul>
	 * <li>specified class
	 * <li>any of specified class' superclasses up to Object.class
	 * <li>any of specified class' interfaces
	 * </ul>
	 * or empty list if no such mapping exists.
	 */
	default List<E> getElementsOfSuper(Class<?> key) {
		List<Class> keys = getSuperClassesInc(key);
		return getElementsOf(keys);
	}

	/**
	 * Returns elements mapped to one of (in that order):
	 * <ul>
	 * <li>specified class
	 * <li>any of specified class' superclasses up to Object.class
	 * <li>any of specified class' interfaces
	 * <li>Void.class or void.class
	 * </ul>
	 * or empty list if no such mapping exists.
	 * <p/>
	 * Note: Void.class is useful for mapping objects based on their generic
	 * type.
	 */
	default List<E> getElementsOfSuperV(Class<?> key) {
		List<Class> keys = getSuperClassesInc(key);
		if (!Void.class.equals(key)) keys.add(Void.class);
		keys.add(void.class);
		return getElementsOf(keys);
	}

	/**
	 * Returns first element mapped to one of (in that order):
	 * <ul>
	 * <li>specified class
	 * <li>any of specified class' superclasses up to Object.class
	 * <li>any of specified class' interfaces
	 * </ul>
	 * or null if no such mapping exists.
	 */
	default E getElementOfSuper(Class<?> key) {
		List<Class> keys = getSuperClassesInc(key);
		for (Class c : keys) {
			List<E> es = getElementsOf(c);
			if (!es.isEmpty())
				return es.get(0);
		}
		return null;
	}

	/**
	 * Returns first element mapped to one of (in that order):
	 * <ul>
	 * <li>specified class
	 * <li>any of specified class' superclasses up to Object.class
	 * <li>any of specified class' interfaces
	 * <li>Void.class or void.class
	 * </ul>
	 * or null if no such mapping exists.
	 */
	default E getElementOfSuperV(Class<?> key) {
		List<Class> keys = getSuperClassesInc(key);
		if (!Void.class.equals(key)) keys.add(Void.class);
		keys.add(void.class);
		for (Class c : keys) {
			List<E> es = getElementsOf(c);
			if (!es.isEmpty())
				return es.get(0);
		}
		return null;
	}
}