/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import javafx.scene.Node;

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
    public ClassWidgetFactory(String _name, Class<? extends Node> type) {
        super(_name, type);
        if (!Controller.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Class must implement Controller in order to become a widget.");
    }
    
    /** {@inheritDoc} */
    @Override
    public ClassWidget create() {
         return new ClassWidget(name, this);
    }
    
}
