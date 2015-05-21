/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional.functor;

/** Function which throws an exception. */
@FunctionalInterface
public interface FunctionE<I,O> {
    public O apply(I i) throws Exception;
}
