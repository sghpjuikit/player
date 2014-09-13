/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.functional.impl;

import java.time.Year;
import java.util.function.Predicate;

/**
 *
 * @author Plutonium_
 */
public interface Validator<T> extends Predicate<T> {
    
    
    public static Validator<String> isBetween0X(int max) {
        return t -> {
            try {
                double i = new Double(t);
                return i>=0 && i<=max;
            } catch(NumberFormatException e) {
                return false;
            }
        };
    }
    
    public static Validator<String> IsBetween0And1() {
        return isBetween0X(1);
    }
    
    public static Validator<Integer> icCurrentYear() {
        return t -> t == Year.now().getValue();
    }
    
    public static Validator<String> icCurrentYearS() {
        return t -> {
            try {
                int i = new Integer(t);
                int max = Year.now().getValue();
                return i==max;
            } catch(NumberFormatException e) {
                return false;
            }
        };
    }
    
    public static Validator<Integer> isPastYear() {
        return i -> i>0 && i<=Year.now().getValue();
    }
    
    public static Validator<String> isPastYearS() {
        return t -> {
            try {
                int i = new Integer(t);
                int max = Year.now().getValue();
                return i>0 && i<=max;
            } catch(NumberFormatException e) {
                return false;
            }
        };
    }
    
    
    public static Validator<String> isIntS() {
        return t -> {
            try {
                int i = new Integer(t);
                return true;
            } catch(NumberFormatException e) {
                return false;
            }
        };
    }
}
