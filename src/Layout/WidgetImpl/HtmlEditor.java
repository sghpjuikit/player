/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.WidgetImpl;

import Layout.Widgets.ClassWidget;
import Layout.Widgets.IsWidget;
import Layout.Widgets.controller.ClassController;
import javafx.scene.web.HTMLEditor;
import static util.Util.setAnchors;

/**
 *
 * @author Plutonium_
 */
@IsWidget
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
        setAnchors(editor, 0);
    }
    
}
