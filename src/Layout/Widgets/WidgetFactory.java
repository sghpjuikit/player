/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import java.util.ArrayList;
import java.util.List;

import Layout.Widgets.feature.Feature;

/**
 * Factory that creates widgets.
 * <p>
 * @author uranium
 */
@Widget.Info // empty widget info with default values
public abstract class WidgetFactory<W extends Widget> implements WidgetInfo {

    final String name;
    final String gui_name;
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
     * @param name name of widget this factory will create
     * @param c class of the controller of the widget this factory will create.
     * There are no restrictions here, but other factories might impose some.
     * In any case, it is recommended for the class to implement {@link Controller}
     * and also be annotated with {@link Widget.Info}
     *
     * @param c controller class
     */
    WidgetFactory(String name, Class<?> c) {
        // init name
        this.name = name;
        this.controller_class = c;

        // init info
            // grab Controller's class and its annotation or get default
        Widget.Info i = c.getAnnotation(Widget.Info.class);
        if (i==null) i = WidgetFactory.class.getAnnotation(Widget.Info.class);

        gui_name = i.name().isEmpty() ? name : i.name();
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
     * Calls {@link #WidgetFactory(java.lang.String, java.lang.Class)} with
     * name derived from the class. If {@link Widget.Info} annotation is present
     * (as it should) the name field will be used. Otherwise the controller class
     * name will be used as widget factory name.
     * @param c controller class
     */
    public WidgetFactory(Class<?> c) {
        this(getNameFromAnnotation(c), c);
    }

    private static String getNameFromAnnotation(Class<?> c) {
        Widget.Info i = c.getAnnotation(Widget.Info.class);
        return i==null ? c.getSimpleName() : i.name();
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
    public String name() { return gui_name; }

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

    @Override
    public Class type() { return controller_class; }

    /** {@inheritDoc} */
    @Override
    public boolean hasFeature(Class feature) {
        return feature.isAssignableFrom(controller_class);
    }

    /** {@inheritDoc} */
    @Override
    public List<Feature> getFeatures() {
        List<Feature> out = new ArrayList();
        for(Class c : controller_class.getInterfaces()) {
            Feature f = (Feature) c.getAnnotation(Feature.class);
            if (f!=null) out.add(f);
        }
        return out;
    }

}