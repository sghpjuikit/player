/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.future;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.control.ProgressIndicator;
import static util.async.Async.eFX;

/**
 <p>
 @author Plutonium_
 */
public class Fut<T> implements Runnable{
    
    private final CompletableFuture<T> f;
    
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
    
    
    public final <R> Fut<R> then(Function<T,R> action, Consumer<Runnable> executor) {
        return new Fut(f.thenApplyAsync(action, executor::accept));
    }
    public final <R> Fut<R> then(Function<T,R> action) {
        return new Fut(f.thenApplyAsync(action));
    }
    public final <R> Fut<R> supply(Supplier<R> action, Consumer<Runnable> executor) {
        return then(r -> action.get(), executor);
    }
    public final <R> Fut<R> supply(Supplier<R> action) {
        return then(r -> action.get());
    }
    public final <R> Fut<R> supply(R value) {
        return supply(() -> value);
    }
    public final <R> Fut<R> then(CompletableFuture<R> action) {
        return new Fut(f.thenComposeAsync(res -> action));
    }
    public final Fut<Void> use(Consumer<T> action) {
        return new Fut(f.thenAcceptAsync(action));
    }
    public final Fut<Void> use(Consumer<T> action, Consumer<Runnable> executor) {
        return new Fut(f.thenAcceptAsync(action, executor::accept));
    }
    public final Fut<T> thenR(Runnable action) {
        return new Fut(f.thenApplyAsync(r -> { action.run(); return r; }));
    }
    public final Fut<T> thenR(Runnable action, Consumer<Runnable> executor) {
        return new Fut(f.thenApplyAsync(r -> { action.run(); return r; }, executor::accept));
    }
    
    public final Fut<T> showProgress(ProgressIndicator p) {
        return new Fut(CompletableFuture
            .runAsync(()->p.setProgress(-1),eFX)
            .thenComposeAsync(res -> f)
            .thenApplyAsync(t -> {
                p.setProgress(1);
                return t;
            },eFX)
        );
    }
    
    @Override
    public void run() {
       f.thenRunAsync(() ->{}).complete(null);
    }
    
}
