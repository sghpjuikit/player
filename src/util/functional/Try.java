package util.functional;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import util.functional.Functors.Ƒ1;

/**
 *
 * @param <R> success return value
 * @param <E> error return value
 *
 * @author Martin Polakovic
 */
public interface Try<R,E> {

	static <E> Try<Void,E> ok() {
		return new Ok<>(null);
	}

	static <R,E> Try<R,E> ok(R val) {
		return new Ok<>(val);
	}

	static <R> Try<R,Void> error() {
		return new Error<>(null);
	}

	static <R,E> Try<R,E> error(E val) {
		return new Error<>(val);
	}

	static <R> Try<R,String> errorOf(Throwable e) {
		String message = e.getMessage();
		return error(message == null ? "Unknown error" : message);
	}

	static <I,O> Ƒ1<I,Try<O,String>> tryF(Function<I,O> f, Iterable<Class<?>> ecs) {
		return i -> {
			try {
				return ok(f.apply(i));
			} catch(Exception e) {
				for (Class<?> ec : ecs)
					if (ec.isInstance(e))
						return errorOf(e);
				throw new RuntimeException("Unhandled exception thrown in Try operation", e);
			}
		};
	}

	static <I,O> Ƒ1<I,Try<O,String>> tryF(Function<I,O> f, Class<?>... ecs) {
		return i -> {
			try {
				return ok(f.apply(i));
			} catch(Exception e) {
				for (Class<?> ec : ecs)
					if (ec.isInstance(e))
						return errorOf(e);
				throw new RuntimeException("Unhandled exception thrown in Try operation", e);
			}
		};
	}


	boolean isOk();

	boolean isError();

	R get();

	default R getOr(R val) {
		return isOk() ? get() : val;
	}

	default R getOrSupply(Supplier<R> val) {
		return isOk() ? get() : val.get();
	}

	E getError();

	Try<R,E> ifOk(Consumer<? super R> action);

	Try<R,E> ifError(Consumer<? super E> action);

	default Try<R,E> ifAny(Consumer<? super R> actionOk, Consumer<? super E> actionError) {
		if (isOk()) actionOk.accept(get());
		else actionError.accept(getError());
		return this;
	}

	default <S> Try<S,E> map(Function<? super R, ? extends S> mapper) {
		return isOk() ? ok(mapper.apply(get())) : error(getError());
	}

	default <F> Try<R,F> mapError(Function<? super E, ? extends F> mapper) {
		return isError() ? error(mapper.apply(getError())) : ok(get());
	}

	default <S,F> Try<S,F> map(Function<? super R, ? extends S> mapperOk, Function<? super E, ? extends F> mapperError) {
		return isOk() ? ok(mapperOk.apply(get())) : error(mapperError.apply(getError()));
	}

	default Try<R,E> and(Try<? super R,? extends E> constraint) {
		if (isError())
			return this;
		else {
			return constraint.isOk() ? this : error(constraint.getError());
		}
	}

	default Try<R,E> and(Predicate<? super R> constraint, Function<? super R, ? extends E> errorSupplier) {
		if (isError())
			return this;
		else {
			return constraint.test(get()) ? this : error(errorSupplier.apply(get()));
		}
	}

	default Try<R,E> and(Function<? super R, Try<Void,E>> constraint) {
		if (isError())
			return this;
		else {
			Try<Void,E> c = constraint.apply(get());
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

	class Ok<R,E> implements Try<R,E> {

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
	class Error<R,E> implements Try<R,E> {

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