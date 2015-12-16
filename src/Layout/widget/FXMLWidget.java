/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget;

import Layout.widget.controller.FXMLController;

/**
 * Widget based on .fxml file and {@link FXMLController}.
 * <p>
 * Widget is loaded from its location. It adopts the .fxml + controller pattern.
 *
 * @see FXMLWidgetFactory
 * @author uranium
 */
public final class FXMLWidget extends Widget<FXMLController> {

    FXMLWidget(String name, FXMLWidgetFactory factory) {
        super(name,factory);
    }

}