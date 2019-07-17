package sp.it.util.functional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import sp.it.util.collections.list.PrefList;
import sp.it.util.collections.map.PrefListMap;
import sp.it.util.functional.Functors.Parameter;
import sp.it.util.functional.Functors.PƑ;
import sp.it.util.functional.Functors.PƑ0;
import sp.it.util.functional.Functors.PƑ1;
import sp.it.util.functional.Functors.PƑ2;
import sp.it.util.functional.Functors.PƑ3;
import sp.it.util.functional.Functors.Ƒ1;
import sp.it.util.functional.Functors.Ƒ2;
import sp.it.util.functional.Functors.Ƒ3;
import sp.it.util.functional.Functors.Ƒ4;
import static sp.it.util.functional.Util.IDENTITY;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.getEnumConstants;
import static sp.it.util.type.Util.getSuperClassesInc;
import static sp.it.util.type.Util.isEnum;
import static sp.it.util.type.Util.unPrimitivize;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unchecked"})
public class FunctorPool {

	// functor pools must not be accessed directly, as accessor must insert IDENTITY functor
	private final PrefListMap<PƑ,Class<?>> fsI = new PrefListMap<>(pf -> pf.in);
	private final PrefListMap<PƑ,Class<?>> fsO = new PrefListMap<>(pf -> pf.out);
	private final PrefListMap<PƑ,Integer> fsIO = new PrefListMap<>(pf -> Objects.hash(pf.in,pf.out));
	private final Set<Class> cacheStorage = new HashSet<>();

	@SuppressWarnings("unchecked")
	public <I,O> void add(String name, Class<I> i , Class<O> o, Ƒ1<? super I, ? extends O> f) {
		addF(new PƑ0(name,i,o,f));
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def) {
		add(name,i,o,f,new Parameter<>(p1, p1def));
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Parameter<P1> p1) {
		addF(new PƑ1<>(name,i,o,f,p1));
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def) {
		add(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def));
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Parameter<P1> p1, Parameter<P2> p2) {
		addF(new PƑ2<>(name,i,o,f,p1,p2));
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def) {
		add(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def),new Parameter<>(p3, p3def));
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Parameter<P1> p1, Parameter<P2> p2, Parameter<P3> p3) {
		addF(new PƑ3<>(name,i,o,f,p1,p2,p3));
	}

	public <I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<I,O> f, boolean pi, boolean po, boolean pio) {
		addF(new PƑ0<>(name,i,o,f),pi,po,pio);
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def, boolean pi, boolean po, boolean pio) {
		addF(new PƑ1<>(name,i,o,f,new Parameter<>(p1,p1def)),pi,po,pio);
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def, boolean pi, boolean po, boolean pio) {
		addF(new PƑ2<>(name,i,o,f,new Parameter<>(p1,p1def),new Parameter<>(p2,p2def)),pi,po,pio);
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def, boolean pi, boolean po, boolean pio) {
		addF(new PƑ3<>(name,i,o,f,new Parameter<>(p1,p1def),new Parameter<>(p2,p2def),new Parameter<>(p3,p3def)),pi,po,pio);
	}

	public <C extends Comparable<? super C>> void addPredicatesComparable(Class<C> c, C def_val) {
		add("Is less",     c,Boolean.class, (x,y) -> x.compareTo(y)<0,  c,def_val);
		add("Is",          c,Boolean.class, (x,y) -> x.compareTo(y)==0, c,def_val);
		add("Is more",     c,Boolean.class, (x,y) -> x.compareTo(y)>0,  c,def_val);
		// add("Is not less", c,Boolean.class, (x,y) -> x.compareTo(y)>=0, c,def_val);
		// add("Is not",      c,Boolean.class, (x,y) -> x.compareTo(y)!=0, c,def_val);
		// add("Is not more", c,Boolean.class, (x,y) -> x.compareTo(y)<=0, c,def_val);
	}

	/** Add function to the pool. */
	public void addF(PƑ f) {
		fsI.accumulate(f);
		fsO.accumulate(f);
		fsIO.accumulate(f);
	}

	/** Add function to the pool and sets as preferred according to parameters. */
	public void addF(PƑ f, boolean i, boolean o, boolean io) {
		fsI.accumulate(f, i);
		fsO.accumulate(f, o);
		fsIO.accumulate(f, io);
	}

	/** Remove function from the pool. */
	public void remF(PƑ f) {
		fsI.deAccumulate(f);
		fsO.deAccumulate(f);
		fsIO.deAccumulate(f);
	}

	@SuppressWarnings("unckeched")
	private <T> void addSelfFunctor(Class<T> c) {
		if (cacheStorage.contains(c)) return;
		cacheStorage.add(c);

		// add self functor
		addF(new PƑ0("As self", c, c, IDENTITY));

		// add enum is predicates
		if (isEnum(c))
			add("Is", c,Boolean.class, (a,b) -> a==b, c, (T) getEnumConstants(c)[0], false,false,true);
	}

	/** Returns all functions taking input I. */
	@SuppressWarnings("unckeched")
	public <I> PrefList<PƑ<I,?>> getI(Class<I> i) {
		addSelfFunctor(i);
		return (PrefList) fsI.getElementsOf(getSuperClassesInc(unPrimitivize(i)));
	}

	/** Returns all functions producing output O. */
	@SuppressWarnings("unckeched")
	public <O> PrefList<PƑ<?,O>> getO(Class<O> o) {
		addSelfFunctor(o);
		List<?> ll = fsO.get(unPrimitivize(o));
		return ll==null ? new PrefList<>() : (PrefList) ll;
	}

	/** Returns all functions taking input I and producing output O. */
	@SuppressWarnings("unckeched")
	public <I,O> PrefList<PƑ<I,O>> getIO(Class<I> i, Class<O> o) {
		addSelfFunctor(i);
		addSelfFunctor(o);

		// this is rather messy, but works
		// we accumulate result for all superclasses & retain first found preferred element, while
		// keeping duplicate elements in check
		PrefList pl = new PrefList();
		Object pref = null;
		for (Class c : getSuperClassesInc(unPrimitivize(i))) {
			List l = fsIO.get(Objects.hash(c, unPrimitivize(o)));
			PrefList ll = l==null ? null : (PrefList) l;
			if (ll!=null) {
				if (pref==null && ll.getPreferredOrFirst()!=null) pref = ll.getPreferredOrFirst();
				pl.addAll(ll);
			}
		}
		Object prefCopy = pref;
		pl.removeIf(e -> e==prefCopy || e==null);
		if (pref!=null) pl.addPreferred(pref);
		if (pl.getPreferredOrFirst()==null && !pl.isEmpty()) {
			Object e = pl.get(pl.size()-1);
			pl.remove(pl.size()-1);
			pl.addPreferred(e);
		}

		return pl;

		// old impl, ignores super classes
		//        PrefList l = (PrefList) fsIO.get(Objects.hash(unPrimitivize(i),unPrimitivize(o)));
		//        return l==null ? new PrefList() : l;
	}

	/** Returns all functions taking input IO and producing output IO. */
	public <IO> PrefList<PƑ<IO,IO>> getIO(Class<IO> io) {
		return getIO(io, io);
	}

	public <I,O> PƑ<I,O> getPF(String name, Class<I> i, Class<O> o) {
		@SuppressWarnings("unchecked")
		List<PƑ<I,O>> l = (List) fsIO.get(Objects.hash(unPrimitivize(i), unPrimitivize(o)));
		return l==null ? null : stream(l).filter(f -> f.name.equals(name)).findAny().orElse(null);
	}

	public <I> PƑ<I,?> getPrefI(Class<I> i) {
		PrefList<PƑ<I,?>> pl = getI(i);
		return pl==null ? null : pl.getPreferredOrFirst();
	}

	public <O> PƑ<?,O> getPrefO(Class<O> o) {
		PrefList<PƑ<?,O>> l = getO(o);
		return l==null ? null : l.getPreferredOrFirst();
	}

	public <I,O> PƑ<I,O> getPrefIO(Class<I> i, Class<O> o) {
		PrefList<PƑ<I,O>> l = getIO(i,o);
		return l==null ? null : l.getPreferredOrFirst();
	}

	public <IO> PƑ<IO,IO> getPrefIO(Class<IO> io) {
		return getPrefIO(io,io);
	}

}