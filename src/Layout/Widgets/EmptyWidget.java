/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Configuration.Config;
import java.util.Collections;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * Empty widget.
 * <p>
 * Useful for certain layout operations and as a fill in for null
 * <p>.
 * Also its own Controller. To be able to be part of scene graph it returns
 * empty node when loaded.
 *  
 * @author uranium
 */
@WidgetInfo(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Empty",
    description = "Empty widget with no content or functionality.",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
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
    
    
    @Override public Node loadInitial() {
        return new Region();
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
