/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.ItemTextFields;

import javafx.scene.text.Font;
import main.App;
import org.controlsfx.dialog.Dialogs;
import utilities.Parser.FontParser;
import utilities.Parser.Parser;
import utilities.Parser.StringParser;

/**
 *
 * @author Plutonium_
 */
public class FontTextField extends ItemTextField<Font,StringParser<Font>> {

    public FontTextField() {
        this(FontParser.class);
    }
    public FontTextField(Class<? extends StringParser<Font>> parser_type) {
        super(parser_type);
    }
    
    @Override
    void onDialogAction() {
        Font tmp = Dialogs.create().owner(App.getInstance().getScene().getWindow()).showFontSelector(item);
        setItem(tmp);
    }

    @Override
    String itemToString(Font item) {
        if(item != null) {
            return Parser.toS(item);
        } else {
            return "";
        }
        
    }
}
