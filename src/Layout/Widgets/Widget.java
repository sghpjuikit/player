
package Layout.Widgets;

import java.io.File;
import java.io.ObjectStreamException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.scene.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import Configuration.CompositeConfigurable;
import Configuration.Configurable;
import Configuration.IsConfig;
import Layout.Component;
import Layout.Widgets.controller.Controller;
import Layout.Widgets.controller.io.InOutput;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.IsInput;
import Layout.Widgets.controller.io.Output;

import static Layout.Widgets.WidgetManager.WidgetSource.ANY;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static util.File.FileUtil.writeFile;
import static util.functional.Util.*;

/**
 * Widget graphical component with a functionality.
 * <p>
 * The functionality is handled by widget's {@link Controller}. The controller 
 * is instantiated when widget loads. The widget-controller relationship is 1:1
 * and permanent.
 * <p>
 * Widget can be thought of as a wrapper for controller (which may be used as
 * standalone object if implementation allows). The type of widget influences
 * the lifecycle. See {@link FXMLWidget} and {@link ClassWidget}.
 * 
 * @author uranium
 */
public abstract class Widget<C extends Controller> extends Component implements CompositeConfigurable<Object> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);
    
    // Name of the widget. Permanent. same as factory input_name
    // it needs to be declared to support deserialization
    final String name;
    
    @XStreamOmitField private WidgetFactory factory;
    @XStreamOmitField protected C controller;
    @XStreamOmitField private Node root;
    
    // configuration
    @XStreamOmitField
    @IsConfig(name = "Is preferred", info = "Prefer this widget among all widgets of its type. If there is a request "
            + "for widget, preferred one will be selected. ")
    private boolean preferred = false;
    
    @XStreamOmitField
    @IsConfig(name = "Is ignored", info = "Ignore this widget if there is a request.")
    private boolean forbid_use = false;
    
    
    /**
     * @param {@link Widget#name}
     */
    public Widget(String name, WidgetFactory factory) {
        this.name = name;
        this.factory = factory;
    }
        
    /** {@inheritDoc} */
    @Override
    public String getName() { return name; }
    
    /**
     * Returns this widget's content.
     * <p>
     * If called the 1st time, loads this widget's content and instantiates its
     * controller. The controller will be null until the first time this method
     * is called.
     * Any subsequent call of this method will simply return the content.
     * <p>
     * {@inheritDoc}
     * @return graphical content of this widget
     */
    @Override
    public final Node load() {
        // - loads only once
        // - attaching root to the scenegraph will automatically remove it
        //   from its old location
        // - this guarantees that widget loads only once, which means:
        //   - graphics will be constructed only once
        //   - -||- controller, controller will always be in control of
        //     the correct graphics - normally we would have to load both
        //     graphics and controller multiple times because we can not
        //     assign new graphics to old controller
        // - entire state of the widget is intact with the exception of
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
     */
    protected abstract Node loadInitial();
    
    protected void initializeController() throws InstantiationException, IllegalAccessException {
        // instantiate controller
        Class cclass = getFactory().getControllerClass();
        controller = (C) cclass.newInstance();

        // inject this widget into the controller
        util.Util.setField(controller, "widget", this);

        // generate inputs
        for(Method m : cclass.getDeclaredMethods()) {
            IsInput a = m.getAnnotation(IsInput.class);
            if(a!=null) {
                int params = m.getParameterCount();
                if(Modifier.isStatic(m.getModifiers()) || params>1) 
                    throw new RuntimeException("Method " + m + " can not be an input.");

                String i_name = a.value();
                boolean isvoid = params==0; if(isvoid) System.out.println(i_name + " " + cclass);
                Class i_type = isvoid ? Void.class : m.getParameterTypes()[0];
                Consumer i_action = isvoid 
                    ?   value -> {
                            if(value!=null) 
                                throw new ClassCastException(cclass + " " + m + ": Can not cast " + value + " into Void.class");
                            try {
                                m.setAccessible(true);
                                m.invoke(controller);
                                m.setAccessible(false);
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                LOGGER.error("Input {} in widget {} failed to process value.",i_name,name,e);
                            }
                        }
                    :   value -> {
                            try {
                                m.setAccessible(true);
                                m.invoke(controller, value);
                                m.setAccessible(false);
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                LOGGER.error("Input {} in widget {} failed to process value.",i_name,name,e);
                            }
                        };
                controller.getInputs().create(i_name, i_type, i_action);
            }
        }
    }
    
    /**
     * Returns controller of the widget. It provides access to public behavior
     * of the widget.
     * <p>
     * The controller is instantiated when widget loads. The controller is null
     * before that and this method should not be invoked. 
     * <p>
     * Do not check the output of this method for null! Receiving null implies
     * wrong use of this method.
     * 
     * @return controller of the widget or null if widget has not been loaded
     * yet.
     */
    public C getController() {
        return controller;
    }
    
    /** @return whether this widget will be preferred over other widgets */
    public boolean isPreffered() {
        return preferred;
    }
    /** @param val whether this widget will be preferred over other widgets */
    public void setPreferred(boolean val) {
        preferred = val;
    }
    /** @return whether this widget will be ignored on widget request */
    public boolean isIgnored() {
        return forbid_use;
    }
    /** @param val whether this widget will be ignored on widget request */
    public void setIgnored(boolean val) {
        forbid_use = val;
    }
    
    /** @return factory that produces this widget */
    public WidgetFactory getFactory() {
        return factory;
    }
    
    /** @return factory information about this widget */
    public WidgetInfo getInfo() {
        return factory;
    }
    
    /** Creates a launcher for this widget with default (no predefined) settings. */
    public void exportFxwlDefault(File dir) {
        File f = new File(dir,name + ".fxwl");
        boolean ok = writeFile(f, name);
        if(!ok) LOGGER.error("Unable to export widget launcher for {} into {}", name,f);
    }
    
/******************************************************************************/

    
    /**
     * Returns whether this widget is empty.
     * <p>
     * Empty widget has no graphics. {@link Controller#isEmpty()} indicates 
     * state - that there is no data to display within widget's graphics.
     */
    public boolean isEmpty() {
        return this instanceof EmptyWidget;
    }
    
    /**
     * Returns singleton list containing the controller of this widget
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Collection<Configurable<Object>> getSubConfigurable() {
        return controller==null ? listRO() : listRO(controller);
    }    
    
    /** @return input_name of the widget */
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
    
/****************************** SERIALIZATION *********************************/
    
    /** Invoked just before the serialization. */
    protected Object writeReplace() throws ObjectStreamException {
        // prepare input-output bindings
        getController().getInputs().getInputs().forEach(i -> 
            properties.put("io"+i.getName(), toS(i.getSources(), (Output o) -> o.id.toString(), ":"))
        );
        // prepare configs
        Map<String,String> m = new HashMap();
        getFields().forEach(c -> m.put(c.getName(), c.getValueS()));
        properties.put("configs", m);

        return this;
    }

        
    /**
     * Invoked just after deserialization.
     * 
     * @implSpec
     * Resolve object by initializing non-deserializable fields or providing an
     * alternative instance (e.g. to adhere to singleton pattern).
     */
    protected Object readResolve() throws ObjectStreamException {
        // try to assign factory
        if (factory==null) factory = WidgetManager.getFactory(name);
        // use empty widget when no factory available
        if (factory==null) return Widget.EMPTY();
        
        // accumulate serialized inputs for later deserialiation when all widgets are ready
        properties.entrySet().stream()
                  .filter(e->e.getKey().startsWith("io"))
                  .map(e -> new IO(this,e.getKey().substring(2), (String)e.getValue()))
                  .forEach(ios::add);
        
        return this;
    }
    
    // normally we would do this in readResolve, but controller ready only after
    // widget loads
    protected void restoreConfigs() {
        if(properties.containsKey("configs")) {
            Map<String,String> m = (Map) properties.get("configs");
            m.forEach(this::setField);
            properties.remove("configs");
        }
    }
    
/******************************************************************************/
    
    
    static class IO {
        public final Widget widget;
        public final String input_name;
        public final List<Output.Id> outputs_ids = new ArrayList();

        IO(Widget widget, String name, String outputs) {
            this.widget = widget;
            this.input_name = name;
            this.outputs_ids.addAll(map(split(outputs,":",x->x),Output.Id::fromString));
        }
    }
    
    static final ArrayList<IO> ios = new ArrayList();
    public static void deserializeWidgetIO() {
        Set<Input> is = new HashSet();
        Map<Output.Id,Output> os = WidgetManager.findAll(ANY)
                     .peek((Widget w) -> w.getController().getInputs().getInputs().forEach(is::add))
                     .flatMap(w -> w.getController().getOutputs().getOutputs().stream())
                     .collect(Collectors.toMap(i->i.id, i->i));
        InOutput.inoutputs.forEach(io -> os.put(io.o.id, io.o));
        
        ios.forEach(io -> {
            Input i = io.widget.getController().getInputs().getInput(io.input_name);
            if(i!=null)
                io.outputs_ids.stream().map(os::get).filter(ISNTØ).forEach(i::bind);
        });
    }
    

    /** Widget metadata. Passed from code to program. Use on controller class. */
    @Retention(value = RUNTIME)
    @Target(value = TYPE)
    public static @interface Info {

        /**
         * Name of the widget. "" by default.
         */
        String name() default "";

        /**
         * Description of the widget.
         */
        String description() default "";

        /**
         * Version of the widget
         */
        String version() default "";

        /**
         * Author of the widget
         */
        String author() default "";

        /**
         * Main developer of the widget.
         * @return
         */
        String programmer() default "";

        /**
         * Co-developer of the widget.
         */
        String contributor() default "";

        /**
         * Last time of change.
         * @return
         */
        String year() default "";

        /**
         * How to use text.
         * <pre>
         * For example:
         * "Available actions:\n" +
         * "    Drag cover away : Removes cover\n" +
         * "    Drop image file : Adds cover\n" +
         * "    Drop audio files : Adds files to tagger\n" +
         * "    Write : Saves the tags\n" +
         * "    Open list of tagged items"
         * </pre>
         * @return
         */
        String howto() default "";

        /**
         * Any words from the author, generally about the intention behind or bugs
         * or plans for the widget or simply unrelated to anything else information.
         * <p>
         * For example: "To do: simplify user interface." or: "Discontinued."
         * @return
         */
        String notes() default "";

        /**
         * Group the widget should categorize under as. Default {@link Widget.Group.UNKNOWN}
         * @return
         */
        Widget.Group group() default Widget.Group.UNKNOWN;
    }
    
    /** Widget's intended functionality. */
    public enum Group {
        APP,
        PLAYBACK,
        PLAYLIST,
        TAGGER,
        LIBRARY,
        VISUALISATION,
        OTHER,
        DEVELOPMENT,
        UNKNOWN;
    }
}