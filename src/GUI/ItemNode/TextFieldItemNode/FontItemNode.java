/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode.TextFieldItemNode;

import javafx.scene.text.Font;
import util.parsing.Parser;

/**
 *
 * @author Plutonium_
 */
public class FontItemNode extends TextFieldItemNode<Font> {

    public FontItemNode() {
        super(Parser.toConverter(Font.class));
    }
    
    @Override
    void onDialogAction() {
//        Font tmp = Dialogs.create().owner(getScene().getWindow()).showFontSelector(v).orElse(null);
        Font tmp = new org.controlsfx.dialog.FontSelectorDialog(gui.GUI.font.get()).showAndWait().get();
        setValue(tmp);
    }
}