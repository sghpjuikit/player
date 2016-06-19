package layout.widget;

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

import layout.Component;
import layout.area.ContainerNode;
import layout.area.IOLayer;
import layout.container.Container;
import layout.widget.controller.Controller;
import layout.widget.controller.io.Input;
import layout.widget.controller.io.IsInput;
import layout.widget.controller.io.Output;
import util.access.V;
import util.conf.CachedCompositeConfigurable;
import util.conf.Config;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.dev.Dependency;
import util.type.Util;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static layout.widget.WidgetManager.WidgetSource.OPEN;
import static main.App.APP;
import static util.async.Async.runLater;
import static util.file.Util.writeFile;
import static util.functional.Util.*;

/**
 * Widget graphical component with a functionality.
 * <p/>
 * The functionality is handled by widget's {@link Controller}. The controller
 * is instantiated when widget loads. The widget-controller relationship is 1:1
 * and permanent.
 * <p/>
 * Widget can be thought of as a wrapper for controller (which may be used as
 * standalone object if implementation allows). The type of widget influences
 * the lifecycle.
 *
 * @author Martin Polakovic
 */
public class Widget<C extends Controller<?>> extends Component implements CachedCompositeConfigurable<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);
    private static final Set<String> ignoredConfigs = set("Is preferred","Is ignored","Custom name"); // avoids data duplication

    // Name of the widget. Permanent. Same as factory name. Used solely for deserialization (to find
    // appropriate factory)
    // TODO: put inside propertymap
    private final String name;

    /**
     * Factory that produced this widget.
     * <p/>
     * Note that in case the application creates another version of the factory (e.g., when the
     * widget source code has been modified and recompiled in runtime), even though the old factory
     * will be removed from the list of factories (substituted by the new factory), this field will
     * still point to the old factory, holding true to the description of this field.
     */
    @Dependency("name - acceesed using reflection by name")
    @XStreamOmitField public final WidgetFactory<?> factory;
    @XStreamOmitField protected Node root;
    @XStreamOmitField protected C controller;

    @XStreamOmitField private HashMap<String,Config<Object>> configs = new HashMap<>();

    // Temporary workaround for bad design. Widget-Container-Controller-Area relationship is badly
    // designed. This particular problem: Area can contain not yet loaded widget. Thus, we cant
    // use controller (null) to obtain area.
    //
    // I think this is the best and most painless way to wire widget with area & container (parent)
    // All the pseudo wiring through Controller is pure chaos.
    @XStreamOmitField @Deprecated public Container parentTemp;
    /**
     *  Graphics this widget is loaded in. It is responsibility of the caller of the {@link #load()} to set this
     *  field properly. There is no restriction where widget is loaded, so this field may be null.
     *  <p/>
     *  This field allows widget to control its lifecycle and context from its controller.
     */
    @XStreamOmitField public ContainerNode areaTemp;

    // configuration

    /** Whether this widget will be preferred over other widgets in widget lookup. */
    @IsConfig(name = "Is preferred", info = "Prefer this widget among all widgets of its type. If there is a request "
            + "for widget, preferred one will be selected. ")
    public final V<Boolean> preferred = new V<>(false);

    /** Whether whether this widget will not be included in widget lookup. */
    @IsConfig(name = "Is ignored", info = "Ignore this widget if there is a request.")
    public final V<Boolean> forbid_use = new V<>(false);

    /** Name displayed in gui. User can set his own. By default component type name. */
    @IsConfig(name = "Custom name", info = "Name displayed in gui. User can set his own. By default component type name.")
    public final V<String> custom_name = new V<>("");


    /**
     * @param {@link Widget#name}
     */
    public Widget(String name) {
        this.name = name;
        this.factory = APP.widgetManager.factories.get(name);
        custom_name.setValue(name);
    }

    public Widget(String name, WidgetFactory factory) {
        this.name = name;
        this.factory = factory;
        custom_name.setValue(name);
    }

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
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public Container<?> getParent() {
        return parentTemp;
    }

    /**
     * Returns this widget's content.
     * <p/>
     * If called the 1st time, loads this widget's content and instantiates its
     * controller. The controller will be null until the first time this method
     * is called.
     * Any subsequent call of this method will simply return the content.
     * <p/>
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
            Util.getField(c.getClass(), "widget"); // we use this as a check, throws Exception on fail
            Util.setField(c, "widget", this); // executes only if the field exists
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
     * <p/>
     * The controller is instantiated when widget loads. The controller is null
     * before that and this method should not be invoked.
     * <p/>
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
            // We set controller null (before closing it). Makes sure that:
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

        // Not the best handling, but at least dev does not have to do this manually and concern
        // himself
        APP.widgetManager.standaloneWidgets.remove(this);
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

        Util.setField(this, "id", w.id); // (nasty cheat) not sure if this 100% required
        preferred.setVof(w.preferred);
        forbid_use.setVof(w.forbid_use);
        custom_name.setVof(w.custom_name);
        loadType.set(w.loadType.get());
        locked.set(w.locked.get());

        // if this widget is loaded we apply state, otherwise its done when it loads
        if(controller!=null)
            restoreConfigs();
    }

/******************************************************************************/


    /**
     * Returns whether this widget is empty.
     * <p/>
     * Empty widget has no graphics. {@link Controller#isEmpty()} indicates
     * state - that there is no data to display within widget's graphics.
     */
    public boolean isEmpty() {
        return this instanceof EmptyWidget;
    }

    /**
     * Returns singleton list containing the controller of this widget
     * <p/>
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

    @Override
    public String toString() {
        return getClass() + " " + name;
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

        // try to assign factory (it must exist) or fallback to empty eidget
        if(factory==null) Util.setField(this, "factory", APP.widgetManager.factories.get(name));
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
        // We store only Controller configs as configs of this widget should be defined in this
        // class as fields and serialized/deserialized automatically. Only Controller is created
        // manually, Widget configs should be independent and full auto...

        // 1) nothing to serialize
        // 2) serializing empty list could actually rewrite previously serialized properties,
        //    not yet restored due to widget not yet loaded
        // 3) easy optimization
        if(controller==null) return;

        @SuppressWarnings("unchecked")
        Map<String,String> serialized_configs = (Map) properties.computeIfAbsent("configs", key -> new HashMap<>()); // preserves existing configs
        controller.getFields().stream().forEach(c -> serialized_configs.put(c.getName(), c.getValueS()));
    }

    private void restoreConfigs() {
        @SuppressWarnings("unchecked")
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
        // loaded passively (then its i/o does not exists yet), we need to update all widget i/os
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
        runLater(IOLayer::relayout);
    }


    /** Widget metadata. Passed from code to program. Use on controller class. */
    @Retention(value = RUNTIME)
    @Target(value = TYPE)
    public @interface Info {

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
         * <p/>
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
        UNKNOWN
    }
    public enum LoadType {
        AUTOMATIC, MANUAL
    }
}