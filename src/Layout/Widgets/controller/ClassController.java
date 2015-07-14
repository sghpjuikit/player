/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller;

import Layout.Widgets.ClassWidget;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.Inputs;
import Layout.Widgets.controller.io.Outputs;
import javafx.scene.layout.AnchorPane;

/**
 * Controller for {@link ClassWidget}
 * 
 * @author Plutonium_
 */
abstract public class ClassController extends AnchorPane implements Controller<ClassWidget> {
    
    public final ClassWidget widget = null;
    public final Outputs outputs = new Outputs();
    public final Inputs inputs = new Inputs();

    @Override
    public ClassWidget getWidget() {
        return widget;
    }

    @Override
    public void refresh() {}

    @Override
    public final void close() {
        onClose();
        inputs.getInputs().forEach(Input::unbindAll);
    }
    
    public void onClose() {}
    
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
