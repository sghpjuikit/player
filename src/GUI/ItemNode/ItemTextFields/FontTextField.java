/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemNode.ItemTextFields;

import javafx.scene.text.Font;
import org.controlsfx.dialog.Dialogs;
import util.parsing.ParserImpl.FontParser;
import util.parsing.StringParser;

/**
 *
 * @author Plutonium_
 */
public class FontTextField extends ItemTextField<Font> {

    public FontTextField() {
        this(FontParser.class);
    }
    public FontTextField(Class<? extends StringParser<Font>> parser_type) {
        super(parser_type);
    }
    
    @Override
    void onDialogAction() {
        Font tmp = Dialogs.create().owner(getScene().getWindow()).showFontSelector(v).orElse(null);
        setValue(tmp);
    }

    @Override
    String itemToString(Font item) {
        if(item != null) {
            return new FontParser().toS(item);
        } else {
            return "";
        }
        
    }
}
