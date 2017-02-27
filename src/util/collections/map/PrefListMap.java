package util.collections.map;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import util.collections.list.PrefList;

/**
 * @author Martin Polakovic
 */
@SuppressWarnings("unchecked")
public class PrefListMap<E, K> extends ListMap<E,K> {

	public PrefListMap(Function<E,K> keyMapper) {
		super(PrefList::new, keyMapper);
	}

	public void accumulate(E e, boolean pref) {
		// get key
		K k = keyMapper.apply(e);
		// get cache storage with key & build new if not yet built
		PrefList<E> c = (PrefList) get(k);
		if (c==null) {
			c = (PrefList) cacheFactory.get();
			put(k, c);
		}
		c.addPreferred(e, pref);
	}

	// TODO: finish implementing
//    @Override
//    public PrefList<E> get(Object key) {
//        List<E> l = super.get(key);
//        return l==null ? new PrefList() : (PrefList) super.get(key);
//    }

	@Override
	public PrefList<E> getElementsOf(Collection<K> keys) {
		return (PrefList) super.getElementsOf(keys);
	}

	@SafeVarargs
	@Override
	public final PrefList<E> getElementsOf(K... keys) {
		return (PrefList) super.getElementsOf(keys); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public PrefList<E> getElementsOf(Stream<K> keys) {
		return (PrefList) super.getElementsOf(keys); //To change body of generated methods, choose Tools | Templates.
	}

}