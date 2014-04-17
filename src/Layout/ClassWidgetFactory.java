/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import javafx.scene.Node;

/**
 *
 * @author uranium
 */
public class ClassWidgetFactory extends WidgetFactory {
    private final Class<?> type;

    /**
     * For Class based widgets Class type must extend Node.
     *
     * @param _name
     * @param type must implement/extend Controller.class and Node.class
     * @throws RuntimeException when param type doesnt fulfill
     * requirements.
     */
    public ClassWidgetFactory(String _name, Class<?> type) {
        super(_name);
        if (!Node.class.isAssignableFrom(type))
            throw new RuntimeException("Unable to create widget. Class must extend Node.");
        
        this.type = type;
        this.info = type.getAnnotation(WidgetInfo.class);
        if (info == null)
            info = Widget.EMPTY().getController().getClass().getAnnotation(WidgetInfo.class);
    }
    
    @Override
    public Widget create() {
         Widget w = new ClassWidget(name, (Class<? extends Node>) type);
         return w;
    }
    
}
