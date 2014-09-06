/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.functional.functor;

/**
 *
 * @author Plutonium_
 */
public interface BiCallback<A,B,R> {
    R call(A o1, B o2);
}
