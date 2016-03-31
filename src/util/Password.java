/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author Michal Szeman
 */
public final class Password {
    private String value;

    public Password(String text) {
        set(text);
    }
    
    public String get(){
        return value;
    }
    
    public void set(String in){
        value = in;
    }

    @Override
    public String toString() {
        return value;
    }
    
    public static Password valueOf(String s) {
        return new Password(s);
    }

}