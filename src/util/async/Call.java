/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async;

import javafx.util.Callback;

/**
 <p>
 @author Plutonium_
 */
public interface Call<I,R> extends Callback<I, R> {
    
//    public default Call<I,R> on(Consumer<Call<I,R>> executor) {
//        return i -> {
//            executor.accept(this);
//        };
//    }
//    
//    public default <RR> Call<I,RR> then(Callback<R,RR> after) {
//        return i -> {
//            R r = call(i);
//            return after.call(r);
//        };
//    }
//    
//    public default Run thenOn(Consumer<Runnable> executor, Run after) {
//        return then(after.on(executor));
//    }
//    
//    
//    
//    public static <P,R> Call c(R r) {
//        return nothing -> r;
//    }
}
