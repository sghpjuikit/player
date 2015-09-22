/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.controller;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.layout.AnchorPane;

import Configuration.Config;
import Layout.widget.ClassWidget;
import Layout.widget.controller.io.Input;
import Layout.widget.controller.io.Inputs;
import Layout.widget.controller.io.Outputs;
import util.dev.Dependency;

/**
 * Controller for {@link ClassWidget}
 * 
 * @author Plutonium_
 */
abstract public class ClassController extends AnchorPane implements Controller<ClassWidget> {
    
    @Dependency("DO NOT RENAME - accessed using reflection")
    public final ClassWidget widget = null;
    public final Outputs outputs = new Outputs();
    public final Inputs inputs = new Inputs();
    private final HashMap<String,Config<Object>> configs = new HashMap<>();

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

    @Override
    public Map<String, Config<Object>> getFieldsMap() {
        return configs;
    }

}
