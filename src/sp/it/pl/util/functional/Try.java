package sp.it.pl.util.functional;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.util.functional.Functors.Ƒ0E;
import sp.it.pl.util.functional.Functors.Ƒ1;

/**
 * Try monad for functional error handling.
 *
 * @param <R> success return value
 * @param <E> error return value
 */
@SuppressWarnings("deprecation")
public interface Try<R, E> {

	@NotNull
	static <E> Try<Void,E> ok() {
		return new Ok<>(null);
	}

	@NotNull
	static <R, E> Try<R,E> ok(R val) {
		return new Ok<>(val);
	}

	@NotNull
	static <R> Try<R,Void> error() {
		return new Error<>(null);
	}

	@NotNull
	static <R, E> Try<R,E> error(E val) {
		return new Error<>(val);
	}

	@NotNull
	static <R> Try<R,String> errorOf(Throwable e) {
		String message = e.getMessage();
		return error(message==null ? "Unknown error" : message);
	}

	static Try<Void,Throwable> tryR(Runnable f, Iterable<Class<?>> ecs) {
		try {
			f.run();
			return ok(null);
		} catch (Exception e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <T> Try<T,Throwable> tryCatchAll(Supplier<? extends T> f) {
		try {
			return ok(f.get());
		} catch (Throwable e) {
			return error(e);
		}
	}

	static Try<Void,Throwable> tryR(Runnable f, Class<?>... ecs) {
		try {
			f.run();
			return ok(null);
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O> Try<O,Throwable> tryS(Supplier<? extends O> f, Iterable<Class<?>> ecs) {
		try {
			return ok(f.get());
		} catch (Exception e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O> Try<O,Throwable> tryS(Supplier<? extends O> f, Class<?>... ecs) {
		try {
			return ok(f.get());
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O, E extends Throwable> Try<O,Throwable> trySE(Ƒ0E<? extends O,E> f, Iterable<Class<?>> ecs) {
		try {
			return ok(f.apply());
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O, E extends Throwable> Try<O,Throwable> trySE(Ƒ0E<? extends O,E> f, Class<?>... ecs) {
		try {
			return ok(f.apply());
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <I, O> Ƒ1<I,Try<O,String>> tryF(Function<I,O> f, Iterable<Class<?>> ecs) {
		return i -> {
			try {
				return ok(f.apply(i));
			} catch (Throwable e) {
				for (Class<?> ec : ecs)
					if (ec.isInstance(e))
						return errorOf(e);
				throw new RuntimeException("Unhandled exception thrown in Try operation", e);
			}
		};
	}

	static <I, O> Ƒ1<I,Try<O,String>> tryF(Function<I,O> f, Class<?>... ecs) {
		return i -> {
			try {
				return ok(f.apply(i));
			} catch (Throwable e) {
				for (Class<?> ec : ecs)
					if (ec.isInstance(e))
						return errorOf(e);
				throw new RuntimeException("Unhandled exception thrown in Try operation", e);
			}
		};
	}

	static <I, O, E extends Throwable> O orThrow(Ƒ0E<O,E> f) {
		try {
			return f.apply();
		} catch (Throwable e) {
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <I, O, E extends Throwable> Try<O,Throwable> wrapE(Ƒ0E<O,E> f) {
		try {
			return ok(f.apply());
		} catch (Throwable e) {
			return error(e);
		}
	}

	/** @return true iff this is a success */
	boolean isOk();

	/** @return true iff this is an error */
	boolean isError();

	/** @return the value if ok or throw an exception if error */
	@Deprecated
	R get();

	/** @return the value if ok or the specified value if error */
	default R getOr(R val) {
		return isOk() ? get() : val;
	}

	/** @return the value if ok or the value computed with specified supplier if error */
	default R getOrSupply(Supplier<R> val) {
		return isOk() ? get() : val.get();
	}

	/** @return the value if ok or the value computed with specified supplier if error */
	default R getOrSupply(Function<? super E,? extends R> recoverValueSupplier) {
		if (isOk()) {
			return get();
		} else {
			return recoverValueSupplier.apply(getError());
		}
	}

	/** @return the value if ok or throw an exception if error */
	default R getOrThrow() {
		if (isOk()) return get();
		throw new AssertionError("Can not get result of an Error Try");
	}

	/** @return the error if error or throw an exception if success */
	@Deprecated
	E getError();

	/** @return the success value if success or the error value if error */
	@SuppressWarnings("unchecked")
	default R getAny() {
		if (isOk()) return get();
		else return (R) getError();
	}

	/**
	 * Invoke the specified action if success
	 *
	 * @return this
	 */
	Try<R,E> ifOk(Consumer<? super R> action);

	/**
	 * Invoke the specified action if error
	 *
	 * @return this
	 */
	Try<R,E> ifError(Consumer<? super E> action);

	/**
	 * Invoke the specified action if success or the other specified action if error
	 *
	 * @return this
	 */
	default Try<R,E> ifAny(Consumer<? super R> actionOk, Consumer<? super E> actionError) {
		if (isOk()) actionOk.accept(get());
		else actionError.accept(getError());
		return this;
	}

	default <S> Try<S,E> map(Function<? super R,? extends S> mapper) {
		return isOk() ? ok(mapper.apply(get())) : error(getError());
	}

	default <F> Try<R,F> mapError(Function<? super E,? extends F> mapper) {
		return isError() ? error(mapper.apply(getError())) : ok(get());
	}

	default <S, F> Try<S,F> map(Function<? super R,? extends S> mapperOk, Function<? super E,? extends F> mapperError) {
		return isOk() ? ok(mapperOk.apply(get())) : error(mapperError.apply(getError()));
	}

	default Try<R,E> and(Try<? super R,? extends E> constraint) {
		if (isError())
			return this;
		else {
			return constraint.isOk() ? this : error(constraint.getError());
		}
	}

	default Try<R,E> and(Predicate<? super R> constraint, Function<? super R,? extends E> errorSupplier) {
		if (isError())
			return this;
		else {
			return constraint.test(get()) ? this : error(errorSupplier.apply(get()));
		}
	}

	default Try<R,E> and(Function<? super R,Try<?,E>> constraint) {
		if (isError())
			return this;
		else {
			Try<?,E> c = constraint.apply(get());
			return c.isOk() ? this : error(c.getError());
		}
	}

	default Try<R,E> or(Try<R,E> constraint) {
		if (isOk())
			return this;
		else {
			return constraint.isOk() ? ok(constraint.get()) : this;
		}
	}

	class Ok<R, E> implements Try<R,E> {

		private final R val;

		public Ok(R val) {
			this.val = val;
		}

		@Override
		public boolean isOk() {
			return true;
		}

		@Override
		public boolean isError() {
			return false;
		}

		@Override
		public R get() {
			return val;
		}

		@Override
		public E getError() {
			throw new NoSuchElementException();
		}

		@Override
		public Try<R,E> ifOk(Consumer<? super R> action) {
			action.accept(val);
			return this;
		}

		@Override
		public Try<R,E> ifError(Consumer<? super E> action) {
			return this;
		}
	}

	class Error<R, E> implements Try<R,E> {

		private final E val;

		public Error(E val) {
			this.val = val;
		}

		@Override
		public boolean isOk() {
			return false;
		}

		@Override
		public boolean isError() {
			return true;
		}

		@Override
		public R get() {
			throw new NoSuchElementException();
		}

		@Override
		public E getError() {
			return val;
		}

		@Override
		public Try<R,E> ifOk(Consumer<? super R> action) {
			return this;
		}

		@Override
		public Try<R,E> ifError(Consumer<? super E> action) {
			action.accept(val);
			return this;
		}
	}
}