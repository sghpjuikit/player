package sp.it.util.functional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kotlin.reflect.KClass;
import sp.it.util.collections.list.PrefList;
import sp.it.util.collections.map.Map2D.Key;
import sp.it.util.collections.map.PrefListMap;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Functors.F2;
import sp.it.util.functional.Functors.F3;
import sp.it.util.functional.Functors.F4;
import sp.it.util.functional.Functors.PF;
import sp.it.util.functional.Functors.PF0;
import sp.it.util.functional.Functors.PF1;
import sp.it.util.functional.Functors.PF2;
import sp.it.util.functional.Functors.PF3;
import sp.it.util.functional.Functors.Parameter;
import sp.it.util.type.VType;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static kotlin.sequences.SequencesKt.toList;
import static sp.it.util.functional.Util.IDENTITY;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.getEnumConstants;
import static sp.it.util.type.Util.isEnum;
import static sp.it.util.type.UtilKt.superKClassesInc;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unchecked"})
public class FunctorPool {
	// functor pools must not be accessed directly, as accessor must insert IDENTITY functor
	private final PrefListMap<PF,KClass<?>> fsI = new PrefListMap<>(pf -> getKotlinClass(pf.in));
	private final PrefListMap<PF,KClass<?>> fsO = new PrefListMap<>(pf -> getKotlinClass(pf.out));
	private final PrefListMap<PF,Key<KClass<?>, KClass<?>>> fsIO = new PrefListMap<>(pf -> new Key<>(getKotlinClass(pf.in),getKotlinClass(pf.out)));
	private final Set<Class> cacheStorage = new HashSet<>();

	@SuppressWarnings("unchecked")
	public <I,O> void add(String name, Class<I> i , Class<O> o, F1<? super I, ? extends O> f) {
		addF(new PF0(name,i,o,f));
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, F2<I,P1,O> f, Class<P1> p1, P1 p1def) {
		add(name,i,o,new Parameter<>(new VType<>(p1, true), p1def),f);
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Parameter<P1> p1, F2<I,P1,O> f) {
		addF(new PF1<>(name,i,o,f,p1));
	}

	public <I,P1,P2,O> void add(String name, Class<I> i, Class<O> o, F3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def) {
		add(name,i,o,new Parameter<>(new VType<>(p1, true), p1def),new Parameter<>(new VType<>(p2, true), p2def),f);
	}

	public <I,P1,P2,O> void add(String name, Class<I> i, Class<O> o, Parameter<P1> p1, Parameter<P2> p2, F3<I,P1,P2,O> f) {
		addF(new PF2<>(name,i,o,f,p1,p2));
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i, Class<O> o, F4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def) {
		add(name,i,o,new Parameter<>(new VType<>(p1, true), p1def),new Parameter<>(new VType<>(p2, true), p2def),new Parameter<>(new VType<>(p3, true), p3def),f);
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i, Class<O> o, Parameter<P1> p1, Parameter<P2> p2, Parameter<P3> p3, F4<I,P1,P2,P3,O> f) {
		addF(new PF3<>(name,i,o,f,p1,p2,p3));
	}

	public <I,O> void add(String name, Class<I> i , Class<O> o, boolean pi, boolean po, boolean pio, F1<I,O> f) {
		addF(new PF0<>(name,i,o,f),pi,po,pio);
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, F2<I,P1,O> f, Class<P1> p1, P1 p1def, boolean pi, boolean po, boolean pio) {
		addF(new PF1<>(name,i,o,f,new Parameter<>(new VType<>(p1, true),p1def)),pi,po,pio);
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Parameter<P1> p1, boolean pi, boolean po, boolean pio, F2<I,P1,O> f) {
		addF(new PF1<>(name,i,o,f,p1),pi,po,pio);
	}

	public <I,P1,P2,O> void add(String name, Class<I> i, Class<O> o, F3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def, boolean pi, boolean po, boolean pio) {
		addF(new PF2<>(name,i,o,f,new Parameter<>(new VType<>(p1, true),p1def),new Parameter<>(new VType<>(p2, true),p2def)),pi,po,pio);
	}

	public <I,P1,P2,O> void add(String name, Class<I> i, Class<O> o, Parameter<P1> p1, Parameter<P2> p2, boolean pi, boolean po, boolean pio, F3<I,P1,P2,O> f) {
		addF(new PF2<>(name,i,o,f,p1, p2),pi,po,pio);
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i, Class<O> o, F4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def, boolean pi, boolean po, boolean pio) {
		addF(new PF3<>(name,i,o,f,new Parameter<>(new VType<>(p1, true),p1def),new Parameter<>(new VType<>(p2, true),p2def),new Parameter<>(new VType<>(p3, true),p3def)),pi,po,pio);
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i, Class<O> o, Parameter<P1> p1, Parameter<P2> p2, Parameter<P3> p3, boolean pi, boolean po, boolean pio, F4<I,P1,P2,P3,O> f) {
		addF(new PF3<>(name,i,o,f,p1, p2, p3),pi,po,pio);
	}

	public <C extends Comparable<? super C>> void addPredicatesComparable(Class<C> c, C defaultValue) {
		add("< Less", c, Boolean.class, new Parameter<>(new VType<>(c, false), defaultValue), (x,y) -> x.compareTo(y)<0);
		add("= Is",   c, Boolean.class, new Parameter<>(new VType<>(c, false), defaultValue), (x,y) -> x.compareTo(y)==0);
		add("> More", c, Boolean.class, new Parameter<>(new VType<>(c, false), defaultValue), (x,y) -> x.compareTo(y)>0);
	}

	/** Add function to the pool. */
	public void addF(PF<?,?> f) {
		fsI.accumulate(f);
		fsO.accumulate(f);
		fsIO.accumulate(f);
	}

	/** Add function to the pool and sets as preferred according to parameters. */
	public void addF(PF<?,?> f, boolean i, boolean o, boolean io) {
		fsI.accumulate(f, i);
		fsO.accumulate(f, o);
		fsIO.accumulate(f, io);
	}

	/** Remove function from the pool. */
	public void remF(PF<?,?> f) {
		fsI.deAccumulate(f);
		fsO.deAccumulate(f);
		fsIO.deAccumulate(f);
	}

	@SuppressWarnings("unckeched")
	private <T> void addSelfFunctor(Class<T> c) {
		if (cacheStorage.contains(c)) return;
		cacheStorage.add(c);

		// add self functor
		addF(new PF0("As self", c, c, IDENTITY));

		// add enum is predicates
		if (isEnum(c))
			add("Is", c, Boolean.class, (a,b) -> a==b, c, (T) getEnumConstants(c)[0], false,false,true);
	}

	/** Returns all functions taking input I. */
	@SuppressWarnings("unckeched")
	public <I> PrefList<PF<I,?>> getI(Class<I> i) {
		addSelfFunctor(i);
		return (PrefList) fsI.getElementsOf(toList(superKClassesInc(getKotlinClass(i))));
	}

	/** Returns all functions producing output O. */
	@SuppressWarnings("unckeched")
	public <O> PrefList<PF<?,O>> getO(Class<O> o) {
		addSelfFunctor(o);
		List<?> ll = fsO.get(getKotlinClass(o));
		return ll==null ? new PrefList<>() : (PrefList) ll;
	}

	/** Returns all functions taking input I and producing output O. */
	@SuppressWarnings("unckeched")
	public <I,O> PrefList<PF<I,O>> getIO(Class<I> i, Class<O> o) {
		addSelfFunctor(i);
		addSelfFunctor(o);

		// this is rather messy, but works
		// we accumulate result for all superclasses & retain first found preferred element, while
		// keeping duplicate elements in check
		PrefList pl = new PrefList();
		Object pref = null;
		for (KClass<?> c : toList(superKClassesInc(getKotlinClass(i)))) {
			List l = fsIO.get(new Key<>(c, getKotlinClass(o)));
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
	public <IO> PrefList<PF<IO,IO>> getIO(Class<IO> io) {
		return getIO(io, io);
	}

	public <I,O> PF<I,O> getPF(String name, Class<I> i, Class<O> o) {
		@SuppressWarnings("unchecked")
		List<PF<I,O>> l = (List) fsIO.get(new Key<>(getKotlinClass(i), getKotlinClass(o)));
		return l==null ? null : stream(l).filter(f -> f.name.equals(name)).findAny().orElse(null);
	}

	public <I> PF<I,?> getPrefI(Class<I> i) {
		PrefList<PF<I,?>> pl = getI(i);
		return pl==null ? null : pl.getPreferredOrFirst();
	}

	public <O> PF<?,O> getPrefO(Class<O> o) {
		PrefList<PF<?,O>> l = getO(o);
		return l==null ? null : l.getPreferredOrFirst();
	}

	public <I,O> PF<I,O> getPrefIO(Class<I> i, Class<O> o) {
		PrefList<PF<I,O>> l = getIO(i,o);
		return l==null ? null : l.getPreferredOrFirst();
	}

	public <IO> PF<IO,IO> getPrefIO(Class<IO> io) {
		return getPrefIO(io,io);
	}

}