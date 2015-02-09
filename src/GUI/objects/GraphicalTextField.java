/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import java.util.ArrayList;
import java.util.List;
import org.controlsfx.control.textfield.CustomTextField;

/**
 <p>
 @author Plutonium_
 */
public class GraphicalTextField extends CustomTextField {

    public GraphicalTextField() {
        //set same css style as TextField
        getStyleClass().setAll(getTextFieldStyleClass());
    }
    
    /** 
     * Returns style class as text field.
     * <p>
     * Should be: text-input, text-field.
     */
    public static List<String> getTextFieldStyleClass() {
        // debug (prints: text-input, text-field.)
//        new TextField().getStyleClass().forEach(System.out::println); 
        
        // general solution, but not optimal
//        return new TextField().getStyleClass();
        
        // manually
        List<String> out = new ArrayList();
                     out.add("text-input");
                     out.add("text-field");
        return out;
    }  
}
