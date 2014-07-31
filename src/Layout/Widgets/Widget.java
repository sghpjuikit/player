
package Layout.Widgets;

import Configuration.Config;
import Configuration.Configurable;
import Configuration.IsConfig;
import Layout.Component;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Node root;  // cache loaded root to avoid loading more than once
    
    @XStreamOmitField
    @IsConfig(name = "Is preferred", info = "Preferred widget for its widget type.")
    public boolean preferred = false;
    
    // list of properties of the widget to provide serialisation support since
    // controller doesnt serialise - this is unwanted and should be handled better
    public Map<String,String> configs;
    
    
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
    public Node load() {
        // if widget has already loaded once, return
        // 1 attaching root to the scenegraph will automatically remove it
        //   from its old location
        // 2 this guarantees that widget loads only once, which means:
        //   - graphics will be constructed only once
        //   - -||- controller, controller will always be in control of
        //     the correct graphics - normally we would have to load both
        //     graphics and controller multiple times because we can not
        //     assign new graphics to old controller
        // 3 entire state of the widget is intact with the exception of
        //   initial load at deserialisation.
        //   This also makes deserialisation the only time when configs
        //   need to be taken care of manually
        if(root==null) root = loadInitial();
        return root;
    }
    
    protected abstract Node loadInitial();
    
    /**
     * Returns controller of the widget. It provides access to public behavior
     * of the widget.
     * <p>
     * The controller is instantiated when widget loads. The controller should
     * not be used (and needed) before that happens.
     * <p>
     * Do not check the output of this method for null! Receiving null implies
     * wrong use of this method (with the exception of internal use)
     * 
     * @return controller of the widget or null if widget has not been loaded
     * yet.
     */
    public C getController() {
        if(controller == null)
            Log.warn("Possible illegal call. Widget doesnt have a controller yet.");
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
        WidgetFactory f = WidgetManager.getFactory(name);
        assert f!=null;
        return f;
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
    public void rememberConfigs() {
        if(controller != null) {
            configs = new HashMap();
            getFields().forEach(c -> 
                configs.put(c.getName(), c.toS())
            );
        }
    }
    public void restoreConfigs() {
        if(configs != null) {
            configs.forEach(this::setField);
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