/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.ItemNode;

import gui.ItemNode.ItemNode.ItemNodeBase;
import javafx.scene.control.TextField;

/**
 *
 * @author Plutonium_
 */
public class StringSplitGenerator extends ItemNodeBase<StringSplitParser> {
    private final TextField root = new TextField();

    public StringSplitGenerator() {
        root.setPromptText("expression");
        root.textProperty().addListener((o,ov,nv) -> generateValue(nv));
        generateValue("%input%");
    }
    
    @Override
    public TextField getNode() {
        return root;
    }
    
    private void generateValue(String s) {
        try {
            root.setText(s);
            changeValue(new StringSplitParser(s));
        } catch(IllegalArgumentException e) {}    // ignore invalid values
    }
    
}
