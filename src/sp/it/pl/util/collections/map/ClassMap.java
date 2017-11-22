package sp.it.pl.util.collections.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import sp.it.pl.util.collections.map.abstr.MapByClass;

/** Map where key is class. Provides additional methods. */
public class ClassMap<E> extends HashMap<Class<?>,E> implements MapByClass<E> {

	@Override
	public List<E> getElementsOf(Collection<Class<?>> keys) {
		List<E> out = new ArrayList<>();
		for (Class<?> c : keys) {
			E e = get(c);
			if (e!=null) out.add(e);
		}
		return out;
	}

	@Override
	public List<E> getElementsOf(Class<?>... keys) {
		List<E> out = new ArrayList<>();
		for (Class<?> c : keys) {
			E e = get(c);
			if (e!=null) out.add(e);
		}
		return out;
	}
}
