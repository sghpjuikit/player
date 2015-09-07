/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.future;

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

import static util.async.Async.eFX;

/**
 <p>
 @author Plutonium_
 */
public class Fut<T> implements Runnable{

    private CompletableFuture<T> f;

    private Fut(CompletableFuture<T> future) {
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

    public static <T> Fut<T> fut(Supplier<T> t) {
//        return new Fut<>().supply(t);
        return new Fut<>(CompletableFuture.supplyAsync(t));
    }

    public static <T> Fut<T> futAfter(Fut<T> f) {
        CompletableFuture<T> nf = f.f.handle((result,exception) -> {
            if(exception!=null) throw new RuntimeException("Fut errored out",exception);
            else return result;
        });
        return new Fut<>(nf);
    }

    public boolean isDone() {
        return f.isDone();
    }

    public T getDone() {
        if(f.isDone()) {
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

    public final <R> Fut<R> map(Function<T,R> action) {
        return new Fut<>(f.thenApplyAsync(action));
    }
    public final <R> Fut<R> map(Function<T,R> action, Executor executor) {
        return new Fut<>(f.thenApplyAsync(action, executor));
    }
    public final <R> Fut<R> map(Function<T,R> action, Consumer<Runnable> executor) {
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
    public final <R> Fut<R> supply(Supplier<R> action, Executor executor) {
        return Fut.this.map(r -> action.get(), executor);
    }
    public final <R> Fut<R> supply(Supplier<R> action, Consumer<Runnable> executor) {
        return map(r -> action.get(), executor);
    }

    public final <R> Fut<R> then(CompletableFuture<R> action) {
        return new Fut<>(f.thenComposeAsync(res -> action));
    }

    public final Fut<Void> use(Consumer<T> action) {
        return new Fut<>(f.thenAcceptAsync(action));
    }
    public final Fut<Void> use(Consumer<T> action, Executor executor) {
        return new Fut<>(f.thenAcceptAsync(action, executor));
    }
    public final Fut<Void> use(Consumer<T> action, Consumer<Runnable> executor) {
        return new Fut<>(f.thenAcceptAsync(action, executor::accept));
    }

    public final Fut<T> then(Runnable action) {
        return new Fut<>(f.thenApplyAsync(r -> { action.run(); return r; }));
    }
    public final Fut<T> then(Runnable action, Executor executor) {
        return new Fut<>(f.thenApplyAsync(r -> { action.run(); return r; }, executor));
    }
    public final Fut<T> then(Runnable action, Consumer<Runnable> executor) {
        return new Fut<>(f.thenApplyAsync(r -> { action.run(); return r; }, executor::accept));
    }

    /**
     * Returns new future, which sets progress to 0 on fx thread, then executes
     * this future and then sets progress to 1, again on fx thread.
     * <p>
     * Note that when chaining futures, the position within chain decides when
     * does the progress reach 1. It will not be at the end of the chain, but
     * at the position of this method in it. The progress is set to 0 always at
     * the beginning of the computation, i.e. the chain length or position of
     * this method within it does not have effect.
     * <p>
     * To set the progress to 1 at the end of computation, this method must be
     * the last element of the chain.
     * To set the progress to 0 somewhere during the computation, a future for
     * the progress computation must created, this method called on it and
     * passed as Runnable into another future which executes it as
     * part of its computation. This will cause only that computation to be bound to
     * the progress.
     */
    public final Fut<T> showProgress(ProgressIndicator p) {
        return new Fut<>(CompletableFuture
            .runAsync(()->p.setProgress(-1),eFX)
            .thenComposeAsync(res -> f)
            .thenApplyAsync(t -> {
                p.setProgress(1);
                return t;
            },eFX)
        );
    }
    public final Fut<T> showProgress(boolean condition, Supplier<ProgressIndicator> sp) {
        if(condition) {
            ProgressIndicator p = sp.get();
            return showProgress(p);
        } else
            return this;
    }

    public <R> Fut<R> then(Ƒ1<Fut<T>,Fut<R>> then) {
        return then.apply(this);
    }

    @Override
    public void run() {
       f.thenRunAsync(() -> {}).complete(null);
    }

}
