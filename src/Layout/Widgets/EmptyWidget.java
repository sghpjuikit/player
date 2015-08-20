/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Configuration.Config;
import Layout.Widgets.controller.Controller;
import Layout.Widgets.controller.io.Inputs;
import Layout.Widgets.controller.io.Outputs;

import java.util.Collection;

import javafx.scene.Node;
import javafx.scene.layout.Region;

import static com.sun.org.apache.xerces.internal.impl.xs.util.ShortListImpl.EMPTY_LIST;

/**
 * Empty widget.
 * <p>
 * Useful for certain layout operations and as a fill in for null.
 * <p>
 * Also its own Controller. Loading returns empty {@link Region}.
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Empty",
    description = "Empty widget with no content or functionality.",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
class EmptyWidget extends Widget<Controller> implements Controller<EmptyWidget> {

    private final Widget widget = null;
    private final Outputs o = new Outputs();
    private final Inputs i = new Inputs();
    
    public EmptyWidget() {
        super("Empty", new EmptyWidgetFactory());
    }

    @Override
    public Collection<Config<Object>> getFields() {
        // cant use default implementation. it calls getFields on the controller
        // since this=this.controller -> StackOverflow
        return EMPTY_LIST;
    }
    
    @Override public Node loadInitial() {
        return new Region();
    }
    
    @Override public Controller getController() {
        return this;
    }
    
    /** This implementation is no-op */
    @Override public void refresh() { }

    @Override
    public EmptyWidget getWidget() {
        return this;
    }

    @Override
    public Outputs getOutputs() {
        return o;
    }
    @Override
    public Inputs getInputs() {
        return i;
    }
    
    
}
