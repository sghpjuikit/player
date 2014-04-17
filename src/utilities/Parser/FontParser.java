
package utilities.Parser;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import utilities.Log;

/**
 * 
 * Font String Converter.
 *
 * @author Plutonium_
 */
public class FontParser implements SingleStringParser<Font> {
    
    @Override
    public boolean supports(Class type) {
        return type.equals(Font.class);
    }
    
    /** Converts Font into String
     * @param font.
     */
    @Override
    public String toS(Font font) {
        return font == null? "": String.format("%s, %s", font.getName(), font.getSize());
    }

    /** 
     * Parses Font from String.
     * @param str
     * @return Font parsed fromString or default Font if parsing unsuccessful.
     * Never null, always valid font. Success of parsing can be checked by
     * querying the name of the returned font.
     */
    @Override
    public Font fromS(String str) {
        try {
            int i = str.indexOf(',');
            String name = str.substring(0, i);
            FontPosture style = str.toLowerCase().contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
            FontWeight weight = str.toLowerCase().contains("bold") ? FontWeight.BOLD : FontWeight.NORMAL;
            double size = Double.parseDouble(str.substring(i+2));
            return Font.font(name, weight, style, size);
        } catch(NumberFormatException | IndexOutOfBoundsException e) {
            Log.err("Font cannot be parsed from '" + str + "'. Using default font.");
            return Font.getDefault();
        }
    }
    
}
