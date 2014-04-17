/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.functional.functor;

/**
 *
 * @author uranium
 * @param <T> type of result of the task calling this object upon its completion.
 */
@FunctionalInterface
public interface OnEnd<T> {
    
    /** Runs when running task finishes successfully
     * @param result finished result of the task calling this object. Type of
     * result is parametrized as generics.
     */
    void success(T result);
    
    /** Runs when running task finishes unsuccessfully. Default implementation
     * does nothing. This is to allow use of lambdas (by turning this interface 
     * into Functional one) on this object when failure implementation is not
     * necessary.
     */
    default void failure() {
        // do nothing
    }
}
