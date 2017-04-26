package util.async.future;

import gui.objects.window.stage.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.ProgressIndicator;
import util.functional.Functors.Ƒ1;
import static main.App.APP;
import static util.async.Async.eFX;
import static util.dev.Util.noØ;

/**
 * <p/>
 *
 * @author Martin Polakovic
 */
public class Fut<T> implements Runnable {

	public CompletableFuture<T> f;

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

	public static <T> Fut<T> fut(T t) {
		return new Fut<>(t);
	}

	public static <T> Fut<T> futWith(Supplier<T> t) {
		noØ(t);
		// TODO: investigate and implement ror remove
//        return new Fut<>().supply(t);
		return new Fut<>(CompletableFuture.supplyAsync(t));
	}

	public static <T> Fut<T> futAfter(Fut<T> f) {
		CompletableFuture<T> nf = f.f.handle((result, exception) -> {
			if (exception!=null) throw new RuntimeException("Fut encountered an error ", exception);
			else return result;
		});
		return new Fut<>(nf);
	}

	public static <T> Fut<T> after(Fut<T> f) {
		CompletableFuture<T> nf = f.f.handle((result, exception) -> {
			if (exception!=null) throw new RuntimeException("Fut encountered an error ", exception);
			else return result;
		});
		return new Fut<>(nf);
	}

	public boolean isDone() {
		return f.isDone();
	}

	@SuppressWarnings("TryWithIdenticalCatches")
	public T getDone() {
		if (f.isDone()) {
			try {
				return f.get();
			} catch (InterruptedException ex) {
				Logger.getLogger(Fut.class.getName()).log(Level.SEVERE, null, ex);
				return null;
			} catch (ExecutionException ex) {
				Logger.getLogger(Fut.class.getName()).log(Level.SEVERE, null, ex);
				return null;
			}
		} else {
			return null;
		}
	}

	public void cancel(boolean mayInterruptIfRunning) {
		f.cancel(mayInterruptIfRunning);
	}

	public final <R> Fut<R> map(Function<? super T,R> action) {
		return new Fut<>(f.thenApplyAsync(action));
	}

	public final <R> Fut<R> map(Executor executor, Function<? super T,R> action) {
		return new Fut<>(f.thenApplyAsync(action, executor));
	}

	public final <R> Fut<R> map(Consumer<Runnable> executor, Function<? super T,R> action) {
		return new Fut<>(f.thenApplyAsync(action, executor::accept));
	}

	public final <R> Fut<R> supply(R value) {
		return supply(() -> value);
	}

	public final <R> Fut<R> supply(Supplier<R> action) {
		return Fut.this.map(r -> action.get());
	}

	public final <R> Fut<R> supply(Fut<R> action) {
		return new Fut<>(CompletableFuture.<Void>completedFuture(null)
				.thenCompose(res -> f)
				.thenCompose(res -> action.f));
	}

	public final <R> Fut<R> supply(Executor executor, Supplier<R> action) {
		return Fut.this.map(executor, r -> action.get());
	}

	public final <R> Fut<R> supply(Consumer<Runnable> executor, Supplier<R> action) {
		return map(executor, r -> action.get());
	}

	public final Fut<T> use(Consumer<T> action) {
//        f = f.thenApplyAsync(r -> {action.accept(r); return r; });
//        return this;
		return new Fut<>(f.thenApplyAsync(r -> {action.accept(r); return r; }));
	}

	public final Fut<T> use(Executor executor, Consumer<T> action) {
		return new Fut<>(f.thenApplyAsync(r -> {action.accept(r); return r; }, executor));
	}

	public final Fut<T> use(Consumer<Runnable> executor, Consumer<T> action) {
		return new Fut<>(f.thenApplyAsync(r -> {action.accept(r); return r; }, executor::accept));
	}

	public final Fut<T> then(Runnable action) {
		return new Fut<>(f.thenApplyAsync(r -> { action.run(); return r; }));
	}

	public final Fut<T> then(Executor executor, Runnable action) {
		return new Fut<>(f.thenApplyAsync(r -> { action.run(); return r; }, executor));
	}

	public final Fut<T> then(Consumer<Runnable> executor, Runnable action) {
		return new Fut<>(f.thenApplyAsync(r -> { action.run(); return r; }, executor::accept));
	}

	public final <R> Fut<R> then(CompletableFuture<R> action) {
		return new Fut<>(f.thenComposeAsync(res -> action));
	}

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
	public final Fut<T> showProgress(Optional<ProgressIndicator> p) {
		return p.map(this::showProgress).orElse(this);
	}

	/**
	 * Invokes {@link #showProgress(java.util.Optional)} using new progress indicator in the currently active window, or
	 * empty optional if no window is empty.
	 */
	public final Fut<T> showProgressOnActiveWindow() {
		return showProgress(APP.windowManager.getActive().map(Window::taskAdd));
	}

	/**
	 *
	 * @param p nonnull progress indicator
	 * @throws java.lang.RuntimeException if any param null
	 */
	public final Fut<T> showProgress(ProgressIndicator p) {
		noØ(p);
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
	 *
	 * @param condition
	 * @param sp function that supplies nonnull progress indicator
	 * @return this (fluent style)
	 */
	public final Fut<T> showProgress(boolean condition, Supplier<ProgressIndicator> sp) {
		if (condition) {
			return showProgress(sp.get());
		} else
			return this;
	}

	public <R> Fut<R> thenChain(Ƒ1<Fut<T>,Fut<R>> then) {
		return then.apply(this);
	}

	// TODO: remove
	@Deprecated
	@Override
	public void run() {
		f.thenRunAsync(() -> {}).complete(null);
	}

}