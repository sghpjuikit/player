/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Validators;

import java.time.Year;

/**
 *
 * @author Plutonium_
 */
public class isYearString implements Validator<String>{

    @Override
    public boolean test(String t) {
        try {
            int i = new Integer(t);
            int max = Year.now().getValue();
            return i>0 && i<=max;
        } catch(NumberFormatException e) {
            return false;
        }
    }
    
}