/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

import javafx.scene.paint.Color;

/**
 *
 * @author Plutonium_
 */
public class ColorParser implements StringParser<Color> {

    /**
     * @param type
     * @return true if and only if the class is Color or its subclass
     */
    @Override
    public boolean supports(Class<?> type) {
        return Color.class.isAssignableFrom(type);
    }
    
    @Override
    public Color fromS(String source) {
        try {
            return Color.valueOf(source);
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toS(Color object) {
        return object.toString();
    }
    
}
