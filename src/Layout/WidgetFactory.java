/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

/**
 * Object that creates widgets.
 * @author uranium
 */
public abstract class WidgetFactory {
    public final String name;
    WidgetInfo info;
    
    private boolean preferred = false;
    
    WidgetFactory(String _name) {
        name = _name;
    }
    
    /**
     * Creates new widget.
     * @return tne widget instance or null if creation fails.
     */
    abstract public Widget create();
    
    public String getName() {
        return name;
    }
    
    /**
     * @return metadata information for widgets of this factory. Never null.
     */
    public WidgetInfo getInfo() {
        return info;
    }
    
    
    /** @return whether this factory will be preferred over others of the same group. */
    public boolean isPreffered() {
        return preferred;
    }
    /** @param val whether this factory will be preferred over others of the same group. */
    public void setPreferred(boolean val) {
        preferred = val;
    }
    
    
     /**
     * Registers widget factory if it isnt yet. Only registered factories can
     * create widgets.
     * Registering can only be done once and can not be undone.
     * Does nothing if specified factory already is registered. Factory can not
     * be registered twice.
     * application.
     */
    void register() {
        if (!isRegistered())
            WidgetManager.factories.add(this);
    }
    /**
     * Returns true if the factory is already registered.
     * @return 
     */
    public boolean isRegistered() {
        return WidgetManager.factories.contains(this);
    }
    
    
    
    /**
     * Sets meta information that will be passed down on widgets produced by this
     * factory. This information is extracted from specified widget's controller.
     * If no info is found then it is initialized to empty values.
     * This method guarantees correctly initialized values.
     * @param w 
     */
    void initInfo(Widget w) {
        info = w.getController().getClass().getAnnotation(WidgetInfo.class); // empty info
        if (info == null)
            initInfo(Widget.EMPTY());
    }
}





