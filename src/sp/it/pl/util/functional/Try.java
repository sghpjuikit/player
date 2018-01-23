package sp.it.pl.util.functional;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import sp.it.pl.util.UtilKt;
import sp.it.pl.util.functional.Functors.Ƒ0E;
import sp.it.pl.util.functional.Functors.Ƒ1;

/**
 * Try monad for functional error handling.
 *
 * @param <R> success return value
 */
@SuppressWarnings("deprecation")
public interface Try<R> {

	Try<Object> emptyError = new Error<>(null, null);

	static Try<Void> ok() {
		return new Ok<>(null);
	}

	static <R> Try<R> ok(R value) {
		return new Ok<>(value);
	}

	/** constructs an error without any content - useful if it's just used to indicate a non-exceptional fail */
	@SuppressWarnings("unchecked")
	static <R> Try<R> errorFast() {
		return (Try<R>) emptyError;
	}

	/** constructs an error with a new Exception */
	static <R> Try<R> error() {
		return new Error<>(new Exception(), null);
	}

	static <R> Try<R> error(String message) {
		return new Error<>(new Exception(message), message);
	}

	static <R> Try<R> error(Throwable ex) {
		return new Error<>(ex, ex.getMessage());
	}

	static <R> Try<R> error(Throwable ex, String message) {
		return new Error<>(ex, message);
	}

	/** if {@link #isOk}: logs {@link #get} as info <br>
	 *  if {@link #isError}: logs the stacktrace of {@link #getException} to debug and the {@link #getError} to Error
	 *  @param clazz the origin for the Logger */
	default Try<R> log(Object clazz) {
		return log(sp.it.pl.util.dev.Util.logger(clazz));
	}

	/** if {@link #isOk}: logs {@link #get} as info <br>
	 *  if {@link #isError}: logs the stacktrace of {@link #getException} to debug and the {@link #getError} to Error */
	default Try<R> log(Logger logger) {
		return log(logger, false);
	}

	/** if {@link #isOk}: logs {@link #get} as info <br>
	 *  if {@link #isError}: logs the stacktrace of {@link #getException} to debug and the {@link #getError} to Warn or Error
	 *  @param onlyWarn if true, {@link #getError()} will be logged to warn, else to error */
	default Try<R> log(Logger logger, boolean onlyWarn) {
		if(isOk())
			logger.info(get().toString());
		else {
			if(onlyWarn)
				logger.warn(getError());
			else
				logger.error(getError());
			if(logger.isTraceEnabled())
				logger.debug(UtilKt.getStacktraceAsString(getException()));
		}
		return this;
	}

	// region tries

	static Try<Void> tryR(Runnable f, Iterable<Class<?>> ecs) {
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

	static Try<Void> tryCatchAll(Runnable f) {
		try {
			f.run();
			return ok(null);
		} catch (Throwable e) {
			return error(e);
		}
	}

	static Try<Void> tryR(Runnable f, Class<?>... ecs) {
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

	static <O> Try<O> tryS(Supplier<? extends O> f, Iterable<Class<?>> ecs) {
		try {
			return ok(f.get());
		} catch (Exception e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O> Try<O> tryS(Supplier<? extends O> f, Class<?>... ecs) {
		try {
			return ok(f.get());
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O, E extends Throwable> Try<O> trySE(Ƒ0E<? extends O,E> f, Iterable<Class<?>> ecs) {
		try {
			return ok(f.apply());
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O, E extends Throwable> Try<O> trySE(Ƒ0E<? extends O,E> f, Class<?>... ecs) {
		try {
			return ok(f.apply());
		} catch (Throwable e) {
			for (Class<?> ec : ecs)
				if (ec.isInstance(e))
					return error(e);
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <I, O> Ƒ1<I,Try<O>> tryF(Function<I,O> f, Iterable<Class<?>> ecs) {
		return i -> {
			try {
				return ok(f.apply(i));
			} catch (Throwable e) {
				for (Class<?> ec : ecs)
					if (ec.isInstance(e))
						return error(e);
				throw new RuntimeException("Unhandled exception thrown in Try operation", e);
			}
		};
	}

	static <I, O> Ƒ1<I,Try<O>> tryF(Function<I,O> f, Class<?>... ecs) {
		return i -> {
			try {
				return ok(f.apply(i));
			} catch (Throwable e) {
				for (Class<?> ec : ecs)
					if (ec.isInstance(e))
						return error(e);
				throw new RuntimeException("Unhandled exception thrown in Try operation", e);
			}
		};
	}

	static <O, E extends Throwable> O orThrow(Ƒ0E<O,E> f) {
		try {
			return f.apply();
		} catch (Throwable e) {
			throw new RuntimeException("Unhandled exception thrown in Try operation", e);
		}
	}

	static <O, E extends Throwable> Try<O> wrapE(Ƒ0E<O,E> f) {
		try {
			return ok(f.apply());
		} catch (Throwable e) {
			return error(e);
		}
	}

	//endregion

	/** @return true if this is a success */
	boolean isOk();

	/** @return true if this is an error */
	default boolean isError() {
		return !isOk();
	}

	/** @return the value if {@link #isOk}
	 * @throws AssertionError if {@link #isError} */
	default R get() {
		throw new AssertionError("Can not get result of an Error Try");
	}

	/** @return the error message if {@link #isError}
	 * @throws NoSuchElementException if {@link #isOk} */
	String getError();

	/** @return the exception if {@link #isError}
	 * @throws NoSuchElementException if {@link #isOk} */
	Throwable getException();

	/** @return the value if {@link #isOk} or the specified value if {@link #isError} */
	default R getOr(R alternative) {
		return isOk() ? get() : alternative;
	}

	/** @return the value if {@link #isOk} or the value computed with specified supplier if {@link #isError} */
	default R getOrSupply(Supplier<R> supplier) {
		return isOk() ? get() : supplier.get();
	}

	/** @return the value if {@link #isOk} or the value computed with specified supplier if {@link #isError} */
	default R getOrSupply(Function<? super Object,? extends R> recoverValueSupplier) {
		if (isOk()) {
			return get();
		} else {
			return recoverValueSupplier.apply(getError());
		}
	}


	/** @return the success value if {@link #isOk} or the error value if error */
	@SuppressWarnings("unchecked")
	default R getAny() {
		if (isOk()) return get();
		else return (R) getError();
	}

	/**
	 * Invoke the specified action if {@link #isOk}
	 *
	 * @return this
	 */
	default Try<R> handleOk(Consumer<? super R> action) {
		if(isOk())
			action.accept(get());
		return this;
	}

	/**
	 * Invoke the specified action if {@link #isError}
	 *
	 * @return this
	 */
	default Try<R> handleError(Consumer<? super String> action) {
		if(isError())
			action.accept(getError());
		return this;
	}

	/**
	 * Invoke the specified action if {@link #isError}
	 *
	 * @return this
	 */
	default Try<R> handleException(Consumer<? super Throwable> action) {
		if(isError())
			action.accept(getException());
		return this;
	}

	/**
	 * Invoke the specified action if {@link #isOk} or the other specified action if {@link #isError}
	 *
	 * @return this
	 */
	default Try<R> ifAny(Consumer<? super R> actionOk, Consumer<? super Object> actionError) {
		if (isOk()) actionOk.accept(get());
		else actionError.accept(getError());
		return this;
	}

	default <S> Try<S> map(Function<? super R,? extends S> mapper) {
		return isOk() ? ok(mapper.apply(get())) : error(getException(), getError());
	}

	default Try<R> mapError(Function<? super String,? extends String> mapper) {
		return isError() ? error(mapper.apply(getError())) : ok(get());
	}

	default Try<R> and(Try<? super R> constraint) {
		return isError() || constraint.isOk() ? this : error(constraint.getError());
	}

	default Try<R> and(Predicate<? super R> constraint, Function<? super R,? extends Throwable> errorSupplier) {
		return isError() || constraint.test(get()) ? this : error(errorSupplier.apply(get()));
	}

	default Try<R> and(Function<? super R,Try<Void>> constraint) {
		if (isError())
			return this;
		else {
			Try<Void> c = constraint.apply(get());
			return c.isOk() ? this : error(c.getError());
		}
	}

	default Try<R> or(Try<R> constraint) {
		return isOk() || !constraint.isOk() ? this : ok(constraint.get());
	}

	class Ok<R> implements Try<R> {

		private final R value;

		public Ok(R value) {
			this.value = value;
		}

		@Override
		public boolean isOk() {
			return true;
		}

		@Override
		public R get() {
			return value;
		}

		@Override
		public String getError() {
			throw new NoSuchElementException();
		}

		@Override
		public Throwable getException() {
			throw new NoSuchElementException();
		}

	}

	class Error<R> implements Try<R> {

		private final String message;
		private final Throwable exception;

		Error(Throwable ex, String message) {
			this.message = message;
			exception = ex;
		}

		@Override
		public boolean isOk() {
			return false;
		}

		@Override
		public R get() {
			throw new NoSuchElementException();
		}

		@Override
		public String getError() {
			return message;
		}

		@Override
		public Throwable getException() {
			return exception;
		}

	}

}