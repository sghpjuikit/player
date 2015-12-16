/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget;

import javafx.scene.Node;

import Layout.widget.controller.Controller;

/**
 * Widget wrapping a {@link Node} that implements {@link Controller}.
 * <p>
 * This is done by providing the Node's class, which is then instantiated during loading. The class
 * must implement {@link Controller} and have a no argument constructor.
 *
 * @see ClassWidgetFactory
 * @author uranium
 */
public class ClassWidget extends Widget<Controller> {

    ClassWidget(String name, ClassWidgetFactory factory) {
        super(name,factory);
    }

}