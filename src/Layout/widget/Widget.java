
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

import util.conf.CachedCompositeConfigurable;
import util.conf.Config;
import util.conf.Configurable;
import util.conf.IsConfig;
import Layout.Areas.Area;
import Layout.Areas.IOLayer;
import Layout.Component;
import Layout.container.Container;
import Layout.widget.controller.Controller;
import Layout.widget.controller.io.Input;
import Layout.widget.controller.io.IsInput;
import Layout.widget.controller.io.Output;

import static Layout.widget.WidgetManager.WidgetSource.OPEN;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static main.App.APP;
import static util.File.FileUtil.writeFile;
import static util.async.Async.runLater;
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
public class Widget<C extends Controller<?>> extends Component implements CachedCompositeConfigurable<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);

    // Name of the widget. Permanent. Same as factory name. Used solely for deserialization (to find
    // appropriate factory)
    // TODO: put it insode propertymap
    private final String name;

    /**
     * Factory that produced this widget.
     * <p>
     * Note that in case the application creates another version of the factory (e.g., when the
     * widget source code has been modified and recompiled in runtime), even though the old factory
     * will be removed from the list of factories (substituted by the new factory), this field will
     * still point to the old factory, holding true to the description of this field.
     */
    @XStreamOmitField public final WidgetFactory<?> factory;
    @XStreamOmitField protected C controller;
    @XStreamOmitField protected Node root;

    @XStreamOmitField private HashMap<String,Config<Object>> configs = new HashMap<>();

    // Temporary workaround for bad design. Widget-COntainer-Controller-Area relationship is badly
    // designed. This particular problem: Area can contain not yet loaded widget. Thus, we cant
    // use controller (null) to obtain area.
    //
    // I think this is the best and most painless way to wire widget with area & container (parent)
    // All the pseudo wiring through Controller is pure chaos.
    @XStreamOmitField @Deprecated public Container parentTemp;
    @XStreamOmitField @Deprecated public Area areaTemp;

    // configuration (we avoid serializing as the fields already serialize as configs)
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
    public Widget(String name) {
        this.name = name;
        this.factory = APP.widgetManager.factories.get(name);
    }

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
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Container<?> getParent() {
        return parentTemp;
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
                    root = controller.loadFirstTime();
                    // we cant call this as parent is injected after this method returns
                    // in WidgetArea
                    // lockedUnder.init();

                    // this call must execute after this widget is attached to the scenegraph
                    // so initialization can access it
                    // however that does not happen here. The root Container and Node should be passed
                    // as parameters to this method
                    controller.init();
                    restoreConfigs();
                    controller.refresh();

                    updateIO();
                } catch(Exception e) {
                    ex = e;
                }
                if(root==null) {
                    root = Widget.EMPTY().load();
                    LOGGER.error("Widget {} graphics creation failed. Using empty widget instead.", getName(),ex);
                }
            }
        }

        updateIOLayout();

        return root;
    }

    public boolean isLoaded() {
        return root!=null;
    }

    private C instantiateController() {
        // instantiate controller
        Class<?> cc = factory.getControllerClass();
        C c;
        try {
            c = (C) cc.newInstance();
        } catch(IllegalAccessException | InstantiationException e) {
            LOGGER.error("Widget controller creation failed {}",cc,e);
            return null;
        }

        // inject this widget into the controller
        // temporary 'dirty' solution
        try {
            util.Util.getField(c.getClass(), "widget"); // we use this as a check, throws Exception on fail
            util.Util.setField(c, "widget", this); // executes only if the field exists
        } catch (NoSuchFieldException ex) {

        }

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

    public void close() {
        if(controller!=null) {
            // We set controlelr null (before closing it). Makes sure that:
            // 1) widget is unusable
            // 2) calling this method again is no-op
            // Avoids:
            // 1) stackoverflow if controller.close() calls widget.close() for some reason
            // 2) stackoverflow when widget==controller, so close() would execute recursively
            Controller<?> c = controller;
            controller = null;

            IOLayer.all_inputs.removeAll(c.getInputs().getInputs());
            IOLayer.all_outputs.removeAll(c.getOutputs().getOutputs());
            c.close();
        }

        // Not the best handling, but at least dev doesnt have to do this manually and concern
        // himself
        APP.widgetManager.standaloneWidgets.remove(this);
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

    /**
     * Sets state of this widget to that of a target widget's.
     * @param w widget to copy state from
     */
    public void setStateFrom(Widget<C> w) {
        // if widget was loaded we store its latest state, otherwise it contains serialized pme
        if(w.controller!=null)
            w.storeConfigs();

        // this takes care of any custom state or controller persistence state or deserialized
        // configs/inputs/outputs
        properties.clear();
        properties.putAll(w.properties);

        util.Util.setField(this, "id", w.id); // (nasty cheat) not sure if this 100% required
        preferred = w.preferred;
        forbid_use = w.forbid_use;
        loadType.set(w.loadType.get());
        locked.set(w.locked.get());

        // if this widget is loaded we apply state, otherwise its done when it loads
        if(controller!=null)
            restoreConfigs();
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
        return getClass() + name;
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
        boolean isLoaded = controller!=null;

        // Prepare input-outputs
        // If widget is loaded, we serialize inputs & outputs
        if(isLoaded) {
            getController().getInputs().getInputs().forEach(i ->
                properties.put("io"+i.getName(), toS(i.getSources(), o -> o.id.toString(), ":"))
            );
        // Otherwise we still have the deserialized inputs/outputs leave them as they are
        } else {}

        // Prepare configs
        // If widget is loaded, we serialize name:value pairs
        if(isLoaded) {
            storeConfigs();
        // Otherwise we still have the deserialized name:value pairs and leave them as they are
        } else {}

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
        if(factory==null) util.Util.setField(this, "factory", APP.widgetManager.factories.get(name));

        // use empty widget when no factory available
        if(factory==null) return Widget.EMPTY();

        if(configs==null) configs = new HashMap<>();

        // accumulate serialized inputs for later deserialiation when all widgets are ready
        properties.entrySet().stream()
                  .filter(e -> e.getKey().startsWith("io"))
                  .map(e -> new IO(this,e.getKey().substring(2), (String)e.getValue()))
                  .forEach(ios::add);

        return this;
    }

    private void storeConfigs() {
        if(controller==null) return;
        Map<String,String> serialized_configs = new HashMap<>();
        getFields().forEach(c -> serialized_configs.put(c.getName(), c.getValueS()));
        properties.put("configs", serialized_configs);
    }
    private void restoreConfigs() {
        Map<String,String> deserialized_configs = (Map) properties.get("configs");
        if(deserialized_configs!=null) {
            deserialized_configs.forEach(this::setField);
            properties.remove("configs"); // restoration can only ever happen once
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

    static final ArrayList<IO> ios = new ArrayList<>();

    public static void deserializeWidgetIO() {
        Set<Input<?>> is = new HashSet<>();
        Map<Output.Id,Output<?>> os = APP.widgetManager.findAll(OPEN)
                     .filter(w -> w.controller != null)
                     .peek(w -> w.controller.getInputs().getInputs().forEach(is::add))
                     .flatMap(w -> w.controller.getOutputs().getOutputs().stream())
                     .collect(Collectors.toMap(i->i.id, i->i));
        IOLayer.all_inoutputs.forEach(io -> os.put(io.o.id, io.o));

        ios.forEach(io -> {
            if(io.widget.controller==null) return;
            Input i = io.widget.controller.getInputs().getInput(io.input_name);
            if(i==null) return;
            io.outputs_ids.stream().map(os::get).filter(ISNTØ).forEach(i::bind);
        });

        IOLayer.all_inoutputs.forEach(io -> is.remove(io.i));
        IOLayer.all_inoutputs.forEach(io -> os.remove(io.o.id));
        IOLayer.all_inputs.addAll(is);
        IOLayer.all_outputs.addAll(os.values());
    }

    // called when widget is loaded/closed (or rather, when inputs or outputs are created/removed)
    // we need to create i/o nodes and i/o connections
    private void updateIO() {
        // because widget inputs can be bound to other widget outputs, and because widgets can be
        // loaded passively (then its i/o doesnt exists yet), we need to update all widget i/os
        // because we dont know which bind to this widget
        IOLayer.all_inputs.addAll(controller.getInputs().getInputs());
        IOLayer.all_outputs.addAll(controller.getOutputs().getOutputs());
        deserializeWidgetIO();
    }

    // called when widget is added/removed/moved within the scenegraph - we need to redraw the
    // i/o connections
    private void updateIOLayout() {
        // because we call this before the widget is part of scenegraph, we delay execution
        // suffering from badly designed (recursive) widget loading again...
        runLater(() -> IOLayer.relayout());
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
    public enum LoadType {
        AUTOMATIC, MANUAL;
    }
}