/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.Components;

import Layout.Widgets.Controller;
import Layout.Widgets.Widget;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.HTMLEditor;

/**
 *
 * @author Plutonium_
 */
public class HtmlEditor extends AnchorPane implements Controller<Widget>  {
    private HTMLEditor editor = new HTMLEditor();
    
    public HtmlEditor() {
        initialize();
    }
    
    private void initialize() {
        editor.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        editor.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        editor.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        
        
        this.getChildren().add(editor);
        AnchorPane.setBottomAnchor(editor,0.0);
        AnchorPane.setTopAnchor(editor,0.0);
        AnchorPane.setRightAnchor(editor,0.0);
        AnchorPane.setLeftAnchor(editor,0.0);
    }
    
    
    
    private Widget widget;
    
    @Override public void refresh() {
    }

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }
    
}
