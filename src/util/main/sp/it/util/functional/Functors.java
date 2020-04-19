package sp.it.util.functional;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.util.Callback;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.functions.Function4;
import kotlin.jvm.functions.Function5;
import kotlin.jvm.functions.Function6;
import sp.it.util.dev.SwitchException;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.Util.IS0;
import static sp.it.util.functional.Util.ISNT;
import static sp.it.util.functional.Util.ISNT0;

@SuppressWarnings({"unchecked", "unused"})
public interface Functors {

	FunctorPool pool = new FunctorPool();

	/** Marker interface for lambda. */
	interface L {}

	/** Marker interface for lambda denoting its first input and output. */
	interface IO<I, O> extends L {}

	@FunctionalInterface
	interface F extends L, IO<Void,Void>, Function0<Unit>, Runnable {
		void apply();

		@Override
		default Unit invoke() {
			apply();
			return Unit.INSTANCE;
		}

		/** Equivalent to {@link #apply()}. Exists for compatibility with {@link Runnable}. */
		default void run() {
			apply();
		}

		static F f(Runnable r) {
			return r::run;
		}
	}

	/**
	 * Supplier function
	 *
	 * @param <O> output type
	 */
	@FunctionalInterface
	interface F0<O> extends L, IO<Void,O>, Function0<O>, Supplier<O> {
		O apply();

		@Override
		default O invoke() {
			return apply();
		}

		/** Equivalent to {@link #apply()}. Exists for compatibility with {@link Supplier}. */
		default O get() {
			return apply();
		}

		default <M> F0<M> map(F1<? super O,? extends M> f) {
			return () -> f.apply(apply());
		}

		/**
		 * Returns equivalent function to this returning no output. The computation will still
		 * take place as normal, so this function should have side effects. If it does not, a
		 * function that does nothing should be used instead of this method.
		 */
		default F toF() {
			return this::apply;
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
	interface F1<I, O> extends L, IO<I,O>, Function<I,O>, Function1<I,O>, Callback<I,O>, Consumer<I> {

		static F1<Void,Void> f1(Runnable r) {
			return i -> {
				r.run();
				return null;
			};
		}

		static <T> F1<Void,T> f1(Supplier<T> s) {
			return i -> s.get();
		}

		static <T> F1<T,Void> f1(Consumer<T> c) {
			return i -> {
				c.accept(i);
				return null;
			};
		}

		@Override
		O apply(I queryParam);

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
		default F0<O> toF0(I i) {
			return () -> apply(i);
		}

		/**
		 * Returns function equivalent to this, except for when certain exception types are thrown.
		 * These will be caught and alternative output returned.
		 */
		default F1<I,O> onEx(O or, Class<?>... ecs) {
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
		default F1<I,O> onEx(Supplier<O> or, Class<?>... ecs) {
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
		default F1<I,O> onEx(F1<I,O> or, Class<?>... ecs) {
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
		default <R> F1<I,R> andThen(Function<? super O,? extends R> after) {
			noNull(after);
			return (I t) -> after.apply(apply(t));
		}

		//* Purely to avoid ambiguity of method overloading. Same as andThen(Function). */
		default <R> F1<I,R> andThen(F1<? super O,? extends R> after) {
			noNull(after);
			return (I t) -> after.apply(apply(t));
		}

		/**
		 * Creates function which runs the action afterwards
		 *
		 * @param after action that executes right after computation is done and before returning the output
		 * @return function identical to this one, but one which runs the runnable after it computes
		 */
		default F1<I,O> andThen(Runnable after) {
			noNull(after);
			return i -> {
				O o = apply(i);
				after.run();
				return o;
			};
		}

		// this change return type from Consumer to Function in a type safe way!!
		@Override
		default F1<I,Void> andThen(Consumer<? super I> after) {
			return i -> {
				apply(i);
				after.accept(i);
				return null;
			};
		}

		@Override
		default <R> F1<R,O> compose(Function<? super R,? extends I> before) {
			noNull(before);
			return (R v) -> apply(before.apply(v));
		}

		/**
		 * @param mutator consumer that takes the input of this function and applies it on output of this function after
		 * this function finishes
		 * @return composed function that applies this function to its input and then mutates the output before
		 * returning it.
		 */
		default F1<I,O> andApply(Consumer<O> mutator) {
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
		default F1<I,O> andApply(BiConsumer<I,O> mutator) {
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
		default <O2> F1<I,O2> andThen(F2<I,O,O2> mutator) {
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
		default F1<I,O> nonNull() {
			return in -> {
				O out = apply(in);
				return out==null ? (O) in : out;
			};
		}

		default F1<I,O> nonNull(O or) {
			return andThen(o -> o==null ? or : o);
		}

		default F1<I,O> passNull() {
			return in -> in==null ? null : apply(in);
		}

		@SuppressWarnings("unchecked")
		default F1<I,O> wrap(NullIn i, NullOut o) {
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

		default F1<I,O> onNullIn(OnNullIn ni) {
			if (ni==OnNullIn.NULL)
				return i -> i==null ? null : apply(i);
			if (ni==OnNullIn.APPLY)
				return this;
			if (ni==OnNullIn.VALUE)
				throw new IllegalArgumentException("No value provided");
			throw new SwitchException(ni);
		}

		default F1<I,O> onNullIn(OnNullIn ni, O or) {
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
	 * {@link sp.it.util.functional.Functors.F1} can not extend Predicate, doing so would not be type safe, hence this subclass.
	 * This class also preserves predicate identity during predicate combination operations.
	 */
	@SuppressWarnings("unchecked")
	@FunctionalInterface
	interface FP<I> extends F1<I,Boolean>, Predicate<I> {

		/** Equivalent to {@link #apply(Object)}}. Exists for compatibility with {@link Predicate}. */
		@Override
		default boolean test(I i) {
			return apply(i);
		}

		@Override
		default FP<I> negate() {
			if (this==IS0) return (FP) ISNT0;
			else if (this==ISNT0) return (FP) IS0;
			else if (this==IS) return (FP) ISNT;
			else if (this==ISNT) return (FP) IS;
			return i -> !apply(i);
		}

		@Override
		default FP<I> and(Predicate<? super I> p) {
			if (this==p) return this;
			else if ((this==IS0 && p==ISNT0) || (this==ISNT0 && p==IS0)) return (FP) ISNT;
			else if (p==ISNT || this==ISNT) return (FP) ISNT;
			return i -> apply(i) && p.test(i);
		}

		@Override
		default FP<I> or(Predicate<? super I> p) {
			if (this==p) return this;
			else if ((this==IS0 && p==ISNT0) || (this==ISNT0 && p==IS0)) return (FP) IS;
			else if (this==IS || p==IS) return (FP) IS;
			return i -> apply(i) || p.test(i);
		}

	}

	@FunctionalInterface
	interface F2<I, I2, O> extends L, IO<I,O>, BiFunction<I,I2,O>, Function2<I,I2,O> {
		@Override
		O apply(I i, I2 i2);

		@Override
		default O invoke(I i, I2 i2) {
			return apply(i, i2);
		}

		default F1<? super I, ? extends O> toF1(I2 i2) {
			return (i) -> apply(i, i2);
		}

		default F2<I,I2,O> onEx(O or, Class<?>... ecs) {
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
	interface F3<I, I2, I3, O> extends L, IO<I,O>, Function3<I,I2,I3,O> {
		O apply(I i, I2 i2, I3 i3);

		@Override
		default O invoke(I i, I2 i2, I3 i3) {
			return apply(i, i2, i3);
		}

		default F2<I,I2,O> toF2(I3 i3) {
			return (i, i2) -> apply(i, i2, i3);
		}

		default F3<I,I2,I3,O> onEx(O or, Class<?>... ecs) {
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
	interface F4<I, I2, I3, I4, O> extends L, IO<I,O>, Function4<I,I2,I3,I4,O> {
		O apply(I i, I2 i2, I3 i3, I4 i4);

		@Override
		default O invoke(I i, I2 i2, I3 i3, I4 i4) {
			return apply(i, i2, i3, i4);
		}

		default F3<I,I2,I3,O> toF3(I4 i4) {
			return (i, i2, i3) -> apply(i, i2, i3, i4);
		}

		default F4<I,I2,I3,I4,O> onEx(O or, Class<?>... ecs) {
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
	interface F5<I, I2, I3, I4, I5, O> extends L, IO<I,O>, Function5<I,I2,I3,I4,I5,O> {
		O apply(I i, I2 i2, I3 i3, I4 i4, I5 i5);

		@Override
		default O invoke(I i, I2 i2, I3 i3, I4 i4, I5 i5) {
			return apply(i, i2, i3, i4, i5);
		}

		default F4<I,I2,I3,I4,O> toF4(I5 i5) {
			return (i, i2, i3, i4) -> apply(i, i2, i3, i4, i5);
		}

		default F5<I,I2,I3,I4,I5,O> onEx(O or, Class<?>... ecs) {
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
	interface F6<I, I2, I3, I4, I5, I6, O> extends L, IO<I,O>, Function6<I,I2,I3,I4,I5,I6,O> {
		O apply(I i, I2 i2, I3 i3, I4 i4, I5 i5, I6 i6);

		@Override
		default O invoke(I i, I2 i2, I3 i3, I4 i4, I5 i5, I6 i6) {
			return apply(i, i2, i3, i4, i5, i6);
		}

		default F5<I,I2,I3,I4,I5,O> toF5(I6 i6) {
			return (i, i2, i3, i4, i5) -> apply(i, i2, i3, i4, i5, i6);
		}

		default F6<I,I2,I3,I4,I5,I6,O> onEx(O or, Class<?>... ecs) {
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

}