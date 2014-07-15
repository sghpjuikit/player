/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Validators;

/**
 *
 * @author Plutonium_
 */
public class isIntString implements Validator<String> {

    @Override
    public boolean test(String t) {
        try {
            int i = new Integer(t);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }
    
}
