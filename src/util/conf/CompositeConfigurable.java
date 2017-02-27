package util.conf;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Defines a {@link Configurable} object which contains another Configurable as
 * its part, for example when using Composition.
 * <p/>
 * This enables the object to act as a Configurable proxy for its internal
 * Configurable.
 * <p/>
 * New introduced method returns the internal sub configurable.
 *
 * @author Martin Polakovic
 */
public interface CompositeConfigurable<T> extends Configurable<T> {

	/** {@inheritDoc} */
	@Override
	default Collection<Config<T>> getFields() {
		Collection<Config<T>> l = new ArrayList<>(Configurable.super.getFields());
		getSubConfigurable().forEach(c -> c.getFields().forEach(l::add));
		return l;
	}

	/** {@inheritDoc} */
	@Override
	default Config<T> getField(String name) {
		Config<T> f = Configurable.super.getField(name);
		if (f!=null) return f;
		for (Configurable<T> c : getSubConfigurable()) {
			f = c.getField(name);
			if (f!=null) return f;
		}
		return null;
	}

	/**
	 * Returns configurables composing this object.
	 *
	 * @return collection of subocnfigurable, never null
	 */
	Collection<Configurable<T>> getSubConfigurable();
}