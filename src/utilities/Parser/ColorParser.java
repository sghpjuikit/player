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
    
    /** {@inheritDoc} */
    @Override
    public Color fromS(String source) {
        try {
            return Color.valueOf(source);
        } catch(IllegalArgumentException e) {
            return null;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public String toS(Color object) {
        return object.toString();
    }

}
