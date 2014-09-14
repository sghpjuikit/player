/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Layout.Widgets.Features.Feature;

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
    boolean preferred = false;
    /** Whether this factory will be ignored on widget requests. */
    boolean ignored = false;
    
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
            // initialize default value if n/a
        if (i==null) i = WidgetFactory.class.getAnnotation(Widget.Info.class);
        
//        name = i.name(); // ignore this for now to not break name contract
        description = i.description();
        version = i.version();
        author = i.author();
        programmer = i.programmer();
        contributor = i.contributor();
        howto = i.howto();
        year = i.year();
        notes = i.notes();
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
    public boolean isPreferred() {
        return preferred;
    }
    /** {@see #preffered} */
    public void setPreferred(boolean val) {
        preferred = val;
    }
    /** {@see #ignored} */
    public boolean isIgnored() {
        return ignored;
    }
    /** {@see #ignored} */
    public void setIgnored(boolean val) {
        ignored = val;
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
    
    /** @return true if the factory is already registered. */
    public boolean isRegistered() {
        return WidgetManager.factories.containsKey(name);
    }

    /** {@inheritDoc} */
    @Override
    public String name() { return name; }

    /** {@inheritDoc} */
    @Override
    public String description() { return description; }

    /** {@inheritDoc} */
    @Override
    public String version() { return version; }

    /** {@inheritDoc} */
    @Override
    public String author() { return author; }

    /** {@inheritDoc} */
    @Override
    public String programmer() { return programmer; }

    /** {@inheritDoc} */
    @Override
    public String contributor() { return contributor; }

    /** {@inheritDoc} */
    @Override
    public String year() { return year; }

    /** {@inheritDoc} */
    @Override
    public String howto() { return howto; }

    /** {@inheritDoc} */
    @Override
    public String notes() { return notes; }

    /** {@inheritDoc} */
    @Override
    public Widget.Group group() { return group; }

    /** {@inheritDoc} */
    @Override
    public boolean hasFeature(Class<? extends Feature> feature) {
        return feature.isAssignableFrom(controller_class);
    }
    
}