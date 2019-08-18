package sp.it.util.collections.map;

import kotlin.jvm.functions.Function1;
import sp.it.util.collections.list.PrefList;

public class PrefListMap<E, K> extends CollectionMap<E,K,PrefList<E>> {

	public PrefListMap(Function1<E,K> keyMapper) {
		super(PrefList::new, keyMapper);
	}

	public void accumulate(E e, boolean pref) {
		K k = keyMapper.invoke(e);
		PrefList<E> c = computeIfAbsent(k, key -> cacheFactory.get());
		c.addPreferred(e, pref);
	}

}