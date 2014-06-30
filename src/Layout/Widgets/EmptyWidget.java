/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Configuration.Config;
import java.util.Collections;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 * Empty widget.
 * Widget wrapping empty AnchorPane. Its useful for certain layout operations
 * as a fill null object - when no widget is present.
 * 
 * Abstract oriented mind will notice that this widget is its own controller
 * which along with being empty makes an interesting case of primitive widget
 * implementation, because both Controller's getWidget, Widget's getController()
 * point to each other - the same object - itself.
 *  
 * @author uranium
 */
@WidgetInfo
class EmptyWidget extends Widget<Controller> implements Controller<EmptyWidget> {

    public EmptyWidget() {
        super("Empty");
    }
    
    /** @return empty (default) widget information */
    @Override public WidgetInfo getInfo() {
        return getClass().getAnnotation(WidgetInfo.class);
    }

    @Override
    public List<Config> getFields() {
    // cant use default implementation. it calls getFields on the controller
    // since this=this.controller -> StackOverflow
        return Collections.EMPTY_LIST;
    }
    
    
    @Override public Node load() {
        return new AnchorPane();
    }
    
    @Override public Controller getController() {
        return this;
    }
    
    /** This implementation is no-op */
    @Override public void refresh() {
        // no-op
    }

    @Override
    public void setWidget(EmptyWidget w) {
        // no-op
    }

    @Override
    public EmptyWidget getWidget() {
        return this;
    }
}
