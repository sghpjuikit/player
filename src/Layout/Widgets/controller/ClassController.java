/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller;

import Layout.Widgets.ClassWidget;
import Layout.Widgets.controller.io.Inputs;
import Layout.Widgets.controller.io.Outputs;
import javafx.scene.layout.AnchorPane;
import static util.dev.Util.require;

/**
 * Controller for {@link ClassWidget}
 * 
 * @author Plutonium_
 */
abstract public class ClassController extends AnchorPane implements Controller<ClassWidget> {
    
    private ClassWidget widget;
    public final Outputs outputs = new Outputs();
    public final Inputs inputs = new Inputs();
    
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
    
    /** {@inheritDoc} */
    @Override
    public Outputs getOutputs() {
        return outputs;
    }
    
    /** {@inheritDoc} */
    @Override
    public Inputs getInputs() {
        return inputs;
    }
}
