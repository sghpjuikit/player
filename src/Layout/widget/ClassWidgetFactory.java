/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javafx.scene.Node;

import Layout.widget.controller.Controller;
import util.File.FileUtil;

import static util.functional.Util.array;

/**
 * {@link WidgetFactory} producing {@link ClassWidget}.
 * <p>
 * This class wraps graphical any {@link Node} object into a {Wlink Widget}.
 * <p>
 * @author uranium
 */
public class ClassWidgetFactory extends WidgetFactory<ClassWidget> {

    /**
     * @param _name
     * @param type Factory type - type of object that will be wrapped.
     */
    public <T extends Node & Controller> ClassWidgetFactory(String _name, Class<T> type) {
        super(_name, type);
    }

    public <T extends Node & Controller> ClassWidgetFactory(Class<T> type) {
        super(type);
    }

    public ClassWidgetFactory(File classfile) {
        super(obtainControllerClass(classfile));
    }


    /** {@inheritDoc} */
    @Override
    public ClassWidget create() {
         return new ClassWidget(name, this);
    }

    private static Class<?> obtainControllerClass(File classFile) {
        try {
            URL dir = classFile.getParentFile().toURI().toURL();
            URLClassLoader controllerLoader = new URLClassLoader(array(dir));

            String name_widget = FileUtil.getName(classFile);
            String fname = classFile.getParentFile().getName();
//            String controllerName = fname + "." + name_widget;
            String controllerName = name_widget;

            return controllerLoader.loadClass(controllerName);
        } catch (ClassNotFoundException | MalformedURLException e) {
            throw new RuntimeException("Class widget factory controller class loading failed for: " + classFile, e);
        }
    }
}
