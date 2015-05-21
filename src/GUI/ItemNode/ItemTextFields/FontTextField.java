/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemNode.ItemTextFields;

import javafx.scene.text.Font;
import org.controlsfx.dialog.Dialogs;
import util.parsing.Parser;

/**
 *
 * @author Plutonium_
 */
public class FontTextField extends ItemTextField<Font> {

    public FontTextField() {
        super(Parser.toConverter(Font.class));
    }
    
    @Override
    void onDialogAction() {
        Font tmp = Dialogs.create().owner(getScene().getWindow()).showFontSelector(v).orElse(null);
        setValue(tmp);
    }
}
