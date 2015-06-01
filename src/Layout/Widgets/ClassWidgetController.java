/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import javafx.scene.layout.AnchorPane;
import static util.dev.Util.require;

/**
 * Controller for {@link ClassWidget}
 * 
 * @author Plutonium_
 */
abstract public class ClassWidgetController extends AnchorPane implements Controller<ClassWidget> {
    
    private ClassWidget widget;
    
    @Override
    public void setWidget(ClassWidget w) {
        require(widget==null);
        widget = w;
    }

    @Override
    public ClassWidget getWidget() {
        return widget;
    }

    @Override
    public void refresh() {}

    @Override
    public void close() {}
}
