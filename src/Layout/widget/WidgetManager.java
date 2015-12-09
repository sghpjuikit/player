
package Layout.widget;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.Node;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Layout.container.layout.LayoutManager;
import Layout.widget.controller.Controller;
import Layout.widget.feature.Feature;
import gui.objects.Window.stage.UiContext;
import main.App;
import util.File.FileUtil;
import util.SwitchException;

import static Layout.widget.WidgetManager.WidgetSource.*;

/**
 * Handles operations with Widgets.
 */
public final class WidgetManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("WindowManager.class");

    /** Collection of registered Widget Factories. Non null, unique.*/
    static final Map<String,WidgetFactory<?>> factories = new HashMap<>();

    public static <T extends Node & Controller> void initialize() {
        // register internal
        ClassIndex.getAnnotated(IsWidget.class).forEach(c -> new ClassWidgetFactory((Class<T>) c).register());
        new EmptyWidgetFactory().register();
        // register external
        registerExternalWidgetFactories();
    }

    /**
     * @return read only list of registered widget factories
     */
    public static Stream<WidgetFactory<?>> getFactories() {
        return factories.values().stream();
    }

    /**
     * Looks for widget factory in the list of registered factories. Searches
     * by name. If no factory is found it will be attempted to register a new
     * one (this means factories added and not registered at runtime will work)
     *
     * @param name
     * @return widget factory, null if not found
     */
    public static WidgetFactory getFactory(String name) {
        // get factory
        WidgetFactory wf = factories.get(name);

        // attempt to register new factory for the file (maybe it was added in
        // runtime)
        if(wf==null) {
            File f = new File(App.WIDGET_FOLDER(), name + File.separator + name + ".fxml");
            if(f.exists()) {
                try {
                    URL source = f.toURI().toURL();
                    new FXMLWidgetFactory(name, source).register();
                    LOGGER.info("registering " + name);
                } catch(MalformedURLException e) {
                    LOGGER.error("Error registering wirget: " + name, e);
                }
            }
        }

        // return factory or null
        return factories.get(name);
    }

    public static Stream<Feature> getFeatures() {
        return getFactories().flatMap(f -> f.getFeatures().stream()).distinct();
    }

/******************************************************************************/

    /**
     * remembers standalone widgets not part of any layout, mostly in popups
     * as for normal widgets - they can be obtained from layouts
     * Do not use.
     */
    public static final List<Widget> standaloneWidgets = new ArrayList();

    public static Stream<Widget> findAll(WidgetSource source) {
        switch(source) {
            case LAYOUT:
                return LayoutManager.getLayouts().flatMap(l->l.getAllWidgets());
            case STANDALONE:
            case NO_LAYOUT:
                return standaloneWidgets.stream();
            case NEW:
                return Stream.empty();
            case OPEN:
            case ANY:
                return Stream.concat(findAll(STANDALONE),findAll(LAYOUT));
            default: throw new SwitchException(source);
        }
    }

    /**
     * Equivalent to {@code find(filter, source, false);}
     * @see #find(java.util.function.Predicate, Layout.Widgets.WidgetManager.WidgetSource, boolean)
     */
    public static Optional<Widget> find(Predicate<WidgetInfo> filter, WidgetSource source) {
        return find(filter, source, false);
    }

    /**
     * Returns widget fulfilling condition.
     * <pre>
     * - any widget can be returned (if it fulfills the cond.), but:
     * - if there is preferred widget it will always be returned first
     * - if no widget available it will be attempted to be created
     * - preferred factory will be used first.
     * - if all methods fail, null is returned
     * <pre>
     * Created widgets are displayed in a popup
     *
     * @param filter filter
     * @param source strategy for widget lookup
     * @param ignore whether the widget will partake in application layout and be
     * available for future widget search query. Use true if you want to simply
     * use the widget as a custom graphics, use false to let it be part of layout.
     * Can not be changed later.
     * <p>
     * Note that using true will cause the returned widget (if any) be visible in
     * one way or another depending on strategy. So for example newly created
     * widget will be put into layout or will show in a popup. If this behavior is
     * not desired, use false.
     * @return optional of widget fulfilling condition or empty if not available
     */
    public static Optional<Widget> find(Predicate<WidgetInfo> filter, WidgetSource source, boolean ignore) {
        Widget out = null;

        // get preferred type
        String preferred = getFactories()
                .filter(filter::test)
                .filter(w -> !w.isIgnored())
                .filter(f -> f.isPreferred())
                .findAny().map(f->f.name).orElse("");

        // get viable widgets - widgets of the feature & of preferred type if any
        List<Widget> widgets = findAll(source)
                .filter(w -> filter.test(w.getInfo()))
                .filter(w -> !w.isIgnored())
                .filter(preferred.isEmpty() ? w->true : w->w.getInfo().name().equals(preferred))
                .collect(Collectors.toList());

        // get preferred widget or any if none preferred
        for(Widget w : widgets) {
            if(out==null) out = w;
            if (w.isPreffered()) {
                out = w;
                break;
            }
        }

        // if no active or layout widget available & new widgets allowed
        if (out == null && source.newWidgetsAllowed()) {
            // get factory
            WidgetFactory f = getFactories()
                   .filter(filter::test)
                   .filter(w -> !w.isIgnored())
                   .filter(preferred.isEmpty() ? w->true : w->w.name().equals(preferred))
                   .findAny().orElse(null);

            // open widget as standalone if found
            if (f!=null) {
                out = f.create();
                if(!ignore) {
                    standaloneWidgets.add(out);
                    UiContext.showFloating(out);
                }
            }
        }

        return Optional.ofNullable(out);
    }

    /**
     * Equivalent to: {@code
     * getWidget(w->w.hasFeature(feature), source).map(w->(F)w.getController())}
     */
    public static<F> Optional<F> find(Class<F> feature, WidgetSource source, boolean ignore) {
        return find(w->w.hasFeature(feature), source, ignore).map(w->(F)w.getController());
    }
    public static<F> Optional<F> find(Class<F> feature, WidgetSource source) {
        return find(w->w.hasFeature(feature), source).map(w->(F)w.getController());
    }
    public static<F> Optional<F> findExact(Class<? extends Controller> type, WidgetSource source) {
        return find(w->w.type().equals(type), source).map(w->(F)w.getController());
    }

    /** Equivalent to: {@code getWidget(type, source).ifPresent(action)} */
    public static<F> void use(Class<F> type, WidgetSource source, Consumer<F> action) {
        find(type, source).ifPresent(action);
    }

    /** Equivalent to: {@code getWidget(cond, source).ifPresent(action)} */
    public static void use(Predicate<WidgetInfo> cond, WidgetSource source, Consumer<Widget> action) {
        find(cond, source).ifPresent(action);
    }

    /**
     * Denotes source for widgets. Used when looking up a widget. Sometimes it
     * is desirable to limit the source.
     */
    public static enum WidgetSource {

        /**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         *//**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         *//**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         *//**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         */
        LAYOUT,

        /**
         * Source is all currently active standalone widgets - widgets not part
         * of the layout, such as in popups.
         */
        STANDALONE,

        /**
         * Union of {@link #LAYOUT} and {@link #STANDALONE}.
         * <p>
         * This is the recommended source when creating widget is not intended.
         */
        OPEN,

        /**
         * The source is all available widget factories. In other words new
         * widget will always be created if possible.
         */
        NEW,

        /**
         * Union of {@link #NEW}, {@link #STANDALONE}.
         * <p>
         * This is the recommended source when it is expected to call the widget
         * multiple times and layout is not to be included,
         * because it creates new widget, but reuses standalone ones.
         */
        NO_LAYOUT,

        /**
         * Union of {@link #LAYOUT}, {@link #STANDALONE} and {@link #NO_LAYOUT}
         * Most complete source.
         */
        ANY;

        public boolean newWidgetsAllowed() {
            return this==NO_LAYOUT || this==ANY || this==NEW;
        }
    }

    public static enum Widget_Target {
        LAYOUT,
        TAB,
        POPUP,
        WINDOW;
    }


/******************************************************************************/

    /**
     * Registers external widget factories.
     * Searches for .fxml files in widget folder and registers them as widget
     * factories.
     */
    private static void registerExternalWidgetFactories() {
        // get folder
        File dir = App.WIDGET_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            LOGGER.error("External widgets registration failed.");
            return;
        }
        // get .fxml files
        try {
            Files.find(dir.toPath(), 2, (path,u) -> path.toString().endsWith(".fxml"))
                 .forEach(f -> registerFactory(FileUtil.getName(f.toUri())));
        } catch(IOException e) {
            LOGGER.error("Error during looking for widgets. Some widgets might not be available.", e);
        }
    }

    private static void registerFactory(String name) {
        // avoid registering twice
        if(factories.get(name) != null) return;

        try {
            File f = new File(App.WIDGET_FOLDER(), name + File.separator + name + ".fxml");
            URL source = f.toURI().toURL();
            new FXMLWidgetFactory(name, source).register();
            LOGGER.info("registering " + name);
        } catch(MalformedURLException e) {
            LOGGER.error("Error registering widget: " + name, e);
        }
    }
}