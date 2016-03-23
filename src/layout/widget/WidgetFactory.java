/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import layout.widget.controller.Controller;
import layout.widget.feature.Feature;
import util.ClassName;

/**
 * Factory that creates widgets.
 * <p>
 * @author uranium
 */
@Widget.Info // empty widget info with default values
public class WidgetFactory<C extends Controller<?>> implements WidgetInfo {

    private final String name;
    private final String gui_name;
    private final String description;
    private final String version;
    private final String author;
    private final String programmer;
    private final String contributor;
    private final String howto;
    private final String year;
    private final String notes;
    private final Widget.Group group;

    private final Class<C> controller_class;
    public final File location;

    /** Whether this factory will be preferred over others of the same group. */
    boolean preferred = false;
    /** Whether this factory will be ignored on widget requests. */
    boolean ignored = false;

    /**
     * Implementation note: this constructor must be called from every extending
     * class' constructor with super().
     *
     * @param name name of widget this factory will create
     * @param type class of the controller of the widget this factory will create.
     * There are no restrictions here, but other factories might impose some.
     * In any case, it is recommended for the class to implement {@link Controller}
     * and also be annotated with {@link Widget.Info}
     *
     * @param type controller class
     */
    private WidgetFactory(String name, Class<C> type, File location) {
        this.name = name;
        this.controller_class = type;
        this.location = location;

        Widget.Info i = type.getAnnotation(Widget.Info.class);
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
     * @param type controller class
     */
    public WidgetFactory(Class<C> type) {
        this(type, null);
    }

    public WidgetFactory(Class<C> type, File location) {
        this(ClassName.of(type), type, location);
    }

    /**
     * Creates new widget.
     * @return new widget instance or null if creation fails.
     */
    public Widget<C> create() {
        return new Widget<>(name,this);
    }

    protected Class<C> getControllerClass() {
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

    @Override
    public String name() { return name; }

    @Override
    public String nameGui() { return gui_name; }

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

    public Class type() { return controller_class; }

    @Override
    public boolean hasFeature(Class feature) {
        return feature.isAssignableFrom(controller_class);
    }

    @Override
    public List<Feature> getFeatures() {
        List<Feature> out = new ArrayList<>();
        for(Class<?> c : controller_class.getInterfaces()) {
            Feature f = c.getAnnotation(Feature.class);
            if (f!=null) out.add(f);
        }
        return out;
    }

    @Override
    public String toString() {
        return "WidgetFactory " + name + " " + gui_name + " " + controller_class;
    }

}