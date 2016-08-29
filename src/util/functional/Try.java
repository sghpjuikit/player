package util.functional;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @param <R> success return value
 * @param <E> error return value
 *
 * @author Martin Polakovic
 */
public interface Try<R,E> {

	static <R,E> Try<R,E> ok(R val) {
		return new Ok<>(val);
	}

	static <R,E> Try<R,E> error(E val) {
		return new Error<>(val);
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

	default <T> Try<T,E> map(Function<? super R, ? extends T> mapper) {
		return isOk() ? ok(mapper.apply(get())) : error(getError());
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
