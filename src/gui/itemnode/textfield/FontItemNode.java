/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode.textfield;

import javafx.scene.text.Font;

import gui.Gui;
import util.parsing.Parser;

/**
 *
 * @author Plutonium_
 */
public class FontItemNode extends TextFieldItemNode<Font> {

    public FontItemNode() {
        super(Parser.DEFAULT.toConverter(Font.class));
    }

    @Override
    void onDialogAction() {
        new org.controlsfx.dialog.FontSelectorDialog(Gui.font.get())
               .showAndWait()
               .ifPresent(this::setValue);
    }
}