/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.functional.impl;

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
    
    public static final Validator<String> IsBetween0And1 = isBetween0X(1);
    
    public static final Validator<Integer> icCurrentYear = t -> t == Year.now().getValue();
    
    public static final Validator<String> icCurrentYearS = t -> {
            try {
                int i = new Integer(t);
                int max = Year.now().getValue();
                return i==max;
            } catch(NumberFormatException e) {
                return false;
            }
        };
    
    public static final Validator<Integer> isPastYear = i -> i>=0 && i<=Year.now().getValue();
    
    
    public static final Validator<String> isPastYearS = t -> {
        try {
            int i = new Integer(t);
            int max = Year.now().getValue();
            return i>0 && i<=max;
        } catch(NumberFormatException e) {
            return false;
        }
    };
    
    
    public static final Validator<String> isIntS = t -> {
        try {
            int i = new Integer(t);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    };
    
}
