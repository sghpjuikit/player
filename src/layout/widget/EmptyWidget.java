/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.widget;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.layout.Region;

import util.conf.Config;
import layout.widget.controller.Controller;
import layout.widget.controller.io.Inputs;
import layout.widget.controller.io.Outputs;

import static java.util.Collections.EMPTY_MAP;

/**
 * Empty widget.
 * <p>
 * Useful for certain layout operations and as a fill in for null.
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
class EmptyWidget extends Widget<EmptyWidget> implements Controller<EmptyWidget> {

    private final Outputs o = new Outputs();
    private final Inputs i = new Inputs();

    public EmptyWidget() {
        super("Empty");
        controller = this;
    }

    @Override
    public Collection<Config<Object>> getFields() {
        // cant use default implementation. it calls getFields on the controller
        // since this=this.controller -> StackOverflow
        return Collections.EMPTY_LIST;
    }

    @Override
    public Node loadFirstTime() {
        return new Region();
    }

    /** This implementation is no-op */
    @Override
    public void refresh() {}

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

    @Override
    public Config<Object> getField(String n) {
        return null;
    }

    @Override
    public Map<String, Config<Object>> getFieldsMap() {
        return EMPTY_MAP;
    }

    @Override
    protected Object readResolve() throws ObjectStreamException {
        root = new Region();
        controller = this;
        return super.readResolve();
    }

}
