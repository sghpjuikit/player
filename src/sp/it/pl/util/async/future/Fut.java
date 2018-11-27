package sp.it.pl.util.async.future;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.control.ProgressIndicator;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.async.AsyncKt.eFX;
import static sp.it.pl.util.async.future.FutHelperKt.logging;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.dev.Util.noNull;

/**
 * Future monad implementation.
 * <p/>
 * Oriented for practicality, not specification (monadic laws) or robustness (API completeness).
 * This is still work in progress.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Fut<T> {

	private CompletableFuture<T> f;

	public Fut(CompletableFuture<T> future) {
		f = future;
	}

	public Fut() {
		f = CompletableFuture.completedFuture(null);
	}

	public Fut(T input) {
		f = CompletableFuture.completedFuture(input);
	}

	public static Fut<Void> fut() {
		return new Fut<>();
	}

	public static <T> Fut<T> fut(T block) {
		return new Fut<>(block);
	}

	public static <T> Fut<T> futWith(Supplier<T> block) {
		noNull(block);
		return new Fut<>(CompletableFuture.supplyAsync(logging(block)));
	}

	public boolean isDone() {
		return f.isDone();
	}

	public T getDone() {
		try {
			return f.get();
		} catch (InterruptedException e) {
			logger(Fut.class).error("Asynchronous computation was interrupted", e);
			return null;
		} catch (ExecutionException e) {
			logger(Fut.class).error("Asynchronous computation encountered a problem", e);
			return null;
		}
	}

	public T getDoneOrNull() {
		if (f.isDone()) {
			try {
				return f.get();
			} catch (InterruptedException e) {
				logger(Fut.class).error("Asynchronous computation was interrupted", e);
				return null;
			} catch (ExecutionException e) {
				logger(Fut.class).error("Asynchronous computation encountered a problem", e);
				return null;
			}
		} else {
			return null;
		}
	}

	public void cancel(boolean mayInterruptIfRunning) {
		f.cancel(mayInterruptIfRunning);
	}

	public <R> Fut<R> map(Function<? super T,? extends R> block) {
		return new Fut<>(f.thenApplyAsync(logging(block)));
	}

	public <R> Fut<R> map(Executor executor, Function<? super T, ? extends R> block) {
		return new Fut<>(f.thenApplyAsync(logging(block), executor));
	}

	public <R> Fut<R> map(Consumer<Runnable> executor, Function<? super T,? extends R> block) {
		return new Fut<>(f.thenApplyAsync(logging(block), executor::accept));
	}

	public <R> Fut<R> supply(R value) {
		return supply(() -> value);
	}

	public <R> Fut<R> supply(Supplier<? extends R> block) {
		return map(r -> block.get());
	}

	public <R> Fut<R> supply(Fut<R> block) {
		return new Fut<>(CompletableFuture.<Void>completedFuture(null)
				.thenCompose(res -> f)
				.thenCompose(res -> block.f));
	}

	public <R> Fut<R> supply(Executor executor, Supplier<? extends R> block) {
		return map(executor, r -> block.get());
	}

	public <R> Fut<R> supply(Consumer<Runnable> executor, Supplier<? extends R> block) {
		return map(executor, r -> block.get());
	}

	public Fut<T> use(Consumer<? super T> block) {
		return new Fut<>(f.thenApplyAsync(r -> { logging(block).accept(r); return r; }));
	}

	public Fut<T> use(Executor executor, Consumer<? super T> block) {
		return new Fut<>(f.thenApplyAsync(r -> { logging(block).accept(r); return r; }, executor));
	}

	public Fut<T> use(Consumer<? super Runnable> executor, Consumer<? super T> block) {
		return new Fut<>(f.thenApplyAsync(r -> { logging(block).accept(r); return r; }, executor::accept));
	}

	public Fut<T> then(Runnable block) {
		return new Fut<>(f.thenApplyAsync(r -> { logging(block).run(); return r; }));
	}

	public Fut<T> then(Executor executor, Runnable block) {
		return new Fut<>(f.thenApplyAsync(r -> { logging(block).run(); return r; }, executor));
	}

	public Fut<T> then(Consumer<? super Runnable> executor, Runnable block) {
		return new Fut<>(f.thenApplyAsync(r -> { logging(block).run(); return r; }, executor::accept));
	}

	// unsafe exception handling, disable for now
//	public <R> Fut<R> then(CompletableFuture<R> block) {
//		return new Fut<>(f.thenComposeAsync(res -> block));
//	}

	/**
	 * Returns new future, which sets progress to 0 on fx thread, then executes
	 * this future and then sets progress to 1, again on fx thread.
	 * <p/>
	 * Note that when chaining futures, the position within chain decides when
	 * does the progress reach 1. It will not be at the end of the chain, but
	 * at the position of this method in it. The progress is set to 0 always at
	 * the beginning of the computation, i.e. the chain length or position of
	 * this method within it does not have effect.
	 * <p/>
	 * To set the progress to 1 at the end of computation, this method must be
	 * the last element of the chain.
	 * To set the progress to 0 somewhere during the computation, a future for
	 * the progress computation must created, this method called on it and
	 * passed as Runnable into another future which executes it as
	 * part of its computation. This will cause only that computation to be bound to
	 * the progress.
	 */
	public Fut<T> showProgress(Optional<ProgressIndicator> p) {
		return p.map(this::showProgress).orElse(this);
	}

	/**
	 * Invokes {@link #showProgress(java.util.Optional)} using new progress indicator in the currently active window, or
	 * empty optional if no window is empty.
	 */
	public Fut<T> showProgressOnActiveWindow() {
		return showProgress(APP.windowManager.getActive().map(Window::taskAdd));
	}

	/**
	 * @param p nonnull progress indicator
	 * @throws java.lang.RuntimeException if any param null
	 */
	public Fut<T> showProgress(ProgressIndicator p) {
		noNull(p);
		return new Fut<>(CompletableFuture
				.runAsync(() -> p.setProgress(-1), eFX)
				.thenComposeAsync(res -> f)
				.thenApplyAsync(t -> {
					p.setProgress(1);
					return t;
				}, eFX)
		);
	}

	/**
	 * @param condition test that if false, progress will not be displayed
	 * @param sp function that supplies nonnull progress indicator
	 * @return this (fluent style)
	 */
	public Fut<T> showProgress(boolean condition, Supplier<? extends ProgressIndicator> sp) {
		return condition ? showProgress(sp.get()) : this;
	}

	public <R> Fut<R> thenChain(Ƒ1<? super Fut<T>, ? extends Fut<R>> then) {
		return then.apply(this);
	}
	
}