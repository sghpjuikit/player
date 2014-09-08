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
@Widget.Info // empty widget info with default values
public abstract class WidgetFactory<W extends Widget> implements WidgetInfo {
    
    final String name;
    final String description;
    final String version;
    final String author;
    final String programmer;
    final String contributor;
    final String howto;
    final String year;
    final String notes;
    final Widget.Group group;
    
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
        Widget.Info i = controller_class.getAnnotation(Widget.Info.class);
            // initialize default value if not n/a
        if (i==null) i = WidgetFactory.class.getAnnotation(Widget.Info.class);
        
//        name = i.name(); // ignore this for now to not break name contract
        description = i.description();
        version = i.version();
        author = i.author();
        programmer = i.programmer();
        contributor = i.contributor();
        howto = i.howto();
        year = i.year();
        notes = i.year();
        group = i.group();
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
        if (!isRegistered()) WidgetManager.factories.put(name,this);
    }
    /**
     * Returns true if the factory is already registered.
     * @return 
     */
    public boolean isRegistered() {
        return WidgetManager.factories.containsKey(name);
    }

    @Override
    public String name() { return name; }

    @Override
    public String description() { return description; }

    @Override
    public String version() { return version; }

    @Override
    public String author() { return author; }

    @Override
    public String programmer() { return programmer; }

    @Override
    public String contributor() { return contributor; }

    @Override
    public String year() { return year; }

    @Override
    public String howto() { return howto; }

    @Override
    public String notes() { return notes; }

    @Override
    public Widget.Group group() { return group; }
    
}





