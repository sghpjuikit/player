package util.functional;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

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
	E getError();
	void ifOk(Consumer<? super R> action);
	void ifError(Consumer<? super E> action);

	private class Ok<R,E> implements Try<R,E> {

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
		public void ifOk(Consumer<? super R> action) {
			action.accept(val);
		}

		@Override
		public void ifError(Consumer<? super E> action) {

		}
	}
	private class Error<R,E> implements Try<R,E> {

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
		public void ifOk(Consumer<? super R> action) {

		}

		@Override
		public void ifError(Consumer<? super E> action) {
			action.accept(val);
		}
	}
}
