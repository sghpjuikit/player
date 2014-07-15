
package Layout.Widgets;

import Configuration.Config;
import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.InstanceFieldConfig;
import Layout.Component;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import utilities.Log;

/**
 * Widget is an abstract representation of graphical component that works like
 * an individual self sufficient package.
 * 
 * Widget allows a way to handle variety of different components in defined
 * way. It provides necessary logic to wrap the component, which includes such
 * things as id, name, controller or properties.
 * 
 * Internal widgets are the default widgets that are irremovable part of the
 * application, while external are loaded from external source.
 * Internal widgets exist as pure design choice to encapsulate application
 * functionalities. They are part of the source code, but its not necessary.
 * External widgets are loaded by the application, either at start or anytime 
 * during runtime.
 * 
 * Widget is a wrapper of underlying component exhibiting standalone
 * functionality. Its not its role to handle behavior of the component, but it
 * does provide a way to access it by forwarding its controller. Controller is the
 * object responsible for the component's behavior.
 * The controller is instantiated when widget loads. The controller should
 * not be called before that happens and exception will be thrown if in such
 * case.
 * 
 * The Concrete implementation of Widget should make sure it guarantees the
 * existence of Controller after the widget has been loaded. This should be
 * done inside load() method.
 * 
 * Use EMPTY widget instread null
 * 
 * @author uranium
 */
public abstract class Widget<C extends Controller> extends Component implements Configurable {
    
    /** Name of the widget. Permanent */
    public final String name;
    
    @XStreamOmitField
    C controller;
    
    @XStreamOmitField
    @IsConfig(name = "Is preferred", info = "Preferred widget for its widget type.")
    public boolean preferred = false;
    
    // list of properties of the widget to provide serialisation support since
    // controller doesnt serialise - this is unwanted and should be handled better
    public List<Config> configs;
    
    
    /**
     * @param {@link Widget#name}
     */
    public Widget(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public abstract Node load();
    
    /**
     * Returns controller of the widget. It provides access to public behavior
     * of the widget.
     * The controller is instantiated when widget loads. The controller should
     * not be called before that happens and exception will be thrown in such
     * case.
     * @return controller of the widget, never null
     * @throws NullPointerException if controller not yet instantiated
     */
    public C getController() {
        Objects.requireNonNull(controller, "Illegal call. Widget doesnt have a controller before it loads.");
        return controller;
    }
    
    /** @return metadata information for this widget. Never null. */
    public WidgetInfo getInfo() {
        return getFactory().getInfo();
    }
    
    /** @return whether this widget will be preferred over other widgets. */
    public boolean isPreffered() {
        return preferred;
    }
    /** @param val whether this widget will be preferred over other widgets. */
    public void setPreferred(boolean val) {
        preferred = val;
    }
    
    /** @return factory that produces this widget */
    public WidgetFactory getFactory() {
        return WidgetManager.getFactory(name);
    }
    
/******************************************************************************/
    
    /**
     * @return empty widget. Use to inject fake widget instead null value.
     */
    public static Widget EMPTY() {
        return new EmptyWidget();
    }
    
    /**
     * Returns whether this widget is intended to store an actual content or 
     * serves different purpose. An example of such use can be substitution
     * for null value. Widgets returning true are not likely to have their
     * factories registered and be accessible to the user.
     * <p>
     * The value returned by this method should be hard coded by design per widget
     * type (by default it returns false). Dont mistake this method for isEmpty() in 
     * {@link Controller}. Empty widget might require different handling, while 
     * different controller simply indicates that the widget has no content.
     */
    public boolean isEmpty() {
        return this instanceof EmptyWidget;
    }
    
/******************************************************************************/
    
    @Override
    public List<Config> getFields() {
        List<Config> l = Configurable.super.getFields();
                     l.addAll(getController().getFields());
        return l;
    }
    
    @Override
    public boolean setField(String name, String value) {
        boolean set = Configurable.super.setField(name, value) ||
                      getController().setField(name, value);
        if (!set)
            Log.mess("Configuration value couldnt be set for field: " + name + " .");
        return set;
    }
    
    @Override
    public boolean setField(String name, Object value) {
        boolean set = Configurable.super.setField(name, value) ||
                      getController().setField(name, value);
        if (!set)
            Log.mess("Configuration value couldnt be set for field: " + name + " .!");
        return set;
    }
    
    // the following two methods help with serialising the widget settings
    protected void rememberConfigs() {
        if(controller != null) configs = getFields();
    }
    protected void restoreConfigs() {
        if(configs != null) {
            configs.forEach(c -> {
                c.setValue(((InstanceFieldConfig)c).value);
            });
            configs = null;
        }
    }

    /** @return name of the widget */
    @Override
    public String toString() {
        return name;
    }
    
/******************************************************************************/
    
    /** Marks widget's intended functionality. */
    public enum Group {
        APP,
        PLAYBACK,
        PLAYLIST,
        TAGGER,
        LIBRARY,
        VISUALISATION,
        OTHER,
        UNKNOWN;
    }
}