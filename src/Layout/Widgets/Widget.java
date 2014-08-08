
package Layout.Widgets;

import Configuration.CompositeConfigurable;
import Configuration.Configurable;
import Configuration.IsConfig;
import Layout.Component;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import utilities.Log;

/**
 * Widget is an abstract representation of graphical component that works like
 * an individual self sufficient modul.
 * <p>
 * Widgets allows a way to handle variety of different components in defined
 * way. It provides necessary logic to wrap the component, which includes such
 * things as id, name, controller or properties
 * <p>
 * Widget is a wrapper of underlying component exhibiting standalone
 * functionality. Its not its role to handle behavior of the component, but it
 * does provide a way to access it by forwarding its controller. Controller is the
 * object responsible for the component's behavior.
 * The controller is instantiated when widget loads. The controller should
 * not be called before that happens.
 * <p>
 * The Concrete implementation of Widget should make sure it guarantees the
 * existence of Controller after the widget has been loaded. This should be
 * done inside load() method.
 * <p>
 * Always use EMPTY widget instead null
 * 
 * @author uranium
 */
public abstract class Widget<C extends Controller> extends Component implements CompositeConfigurable {
    
    /** Name of the widget. Permanent */
    public final String name;
    
    @XStreamOmitField
    C controller;
    
    @XStreamOmitField
    private Node root;  // cache loaded root to avoid loading more than once
    
    @XStreamOmitField
    @IsConfig(name = "Is preferred", info = "Preferred widget for its widget type.")
    private boolean preferred = false;
    
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
    
    /**
     * Loads this widget's content.
     * The content is cached and will only be loaded once.
     * Any subsequent call of this method will simply reattach the content to
     * new location in the sceneGraph.
     * <p>
     * Only if the load is initial, the controller will be refreshed.
     * <p>
     * Widget's controller will be null until the first time the widget loads.
     * {@inheritDoc}
     * @return 
     */
    @Override
    public final Node load() {
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
    
    /**
     * Loads the widget. Solely used for {@link #load()} method called initially,
     * before the loaded content is cached. Should be called only once per life
     * cycle of the widget and internally.
     * @return 
     */
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
        assert f!=null; // factory must never be null
        return f;
    }
    
/******************************************************************************/

    
    /**
     * Returns whether this widget is intended to store an actual content or 
     * serves different purpose such as substitution for null value.
     * <p>
     * The value returned by this method should be hard coded by design per widget
     * type (by default is false). Dont mistake this method for isEmpty() in 
     * {@link Controller}. Empty widget might require different handling, while 
     * empty controller simply indicates that the widget has currently no content.
     */
    public boolean isEmpty() {
        return this instanceof EmptyWidget;
    }
    
    /**
     * Returns the controller of this widget
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Configurable getSubConfigurable() {
        return controller;
    }
    
    // the following two methods help with serialising the widget settings
    public void rememberConfigs() {
        if(controller != null) {
            configs = new HashMap();
            getFields().forEach(c -> 
                configs.put(c.getName(), c.getValueS())
            );
        }
    }
    public void restoreConfigs() {
        if(configs != null) {
            configs.forEach((nam,value) -> this.setField(nam, value));
//            configs.forEach(this::setField);
            configs = null;
        }
    }

    /** @return name of the widget */
    @Override
    public String toString() {
        return name;
    }
    
    
    /**
     * @return empty widget. Use to inject fake widget instead null value.
     */
    public static Widget EMPTY() {
        return new EmptyWidget();
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