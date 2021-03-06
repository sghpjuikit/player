package sp.it.util.collections.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * List with a preferred item. The item is always contained within list.
 */
public class PrefList<E> extends ArrayList<E> {
	private E pref;

	public PrefList() {
		this(List.of(), null);
	}

	public PrefList(List<E> elements, E pref) {
		addAll(elements);
		setPreferred(pref);
	}

	/**
	 * Get preferred item.
	 *
	 * @return preferred item or null if none
	 */
	public E getPreferred() {
		return pref;
	}

	/**
	 * Get preferred item. First item if no preferred is set.
	 *
	 * @return preferred item or first if none or null list empty
	 */
	public E getPreferredOrFirst() {
		return pref!=null ? pref : isEmpty() ? null : get(0);
	}

	public void addPreferred(E e) {
		pref = e;
		super.add(e);
	}

	public void addPreferred(int index, E element) {
		pref = element;
		super.add(index, element);
	}

	public void addPreferred(E e, boolean preferred) {
		if (preferred) addPreferred(e);
		else add(e);
	}

	public void setPreferred(E e) {
		if (e==null) {
			pref = null;
		} else {
			var i = indexOf(e);
			if (i>=0) pref = get(i);
		}
	}

	@Override
	public boolean remove(Object o) {
		if (pref!=null && pref.equals(o)) pref = null;
		return super.remove(o);
	}

	@Override
	public E remove(int i) {
		if (pref!=null && pref.equals(get(i))) pref = null;
		return super.remove(i);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		if (pref!=null && filter.test(pref)) pref = null;
		return super.removeIf(filter);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (pref!=null && c.contains(pref)) pref = null;
		return super.removeAll(c);
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		int i = pref==null ? -1 : indexOf(pref);
		super.replaceAll(operator);
		if (i!=-1) pref = get(i);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (pref!=null && !c.contains(pref)) pref = null;
		return super.retainAll(c);
	}

	@Override
	public void clear() {
		pref = null;
		super.clear();
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		if (pref!=null) {
			int i = indexOf(pref);
			if (i>=fromIndex && i<toIndex) pref = null;
		}
		super.removeRange(fromIndex, toIndex);
	}

	public <T> PrefList<T> map(Function<? super E, ? extends T> mapper) {
		var l = new PrefList<T>();
		forEach(it -> {
			if (pref==it) l.addPreferred(mapper.apply(it));
			else l.add(mapper.apply(it));
		});
		return l;
	}
}
