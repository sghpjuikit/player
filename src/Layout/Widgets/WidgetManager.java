
package Layout.Widgets;

import GUI.ContextManager;
import Layout.LayoutManager;
import Layout.WidgetImpl.Configurator;
import Layout.WidgetImpl.HtmlEditor;
import Layout.WidgetImpl.Spectrumator;
import Layout.WidgetImpl.Visualisation;
import Layout.Widgets.Features.Feature;
import static Layout.Widgets.WidgetManager.WidgetSource.ANY;
import static Layout.Widgets.WidgetManager.WidgetSource.LAYOUT;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import static Layout.Widgets.WidgetManager.WidgetSource.STANDALONE;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import main.App;
import util.Log;
import util.Parser.File.FileUtil;

/**
 * Handles operations with Widgets.
 * <p>
 */
public final class WidgetManager {
    /** Collection of registered Widget Factories. Non null, unique.*/
    static final Map<String,WidgetFactory> factories = new HashMap<>();
    
    public static void initialize() {
        registerInternalWidgetFactories();
        registerExternalWidgetFactories();
    }
    
    /**
     * @return read only list of registered widget factories
     */
    public static Stream<WidgetFactory> getFactories() {
        return factories.values().stream();
    }
    
    /**
     * Looks for widget factory in the list of registered factories. Searches
     * by name.
     * @param name
     * @return widget factory, null if not found
     */
    public static WidgetFactory getFactory(String name) {
        // get factory
        WidgetFactory wf = factories.get(name);
        
        // attempt to register new factory for the file
        if(wf==null) {
            try {
                File f = new File(App.WIDGET_FOLDER(), name + File.separator + name + ".fxml");
                URL source = f.toURI().toURL();
                new FXMLWidgetFactory(name, source).register();
                Log.deb("registering " + name);
            } catch(MalformedURLException e) {
                Log.err("Error registering wirget: " + name);
            }
        }
        
        // return factory or null
        return factories.get(name);
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
            case NOLAYOUT:
                return standaloneWidgets.stream();
            case NEW:
                return Stream.empty();
            case ACTIVE:
            case ANY:
                return Stream.concat(findAll(STANDALONE),findAll(LAYOUT));
            default: throw new AssertionError(source + " in default switch value.");
        }
    }
    
    /**
     * Returns widged fulfilling condition.
     * <pre>
     * - any widget can be returned (if it fulfills the cond.), but:
     * - if there is preferred widget it will always be returned first
     * - if no widget available it will be attempted to be created
     * - preferred factory will be used first.
     * - if all methods fail, null is returned
     * <pre>
     * Created widgets are displayed in a popup
     * 
     * @param cond
     * @return optional of widget fulfilling condition or empty if not available
     */
    public static Optional<Widget> find(Predicate<WidgetInfo> cond, WidgetSource source) {
        Widget out = null;
        
        // get preferred type
        String preferred = getFactories()
                .filter(cond::test)
                .filter(w -> !w.isIgnored())
                .filter(f->f.isPreferred())
                .findAny().map(f->f.name).orElse("");
        
        // get viable widgets - widgets of the feature & of preferred type if any
        List<Widget> widgets = findAll(source)
                .filter(cond::test)
                .filter(w -> !w.isIgnored())
                .filter(preferred.isEmpty() ? w->true : w->w.name().equals(preferred))
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
                   .filter(cond::test)
                   .filter(w -> !w.isIgnored())
                   .filter(preferred.isEmpty() ? w->true : w->w.name().equals(preferred))
                   .findAny().orElse(null);
           
            // open widget as standalone if found
            if (f!=null) {
                out = f.create();
                standaloneWidgets.add(out);
                ContextManager.showFloating(out);
            }
        }
        
        return Optional.ofNullable(out);
    }
    
    /**
     * Equivalent to: {@code 
     * getWidget(w->w.hasFeature(feature), source).map(w->(F)w.getController())}
     */
    public static<F extends Feature> Optional<F> find(Class<F> feature, WidgetSource source) {
        return WidgetManager.find(w->w.hasFeature(feature), source).map(w->(F)w.getController());
    }
    
    /** Equivalent to: {@code getWidget(type, source).ifPresent(action)} */
    public static<F extends Feature> void use(Class<F> type, WidgetSource source, Consumer<F> action) {
        find(type, source).ifPresent(action);
    }
    
    /** Equivalent to: {@code getWidget(cond, source).ifPresent(action)} */
    public static void use(Predicate<WidgetInfo> cond, WidgetSource source, Consumer<Widget> action) {
        WidgetManager.find(cond, source).ifPresent(action);
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
        ACTIVE,
        
        /**
         * The source is all available widget factories. In other words new
         * widget will always be created if possible.
         */
        NEW,
        
        /**
         * Union of {@link #NOLAYOUT}, {@link #STANDALONE}. 
         * <p>
         * This is the recommended source when it is expected to call the widget
         * multiple times and layout is not to be included,
         * because it creates new widget, but reuses standalone ones.
         */
        NOLAYOUT,
        
        /**
         * Union of {@link #LAYOUT}, {@link #STANDALONE} and {@link #NOLAYOUT}
         * Most complete source.
         */
        ANY;
        
        public boolean newWidgetsAllowed() {
            return this==NOLAYOUT || this==ANY || this==NEW;
        }
    }
    
    public static enum Widget_Target {
        LAYOUT,
        TAB,
        POPUP,
        WINDOW;
    }
    
    
/******************************************************************************/
    
    
    /** Registers internal widget factories. */
    private static void registerInternalWidgetFactories() {
        new ClassWidgetFactory("Settings", Configurator.class).register();
        //new ClassWidgetFactory("Playlist Manager", PlaylistManagerComponent.class).register();
        new ClassWidgetFactory("Circles", Visualisation.class).register();
        //new ClassWidgetFactory("Graphs", Graphs.class).register();
        new ClassWidgetFactory("HTMLEditor", HtmlEditor.class).register();
        new ClassWidgetFactory("Spectrumator", Spectrumator.class).register();
        new EmptyWidgetFactory().register();
    }
    /**
     * Registers external widget factories.
     * Searches for .fxml files in widget folder and registers them as widget
     * factories.
     */
    private static void registerExternalWidgetFactories() {
        Log.deb("Searching for external widgets.");
        // get folder
        File dir = App.WIDGET_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("External widgets registration failed.");
            return;
        }
        // get .fxml files
        try {
            Files.find(dir.toPath(), 2, (path,u) -> path.toString().endsWith(".fxml"))
                    .forEach(p-> registerFactory(FileUtil.getName(p.toUri())));
        } catch(IOException e) {
            Log.err("Error during looking for widgets. Some widgets might not be available.");
        }
    }
    
    private static void registerFactory(String name) {
        // avoid registering twice
        if(factories.get(name) != null) return;
        
        try {
            File f = new File(App.WIDGET_FOLDER(), name + File.separator + name + ".fxml");
            URL source = f.toURI().toURL();
            new FXMLWidgetFactory(name, source).register();
            Log.deb("registering " + name);
        } catch(MalformedURLException e) {
            Log.err("Error registering wirget: " + name);
        }
    }
}