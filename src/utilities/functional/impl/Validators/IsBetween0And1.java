/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.functional.impl.Validators;

/**
 *
 * @author Plutonium_
 */
public class IsBetween0And1 implements Validator<String> {

    @Override
    public boolean test(String t) {
        try {
            double i = new Double(t);
            return i>=0 && i<=1;
        } catch(NumberFormatException e) {
            return false;
        }
    }
    
}
