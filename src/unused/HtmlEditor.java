/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package unused;

import javafx.scene.web.HTMLEditor;

import Layout.widget.controller.ClassController;

import static util.graphics.Util.setAnchors;

/**
 *
 * @author Plutonium_
 */
//@IsWidget // enable to make into widget
public class HtmlEditor extends ClassController  {
    private HTMLEditor editor = new HTMLEditor();
    
    public HtmlEditor() {
        editor.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        editor.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        editor.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        
        this.getChildren().add(editor);
        setAnchors(editor, 0d);
    }
    
}
