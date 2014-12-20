/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional.functor;

/**
 *
 * @author Plutonium_
 */
@FunctionalInterface
public interface BiCallback<IN1,IN2,OUT> {
    OUT call(IN1 param1, IN2 param2);
}
