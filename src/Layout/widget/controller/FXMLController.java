/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.controller;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import org.reactfx.Subscription;

import Configuration.Config;
import Layout.widget.FXMLWidget;
import Layout.widget.FXMLWidgetFactory;
import Layout.widget.controller.io.Input;
import Layout.widget.controller.io.Inputs;
import Layout.widget.controller.io.Outputs;
import util.dev.Dependency;

/**
 * Controller for {@link FXMLWidget}
 *
 * @author uranium
 */
abstract public class FXMLController implements Controller<FXMLWidget> {

    @Dependency("DO NOT RENAME - accessed using reflection")
    public final FXMLWidget widget = null;
    public final Outputs outputs = new Outputs();
    public final Inputs inputs = new Inputs();
    private final HashMap<String,Config<Object>> configs = new HashMap<>();
    private final List<Subscription> disposables = new ArrayList<>();

    public FXMLController() {

        String nameClass = getClass().getSimpleName();
        String nameWidget = nameClass.substring(0, nameClass.length()-"Controller".length());

        System.out.println("Building controller for : " + getClass() + " " + nameWidget);

    }

    @Override
    public FXMLWidget getWidget() {
        return widget;
    }

    @Override
    public Node loadFirstTime() throws Exception {
        FXMLLoader loader = new FXMLLoader();
                   loader.setLocation(getResource(widget.getName() + ".fxml").toURI().toURL());
                   loader.setController(this);
        return loader.load();
    }

    @Override
    abstract public void refresh();

    /**
     * Returns specified resource file from the widget's location.
     * @param filename of the file with extension. For example: "bgr.jpg"
     */
    public File getResource(String filename) {
        return new File(((FXMLWidgetFactory)widget.factory).location,filename).getAbsoluteFile();
    }

    public void loadSkin(String filename, Pane root) {
        try {
            root.getStylesheets().add(getResource(filename).toURI().toURL().toExternalForm());
        } catch (MalformedURLException ex) {}
    }

    @Override
    public Outputs getOutputs() {
        return outputs;
    }

    @Override
    public Inputs getInputs() {
        return inputs;
    }

    @Override
    public Map<String, Config<Object>> getFieldsMap() {
        return configs;
    }

    @Override
    public final void close() {
        disposables.forEach(Subscription::unsubscribe);
        onClose();
        inputs.getInputs().forEach(Input::unbindAll);
    }

    public void onClose() {}

    /**
     * Adds the subscription to the list of subscriptions that unsubscribe when
     * this controller's widget is closed.
     * <p>
     * Anything that needs to be disposed can be passed here as runnable at any
     * time.
     */
    public void d(Subscription d) {
        disposables.add(d);
    }

}
