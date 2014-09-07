/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

/**
 * Factory that creates widgets.
 * <p>
 * @author uranium
 */
@WidgetInfo
public abstract class WidgetFactory<W extends Widget> {
    public final String name;
    public final WidgetInfo info;
    private final Class<?> controller_class;
    
    /** Whether this factory will be preferred over others of the same group. */
    public boolean preferred = false;
    
    /**
     * Implementation note: this constructor must be called from every extending
     * class' constructor with super().
     * 
     * @param name
     * @param controller_class class of the controller. There are no restrictions
     * for this general constructor, but more specific factories might impose some.
     * In any case, it is recommended for the class to be at least Class<Controller>
     *  - the controller should always implement Controller interface
     */
    WidgetFactory(String name, Class<?> controller_class) {
        // init name
        this.name = name;
        this.controller_class = controller_class;
        
        // init info
            // grab Controller's class and its annotation
        WidgetInfo i = controller_class.getAnnotation(WidgetInfo.class);
            // initialize default value if not n/a
        info = i!=null ? i : WidgetFactory.class.getAnnotation(WidgetInfo.class);
        
    }
    
    /**
     * Creates new widget.
     * @return new widget instance or null if creation fails.
     */
    abstract public W create();
    
    public String getName() {
        return name;
    }
    
    protected Class getControllerClass() {
        return controller_class;
    }
    
    /**
     * @return metadata information for widgets of this factory. Never null.
     */
    public WidgetInfo getInfo() {
        return info;
    }
    
    
    /** {@see #preffered} */
    public boolean isPreffered() {
        return preferred;
    }
    /** {@see #preffered} */
    public void setPreferred(boolean val) {
        preferred = val;
    }
    
    
     /**
     * Registers widget factory. Only registered factories can
     * create widgets.
     * Registering can only be done once and can not be undone.
     * Does nothing if specified factory already is registered. Factory can not
     * be registered twice.
     * application.
     */
    void register() {
        if (!isRegistered())
            WidgetManager.factories.put(name,this);
    }
    /**
     * Returns true if the factory is already registered.
     * @return 
     */
    public boolean isRegistered() {
        return WidgetManager.factories.containsKey(name);
    }
}





