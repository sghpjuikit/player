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
public class isCurretYear implements Validator<Integer> {
    @Override
    public boolean test(Integer t) {
        int max = Year.now().getValue();
        return t==max;
    }
    
}
