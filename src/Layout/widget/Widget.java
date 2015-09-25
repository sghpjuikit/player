
package Layout.widget;

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

import Configuration.CachedCompositeConfigurable;
import Configuration.Config;
import Configuration.Configurable;
import Configuration.IsConfig;
import Layout.Areas.Area;
import Layout.Component;
import Layout.container.Container;
import Layout.widget.controller.Controller;
import Layout.widget.controller.io.InOutput;
import Layout.widget.controller.io.Input;
import Layout.widget.controller.io.IsInput;
import Layout.widget.controller.io.Output;

import static Layout.widget.WidgetManager.WidgetSource.ANY;
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
public abstract class Widget<C extends Controller> extends Component implements CachedCompositeConfigurable<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);

    // Name of the widget. Permanent. same as factory input_name
    // it needs to be declared to support deserialization
    final String name;

    @XStreamOmitField private WidgetFactory factory;
    @XStreamOmitField protected C controller;
    @XStreamOmitField private Node root;
    @XStreamOmitField private HashMap<String,Config<Object>> configs = new HashMap<>();

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
    public String getName() {
        return name;
    }

    @Deprecated
    public Node getGraphics() {
        return root;
    }

    /**
     * Non null only if within container and loaded.
     * {@inheritDoc} */
    @Override
    public Container getParent() {
        if(controller!=null) {
            Area a = controller.getArea();
            if(a!=null) return a.container;
        }
        return null;
    }

    /**
     * Returns this widget's content.
     * <p>
     * If called the 1st time, loads this widget's content and instantiates its
     * controller. The controller will be null until the first time this method
     * is called.
     * Any subsequent call of this method will simply return the content.
     * <p>
     * Details:
     * <ul>
     * <li> graphics and controller is created only once when needed and reused
     * <li> attaching the graphics to the scenegraph will remove it from its old location
     * <li> reattaching to scenegraph has no effect on widget state
     * <li> serialisation is the only time when widget state (configs) need to be taken care of
     * manually
     * </ul>
     * {@inheritDoc}
     * @return graphical content of this widget
     */
    @Override
    public final Node load() {
        if(root==null) {
            controller = instantiateController();
            if(controller==null) {
                root = Widget.EMPTY().load();
            } else {
                Exception ex = null;
                try {
                    root = loadInitial();

                    // we cant call this as parent is injected after this method returns
                    // in WidgetArea
                    // lockedUnder.init();

                    // this call must execute after this widget is attached to the scenegraph
                    // so initialization can access it
                    // however that does not happen here. The root Container and Node should be passed
                    // as parameters of this method
                    loadInitialize();
                } catch(Exception e) {
                    ex = e;
                }
                if(root==null) {
                    root = Widget.EMPTY().load();
                    LOGGER.error("Widget {} graphics creation failed. Using empty widget instead.", getName(),ex);
                }
            }
        }
        return root;
    }

    /**
     * Loads the widget. Solely used for {@link #load()} method called initially,
     * before the loaded content is cached. Should be called only once per life
     * cycle of the widget and internally.
     */
    protected abstract Node loadInitial() throws Exception;
    protected void loadInitialize() {};

    private C instantiateController() {
        // instantiate controller
        Class cc = getFactory().getControllerClass();
        C c;
        try {
            c = (C) cc.newInstance();
        } catch(IllegalAccessException | InstantiationException e) {
            LOGGER.error("Widget controller creation failed {}",cc,e);
            return null;
        }

        // inject this widget into the controller
        util.Util.setField(c, "widget", this);

        // generate inputs
        for(Method m : cc.getDeclaredMethods()) {
            IsInput a = m.getAnnotation(IsInput.class);
            if(a!=null) {
                int params = m.getParameterCount();
                if(Modifier.isStatic(m.getModifiers()) || params>1)
                    throw new RuntimeException("Method " + m + " can not be an input.");

                String i_name = a.value();
                boolean isvoid = params==0;
                Class i_type = isvoid ? Void.class : m.getParameterTypes()[0];
                Consumer i_action = isvoid
                    ?   value -> {
                            if(value!=null)
                                throw new ClassCastException(cc + " " + m + ": Can not cast " + value + " into Void.class");
                            try {
                                m.setAccessible(true);
                                m.invoke(c);
                                m.setAccessible(false);
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                LOGGER.error("Input {} in widget {} failed to process value.",i_name,name,e);
                            }
                        }
                    :   value -> {
                            try {
                                m.setAccessible(true);
                                m.invoke(c, value);
                                m.setAccessible(false);
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                LOGGER.error("Input {} in widget {} failed to process value.",i_name,name,e);
                            }
                        };
                c.getInputs().create(i_name, i_type, i_action);
            }
        }

        return c;
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

    @Override
    public Map<String, Config<Object>> getFieldsMap() {
        return configs;
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
        super.readResolve();

        // try to assign factory
        if (factory==null) factory = WidgetManager.getFactory(name);
        // use empty widget when no factory available
        if (factory==null) return Widget.EMPTY();

        if(configs==null) configs = configs = new HashMap<>();

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