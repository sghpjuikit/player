package sp.it.pl.util.collections.map;

import java.util.function.Function;
import sp.it.pl.util.collections.list.PrefList;

public class PrefListMap<E, K> extends CollectionMap<E,K,PrefList<E>> {

	public PrefListMap(Function<E,K> keyMapper) {
		super(PrefList::new, keyMapper);
	}

	public void accumulate(E e, boolean pref) {
		K k = keyMapper.apply(e);
		PrefList<E> c = computeIfAbsent(k, key -> cacheFactory.get());
		c.addPreferred(e, pref);
	}

}