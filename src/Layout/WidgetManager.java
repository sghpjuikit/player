
package Layout;

import Configuration.Configuration;
import GUI.Components.Circles;
import GUI.Components.ConfiguratorComponent;
import GUI.Components.Graphs;
import GUI.Components.HtmlEditor;
import GUI.Components.PlaylistManagerComponent;
import GUI.Components.Spectrumator;
import GUI.ContextManager;
import Layout.Widget.Group;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import utilities.FileUtil;
import utilities.Log;

/**
 * Handles operations with Widgets.
 * Note, that only pure Widgets are handled.
 * There are also Container objects subclassing Widget, but they are handled with
 * Layout Manager. This class handles only external and internal widgets with no
 * children (!= Container).
 * 
 */
public final class WidgetManager {
    // non null unique elements
    static final List<WidgetFactory> factories = new ArrayList<>();
    
    public static void initialize() {
        registerInternalWidgetFactories();
        registerExternalWidgetFactories();
    }
    
    /**
     * @return read only list of registered widget factories
     */
    public static List<WidgetFactory> getFactories() {
        return Collections.unmodifiableList(factories);
    }
    /**
     * Looks for widget factory in the list of registered factories. Searches
     * by name.
     * @param name
     * @return widget factory, null if not found
     */
    public static WidgetFactory getFactory(String name) {
        for (WidgetFactory f: factories)
            if (name.equals(f.name))
                return f;
        return null;
    }

    
    /** Registers internal widget factories. */
    private static void registerInternalWidgetFactories() {
        new ClassWidgetFactory("Settings", ConfiguratorComponent.class).register();
        new ClassWidgetFactory("Playlist Manager", PlaylistManagerComponent.class).register();
        new ClassWidgetFactory("Circles", Circles.class).register();
        new ClassWidgetFactory("Graphs", Graphs.class).register();
        new ClassWidgetFactory("HTMLEditor", HtmlEditor.class).register();
        new ClassWidgetFactory("Spectrumator", Spectrumator.class).register();
    }
    /**
     * Registers external widget factories.
     * Searches for .fxml files in widget folder and registers them as widget
     * factories.
     */
    private static void registerExternalWidgetFactories() {
        Log.deb("Searching for and registering external widgets.");
        // get folder
        File dir = new File(Configuration.WIDGET_FOLDER);
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("External widgets registration failed.");
            return;
        }
        // get .fxml files
        File[] files  = dir.listFiles((File pathname) -> pathname.getName().endsWith(".fxml"));
        // register
        for(File f: files) {
            String name = FileUtil.getName(f);
            new FXMLWidgetFactory(name, f.getPath()).register();System.out.println("registering " + name);
        }
    }
    
/******************************************************************************/
    
    /** @return stream of currently loaded widgets. */
    public static Stream<Widget> getWidgets() {
        return LayoutManager.getLayouts().flatMap(l->l.getAllWidgets().stream());
    }

    /**
     * Returns widged based on condition. If there is preferred widget it will 
     * be returned first. Otherwise any widget can be returned from those that
     * fulfill condition.
     * @param cond widget predicate
     * @return widget testing true for predicate, null otherwise.
     */
    public static Widget getWidget(Predicate<? super Widget> cond) {
        Widget out = null;
        // attempt to get preferred widget from loaded widgets
        if (out == null)
            out = getWidgets()
                    .filter(w->cond.test(w))
                    .filter(w-> w.isPreffered())
                    .findFirst().orElse(null);
        // attempt to get any widget from loaded widgets
        if (out == null)
            out = getWidgets()
                    .filter(w->cond.test(w))
                    .findFirst().orElse(null);
        return out;
    }
    
    /**
     * Returns widged based on condition. If there is preferred widget it will 
     * be returned first. Otherwise any widget can be returned from those that
     * fulfill condition.
     * If the widget isnt found it will be attempted to create it and display it
     * in a new window.
     * @param cond
     * @return widget fulfilling condition. Null if application has no access to
     * any widget fulfilling the condition.
     */
    public static Widget getWidgetOrCreate(Predicate<? super WidgetInfo> cond) {
        Widget out;
        // attempt to get preferred widget from loaded widgets
        out = getWidget(w->cond.test(w.getInfo()));
        // attempt to create new
        if (out == null) {
            WidgetFactory f;
            // attempt to get preferred factory
                f = factories.stream()
                    .filter(w->cond.test(w.info))
                    .filter(w->w.isPreffered())
                    .findFirst().orElse(null);
            if (f==null)
            // attempt to get any factory
                f = factories.stream()
                    .filter(w->cond.test(w.info))
                    .findFirst().orElse(null);
            // open widget if found
            out = f==null ? null : f.create();
            if(out!=null)
                ContextManager.openFloatingWindow(out);
        }
        return out;
    }
    
    /**
     * @return any open widget supporting tagging or loaded new if none is loaded.
     * Null if no tagging widget available in application.
     */
    public static SupportsTagging getTaggerOrCreate() {
        return (SupportsTagging)getWidgetOrCreate(w->w.group()==Group.TAGGER).getController();
    }
}