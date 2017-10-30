package util.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import javafx.util.Callback;
import kotlin.Unit;
import kotlin.jvm.functions.*;
import util.SwitchException;
import util.access.V;
import util.conf.Config;
import util.conf.Config.AccessorConfig;
import util.conf.Configurable;
import static util.dev.Util.noØ;
import static util.functional.Util.*;
import static util.type.Util.unPrimitivize;

@SuppressWarnings({"unchecked","unused","NonAsciiCharacters"})
public interface Functors {

	FunctorPool pool = new FunctorPool();

	/** Marker interface for lambda. */
	interface Λ {}

	/** Marker interface for lambda denoting its first input and output. */
	interface IO<I, O> extends Λ {
		// not sure if good idea
		// for default impl i want to use reflection to inspect generic type in runtime
		// subclasses may want to override, like PF or TypeAwareF
		// default Class<? super I> getTypeInput() {}
		// default Class<? super I> getTypeOutput() {}
	}

	@FunctionalInterface
	interface Ƒ extends Λ, IO<Void,Void>, Function0<Unit>, Runnable {
		void apply();

		@Override
		default Unit invoke() {
			return Unit.INSTANCE;
		}

		/** Equivalent to {@link #apply()}. Exists for compatibility with {@link Runnable}. */
		default void run() {
			apply();
		}
	}

	/**
	 * Supplier function
	 *
	 * @param <O> output type
	 */
	@FunctionalInterface
	interface Ƒ0<O> extends Λ, IO<Void,O>, Function0<O>, Supplier<O> {
		O apply();

		@Override
		default O invoke() {
			return apply();
		}

		/** Equivalent to {@link #apply()}. Exists for compatibility with {@link Supplier}. */
		default O get() {
			return apply();
		}

		default <M> Ƒ0<M> map(Ƒ1<? super O,? extends M> f) {
			return () -> f.apply(apply());
		}

		/**
		 * Returns equivalent function to this returning no output. The computation will still
		 * take place as normal, so this function should have side effects. If it does not, a
		 * function that does nothing should be used instead of this method.
		 */
		default Ƒ toƑ() {
			return this::apply;
		}
	}

	/**
	 * Supplier function throwing an exception.
	 * <p/>
	 * Due to the signature, it is impossible to extend {@link Consumer}
	 *
	 * @param <O> output type
	 */
	@FunctionalInterface
	interface Ƒ0E<O, E extends Throwable> extends Λ, IO<Void,O> {
		O apply() throws E;

		default Ƒ0E<O,E> onEx(O or, Class<?>... ecs) {
			return () -> {
				try {
					return apply();
				} catch (Throwable e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
					throw e;
				}
			};
		}
	}

	/**
	 * Function. Provides additional methods.
	 * <p/>
	 * Can also be used as a callback (it falls back to the {@link #apply(java.lang.Object)}
	 * method) or consumer (same and in addition ignores output - this is not pointless due to side
	 * effects - consumer by nature relies on side effects.)
	 */
	@FunctionalInterface
	interface Ƒ1<I, O> extends Λ, IO<I,O>, Function<I,O>, Function1<I,O>, Callback<I,O>, Consumer<I> {

		static Ƒ1<Void,Void> f1(Runnable r) {
			return i -> {
				r.run();
				return null;
			};
		}

		static <T> Ƒ1<Void,T> f1(Supplier<T> s) {
			return i -> s.get();
		}

		static <T> Ƒ1<T,Void> f1(Consumer<T> c) {
			return i -> {
				c.accept(i);
				return null;
			};
		}

		@Override
		O apply(I i);

		@Override
		default O invoke(I i) {
			return apply(i);
		}

		/** Equivalent to {@link #apply(Object)}. Exists for compatibility with {@link Callback}. */
		@Override
		default O call(I i) {
			return apply(i);
		}

		/** Equivalent to {@link #apply(Object)}}, ignoring the result. Exists for compatibility with {@link Consumer}. */
		@Override
		default void accept(I i) {
			apply(i); // and ignore result as a proper Consumer
		}

		/** Partially applies this function with 1st parameter. */
		default Ƒ0<O> toƑ0(I i) {
			return () -> apply(i);
		}

		/**
		 * Returns function equivalent to this, except for when certain exception types are thrown.
		 * These will be caught and alternative output returned.
		 */
		default Ƒ1<I,O> onEx(O or, Class<?>... ecs) {
			return i -> {
				try {
					return apply(i);
				} catch (Exception e) {
					for (Class<?> ec : ecs)
						if (ec.isAssignableFrom(ec.getClass()))
							return or;
					throw e;
				}
			};
		}

		/** Lazy version of {@link #onEx(java.lang.Object, java.lang.Class...) } */
		default Ƒ1<I,O> onEx(Supplier<O> or, Class<?>... ecs) {
			return i -> {
				try {
					return apply(i);
				} catch (Exception e) {
					for (Class<?> ec : ecs)
						if (ec.isAssignableFrom(ec.getClass()))
							return or.get();
					throw e;
				}
			};
		}

		/** Function version of {@link #onEx(java.lang.Object, java.lang.Class...) }. */
		default Ƒ1<I,O> onEx(Ƒ1<I,O> or, Class<?>... ecs) {
			return i -> {
				try {
					return apply(i);
				} catch (Exception e) {
					for (Class<?> ec : ecs)
						if (ec.isAssignableFrom(ec.getClass()))
							return or.apply(i);
					throw e;
				}
			};
		}

		@Override
		default <R> Ƒ1<I,R> andThen(Function<? super O,? extends R> after) {
			noØ(after);
			return (I t) -> after.apply(apply(t));
		}

		//* Purely to avoid ambiguity of method overloading. Same as andThen(Function). */
		default <R> Ƒ1<I,R> andThen(Ƒ1<? super O,? extends R> after) {
			noØ(after);
			return (I t) -> after.apply(apply(t));
		}

		/**
		 * Creates function which runs the action afterwards
		 *
		 * @param after action that executes right after computation is done and before returning the output
		 * @return function identical to this one, but one which runs the runnable after it computes
		 */
		default Ƒ1<I,O> andThen(Runnable after) {
			noØ(after);
			return i -> {
				O o = apply(i);
				after.run();
				return o;
			};
		}

		// this change return type from Consumer to Function in a type safe way!!
		@Override
		default Ƒ1<I,Void> andThen(Consumer<? super I> after) {
			return i -> {
				apply(i);
				after.accept(i);
				return null;
			};
		}

		@Override
		default <R> Ƒ1<R,O> compose(Function<? super R,? extends I> before) {
			noØ(before);
			return (R v) -> apply(before.apply(v));
		}

		/**
		 * @param mutator consumer that takes the input of this function and applies it on output of this function after
		 * this function finishes
		 * @return composed function that applies this function to its input and then mutates the output before
		 * returning it.
		 */
		default Ƒ1<I,O> andApply(Consumer<O> mutator) {
			return in -> {
				O o = apply(in);
				mutator.accept(o);
				return o;
			};
		}

		/**
		 * Similar to {@link #andApply(java.util.function.Consumer)} but the mutator takes
		 * additional parameter - initial input of this function.
		 *
		 * @param mutator consumer that takes the input of this function and applies it on output of this function after
		 * this function finishes
		 * @return composed function that applies this function to its input and then mutates the output before
		 * returning it.
		 */
		default Ƒ1<I,O> andApply(BiConsumer<I,O> mutator) {
			return in -> {
				O o = apply(in);
				mutator.accept(in, o);
				return o;
			};
		}

		/**
		 * Similar to {@link #andThen(java.util.function.Function)} but the mutator
		 * takes additional parameter - the original input to this function.
		 *
		 * @param mutator consumer that takes the input of this function and applies it on output of this function after
		 * this function finishes
		 * @return composed function that applies this function to its input and then applies the mutator before
		 * returning it.
		 */
		default <O2> Ƒ1<I,O2> andThen(Ƒ2<I,O,O2> mutator) {
			return in -> {
				O o = apply(in);
				return mutator.apply(in, o);
			};
		}

		/**
		 * Returns nonnull version of this f, which returns its input instead of null. The input
		 * type must conform to output type! This mostly makes sense when input and output type
		 * match.
		 */
		@SuppressWarnings("unchecked")
		default Ƒ1<I,O> nonNull() {
			return in -> {
				O out = apply(in);
				return out==null ? (O) in : out;
			};
		}

		default Ƒ1<I,O> nonNull(O or) {
			return andThen(o -> o==null ? or : o);
		}

		default Ƒ1<I,O> passNull() {
			return in -> in==null ? null : apply(in);
		}

		@SuppressWarnings("unchecked")
		default Ƒ1<I,O> wrap(NullIn i, NullOut o) {
			if (i==NullIn.NULL && o==NullOut.NULL)
				return in -> in==null ? null : apply(in);
			if (i==NullIn.APPLY && o==NullOut.NULL)
				return this;
			if (i==NullIn.APPLY && o==NullOut.INPUT)
				return in -> {
					O out = apply(in);
					return out==null ? (O) in : out;
				};
			if (i==NullIn.NULL && o==NullOut.INPUT)
				return in -> {
					if (in==null) return null;
					O out = apply(in);
					return out==null ? (O) in : out;
				};

			throw new AssertionError("Illegal switch case");
		}

		default Ƒ1<I,O> onNullIn(OnNullIn ni) {
			if (ni==OnNullIn.NULL)
				return i -> i==null ? null : apply(i);
			if (ni==OnNullIn.APPLY)
				return this;
			if (ni==OnNullIn.VALUE)
				throw new IllegalArgumentException("No value provided");
			throw new SwitchException(ni);
		}

		default Ƒ1<I,O> onNullIn(OnNullIn ni, O or) {
			if (ni==OnNullIn.NULL)
				throw new SwitchException(ni);
			if (ni==OnNullIn.APPLY)
				throw new SwitchException(ni);
			if (ni==OnNullIn.VALUE)
				return i -> i==null ? or : apply(i);
			throw new SwitchException(ni);
		}
	}

	/**
	 * Predicate.
	 * <p/>
	 * {@link Ƒ1} can not extend Predicate, doing so would not be type safe, hence this subclass.
	 * This class also preserves predicate identity during predicate combination operations.
	 */
	@SuppressWarnings("unchecked")
	@FunctionalInterface
	interface ƑP<I> extends Ƒ1<I,Boolean>, Predicate<I> {

		/** Equivalent to {@link #apply(Object)}}. Exists for compatibility with {@link Predicate}. */
		@Override
		default boolean test(I i) {
			return apply(i);
		}

		@Override
		default ƑP<I> negate() {
			// we should retain the predicate identity if possible. Of course it can be leveraged
			// only if unique predicates are used, not dynamically created ones, e.g. (o -> o==null)
			if (this==ISØ) return (ƑP) ISNTØ;
			else if (this==ISNTØ) return (ƑP) ISØ;
			else if (this==IS) return (ƑP) IS;
			else if (this==ISNT) return (ƑP) ISNT;
			return i -> !apply(i);
		}

		@Override
		default ƑP<I> and(Predicate<? super I> p) {
			// we should retain the predicate identity if possible
			if (this==p) return this;
			else if ((this==ISØ && p==ISNTØ) || (this==ISNTØ && p==ISØ)) return (ƑP) ISNT;
			else if (p==ISNT || this==ISNT) return (ƑP) ISNT;
			return i -> apply(i) && apply(i);
		}

		@Override
		default ƑP<I> or(Predicate<? super I> p) {
			// we should retain the predicate identity if possible
			if (this==p) return this;
			else if ((this==ISØ && p==ISNTØ) || (this==ISNTØ && p==ISØ)) return (ƑP) IS;
			else if (this==IS || p==IS) return (ƑP) IS;
			return i -> apply(i) || apply(i);
		}

	}

	/**
	 * Function throwing an exception.
	 * <p/>
	 * Due to the signature, it is impossible to extend {@link Consumer}
	 */
	@FunctionalInterface
	interface Ƒ1E<I, O, E extends Throwable> extends Λ, IO<I,O> {
		O apply(I i) throws E;

		default Ƒ1E<I,O,E> onEx(O or, Class<?>... ecs) {
			return i -> {
				try {
					return apply(i);
				} catch (Throwable e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
					throw e;
				}
			};
		}
	}

	/**
	 * {@link Consumer} which throws an exception.
	 * <p/>
	 * Consumer version of {@link Ƒ1E}, so lambda expression does not need to return void (null)
	 * at the end
	 */
	// this class is ~pointless, although now lambda does not have to return null like in case of F1E,
	// but now the some method takes parameter of this class. Which will prevent
	// other F1E from being used!
	@FunctionalInterface
	interface ƑEC<I, E extends Throwable> extends Ƒ1E<I,Void,E> {

		@Override
		default Void apply(I i) throws E {
			accept(i);
			return null;
		}

		void accept(I i) throws E;
	}

	@FunctionalInterface
	interface Ƒ2<I, I2, O> extends Λ, IO<I,O>, BiFunction<I,I2,O>, Function2<I,I2,O> {
		@Override
		O apply(I i, I2 i2);

		@Override
		default O invoke(I i, I2 i2) {
			return apply(i, i2);
		}

		default Ƒ1<I,O> toƑ1(I2 i2) {
			return (i) -> apply(i, i2);
		}

		default Ƒ2<I,I2,O> onEx(O or, Class<?>... ecs) {
			return (i1, i2) -> {
				try {
					return apply(i1, i2);
				} catch (Exception e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
					throw e;
				}
			};
		}
	}

	@FunctionalInterface
	interface Ƒ3<I, I2, I3, O> extends Λ, IO<I,O>, Function3<I,I2,I3,O> {
		O apply(I i, I2 i2, I3 i3);

		@Override
		default O invoke(I i, I2 i2, I3 i3) {
			return apply(i, i2, i3);
		}

		default Ƒ2<I,I2,O> toƑ2(I3 i3) {
			return (i, i2) -> apply(i, i2, i3);
		}

		default Ƒ3<I,I2,I3,O> onEx(O or, Class<?>... ecs) {
			return (i1, i2, i3) -> {
				try {
					return apply(i1, i2, i3);
				} catch (Exception e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
					throw e;
				}
			};
		}
	}

	@FunctionalInterface
	interface Ƒ4<I, I2, I3, I4, O> extends Λ, IO<I,O>, Function4<I,I2,I3,I4,O> {
		O apply(I i, I2 i2, I3 i3, I4 i4);

		@Override
		default O invoke(I i, I2 i2, I3 i3, I4 i4) {
			return apply(i, i2, i3, i4);
		}

		default Ƒ3<I,I2,I3,O> toƑ3(I4 i4) {
			return (i, i2, i3) -> apply(i, i2, i3, i4);
		}

		default Ƒ4<I,I2,I3,I4,O> onEx(O or, Class<?>... ecs) {
			return (i1, i2, i3, i4) -> {
				try {
					return apply(i1, i2, i3, i4);
				} catch (Exception e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
					throw e;
				}
			};
		}
	}

	@FunctionalInterface
	interface Ƒ5<I, I2, I3, I4, I5, O> extends Λ, IO<I,O>, Function5<I,I2,I3,I4,I5,O> {
		O apply(I i, I2 i2, I3 i3, I4 i4, I5 i5);

		@Override
		default O invoke(I i, I2 i2, I3 i3, I4 i4, I5 i5) {
			return apply(i, i2, i3, i4, i5);
		}

		default Ƒ4<I,I2,I3,I4,O> toƑ4(I5 i5) {
			return (i, i2, i3, i4) -> apply(i, i2, i3, i4, i5);
		}

		default Ƒ5<I,I2,I3,I4,I5,O> onEx(O or, Class<?>... ecs) {
			return (i1, i2, i3, i4, i5) -> {
				try {
					return apply(i1, i2, i3, i4, i5);
				} catch (Exception e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(ec.getClass())) return or;
					throw e;
				}
			};
		}
	}

	@FunctionalInterface
	interface Ƒ6<I, I2, I3, I4, I5, I6, O> extends Λ, IO<I,O>, Function6<I,I2,I3,I4,I5,I6,O> {
		O apply(I i, I2 i2, I3 i3, I4 i4, I5 i5, I6 i6);

		@Override
		default O invoke(I i, I2 i2, I3 i3, I4 i4, I5 i5, I6 i6) {
			return apply(i, i2, i3, i4, i5, i6);
		}

		default Ƒ5<I,I2,I3,I4,I5,O> toƑ5(I6 i6) {
			return (i, i2, i3, i4, i5) -> apply(i, i2, i3, i4, i5, i6);
		}

		default Ƒ6<I,I2,I3,I4,I5,I6,O> onEx(O or, Class<?>... ecs) {
			return (i1, i2, i3, i4, i5, i6) -> {
				try {
					return apply(i1, i2, i3, i4, i5, i6);
				} catch (Exception e) {
					for (Class<?> ec : ecs) if (ec.isAssignableFrom(ec.getClass())) return or;
					throw e;
				}
			};
		}
	}

	enum NullIn {
		NULL,
		APPLY
	}

	enum NullOut {
		NULL,
		INPUT
	}

	enum OnNullIn {
		NULL,
		APPLY,
		VALUE
	}

	class Parameter<P> {
		public final Class<P> type;
		public final P defaultValue;
		public final String name;
		public final String description;

		public Parameter(Class<P> type, P defaultValue) {
			this("", "", type, defaultValue);
			noØ(type, defaultValue);
		}

		public Parameter(String name, String description, Class<P> type, P defaultValue) {
			this.name = name.isEmpty() ? "<value>" : name;
			this.description = description.isEmpty() ? this.name : description;
			this.type = unPrimitivize(type);
			this.defaultValue = defaultValue;
			noØ(type, description, type, defaultValue);
		}

		public static <P> Parameter<P> p(String name, String description, Class<P> type, P defaultValue) {
			return new Parameter<>(name, description, type, defaultValue);
		}
	}

	interface Parameterized<P> {
		List<Parameter<? extends P>> getParameters();
	}

	// parameterized function - variadic I -> O function factory with parameters
	abstract class PƑ<I, O> implements Ƒ2<I,Object[],O>, Parameterized<Object> {
		public final String name;
		public final Class<I> in;
		public final Class<O> out;
		private final IO<I,O> ff;

		@SuppressWarnings("unchecked")
		public PƑ(String name, Class<I> in, Class<O> out, IO<I,O> f) {
			this.name = name;
			this.in = unPrimitivize(in);
			this.out = unPrimitivize(out);
			this.ff = f;
		}

		public Ƒ1<I,O> toFunction() {
			return i -> apply(i, new Object[]{});
		}

		@Override
		public abstract O apply(I t, Object... is);

		@Override
		public Ƒ1<I,O> toƑ1(Object... is) {
			// retain predicate identity
			if (isAny(ff, IDENTITY, ISØ, ISNTØ, IS, ISNT)) return (Ƒ1<I,O>) ff;
			return new TypeAwareƑ<>(i -> apply(i, is), in, out);
			// return i -> apply(i, is); // would not preserve I,O types
		}

	}

	// solely to hide generic parameter of PF above, the 3rd parameter (F) is implementation
	// detail - we do not want it to pollute external code, in fact this parameter exists solely
	// so PƑ can access its underlying function, while not breaking type safety for subclasses
	abstract class PƑB<I, O, F extends IO<I,O>> extends PƑ<I,O> {

		public final F f;

		public PƑB(String name, Class<I> in, Class<O> out, F f) {
			super(name, in, out, f);
			this.f = f;
		}

	}

	/**
	 * Parametric function, {@code In -> Out} function defined as {@code (In, P1, P2, ..., Pn) -> Out} variadic
	 * function with parameters. Formally, the signature is {@code (In, Param...) -> Out}, but the parameters are
	 * degrees of freedom, fixing of which collapses the signature to {@code In -> Out} (as in partial application),
	 * which can be applied on the input. While the parameters themselves are technically inputs, they are transparent
	 * for the function user, (which should only see the collapsed signature) and serve as a variadic generalisation
	 * of a function - to express function of any number of parameters equally. This is useful for example for ui
	 * function builders.
	 */
	class PƑ0<I, O> extends PƑB<I,O,Ƒ1<I,O>> {

		public PƑ0(String _name, Class<I> i, Class<O> o, Ƒ1<I,O> f) {
			super(_name, i, o, f);
		}

		@Override
		public List<Parameter<?>> getParameters() {
			return listRO();
		}

		@Override
		public O apply(I t, Object... ps) {
			return f.apply(t);
		}

	}

	/** Unary parametric function. */
	class PƑ1<I, P1, O> extends PƑB<I,O,Ƒ2<I,P1,O>> {
		private Parameter<P1> p1;

		public PƑ1(String _name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Parameter<P1> p1) {
			super(_name, i, o, f);
			this.p1 = p1;
		}

		@Override
		public List<Parameter<?>> getParameters() {
			return list(p1);
		}

		@Override
		public O apply(I t, Object... ps) {
			return f.apply(t, (P1) ps[0]);
		}
	}

	/** Binary parametric function. */
	class PƑ2<I, P1, P2, O> extends PƑB<I,O,Ƒ3<I,P1,P2,O>> {
		private Parameter<P1> p1;
		private Parameter<P2> p2;

		public PƑ2(String _name, Class<I> i, Class<O> o, Ƒ3<I,P1,P2,O> f, Parameter<P1> p1, Parameter<P2> p2) {
			super(_name, i, o, f);
			this.p1 = p1;
			this.p2 = p2;
		}

		@Override
		public List<Parameter<?>> getParameters() {
			return list(p1, p2);
		}

		@Override
		public O apply(I t, Object... ps) {
			return f.apply(t, (P1) ps[0], (P2) ps[1]);
		}
	}

	/** Tertiary  parametric function. */
	class PƑ3<I, P1, P2, P3, O> extends PƑB<I,O,Ƒ4<I,P1,P2,P3,O>> {
		private Parameter<P1> p1;
		private Parameter<P2> p2;
		private Parameter<P3> p3;

		public PƑ3(String _name, Class<I> i, Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Parameter<P1> p1, Parameter<P2> p2, Parameter<P3> p3) {
			super(_name, i, o, f);
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
		}

		@Override
		public List<Parameter<?>> getParameters() {
			return list(p1, p2, p3);
		}

		@Override
		public O apply(I t, Object... ps) {
			return f.apply(t, (P1) ps[0], (P2) ps[1], (P3) ps[2]);
		}
	}

	/** N-ary parametric function. */
	class PƑn<I, O> extends PƑB<I,O,Ƒ2<I,Object[],O>> {
		private Parameter<Object>[] ps;

		public PƑn(String _name, Class<I> i, Class<O> o, Ƒ2<I,Object[],O> f, Parameter<Object>[] ps) {
			super(_name, i, o, f);
			this.ps = ps;
		}

		@Override
		public List<Parameter<?>> getParameters() {
			return list(ps);
		}

		@Override
		public O apply(I t, Object... ps) {
			return f.apply(t, ps);
		}
	}

	class CƑ<I, O> implements Ƒ1<I,O>, Configurable<Object> {

		final PƑ<I,O> pf;
		private final List<Config<Object>> cs = new ArrayList<>();

		public CƑ(PƑ<I,O> pf) {
			this.pf = pf;
			pf.getParameters().forEach(p -> {
				V<Object> a = new V<>(p.defaultValue);
				cs.add(new AccessorConfig(p.type, p.name, p.description, a::setValue, a::getValue));
			});
		}

		@Override
		public O apply(I i) {
			return pf.apply(i, cs.stream().map(Config::getValue).toArray());
		}

	}

	class TypeAwareƑ<I, O> implements Ƒ1<I,O> {
		public final Class<I> in;
		public final Class<O> out;
		public final Ƒ1<I,O> f;

		public TypeAwareƑ(Ƒ1<I,O> f, Class<I> in, Class<O> out) {
			this.in = in;
			this.out = out;
			this.f = f;
		}

		@Override
		public O apply(I i) {
			return f.apply(i);
		}

	}
}