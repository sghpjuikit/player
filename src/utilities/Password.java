/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

/**
 *
 * @author Michal
 */
public final class Password {
    String value;

    public Password(String text) {
        set(text);
    }
    
    public String get(){
        return value;
    }
    
    public void set(String in){
        value = in;
    }
}
