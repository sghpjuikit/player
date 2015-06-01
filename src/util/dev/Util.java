/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.dev;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 *
 * @author Plutonium_
 */
public class Util {
    
    public static void forbid(boolean v) {
        if(v) throw new IllegalStateException("Requirement condition not met.");
    }
    
    public static void forbid(boolean v, String s) {
        if(v) throw new IllegalStateException("Requirement condition not met: " + s);
    }
    
    public static void require(boolean v) {
        if(!v) throw new IllegalStateException("Requirement condition not met.");
    }
    
    public static void require(boolean v, String s) {
        if(!v) throw new IllegalStateException("Requirement condition not met: " + s);
    }
    
    public static void forbidNull(Object o) {
        if(o==null) throw new IllegalStateException("Null forbidden");
    }
    public static void forbidNull(Object o, String message) {
        if(o==null) throw new IllegalStateException("Null forbidden. " + message);
    }
    
    public static void forbidFinal(Field f) {
        if(Modifier.isFinal(f.getModifiers())) 
            throw new IllegalStateException("Final field forbidden.");
    }
    
    public static void requireFinal(Field f) {
        if(!Modifier.isFinal(f.getModifiers())) 
            throw new IllegalStateException("Non final field forbidden.");
    }
    
    public static <T> void require(T t, Predicate<T> cond) {
        if(!cond.test(t))
            throw new IllegalStateException("Requirement condition not met.");
    }
}