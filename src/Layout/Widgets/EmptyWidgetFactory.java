/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets;

/**
 *
 * @author Plutonium_
 */
public class EmptyWidgetFactory extends WidgetFactory<EmptyWidget> {

    public EmptyWidgetFactory() {
        super("Empty", EmptyWidget.class);
    }
    
    @Override
    public EmptyWidget create() {
        return new EmptyWidget();
    }
    
}
