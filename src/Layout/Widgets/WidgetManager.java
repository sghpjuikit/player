
package Layout.Widgets;

import GUI.ContextManager;
import GUI.objects.PopOver.PopOver;
import Layout.Layout;
import Layout.LayoutManager;
import Layout.WidgetImpl.Circles;
import Layout.WidgetImpl.ConfiguratorComponent;
import Layout.WidgetImpl.Graphs;
import Layout.WidgetImpl.HtmlEditor;
import Layout.WidgetImpl.PlaylistManagerComponent;
import Layout.WidgetImpl.Spectrumator;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget.Group;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javafx.scene.layout.AnchorPane;
import main.App;
import utilities.FileUtil;
import utilities.Log;

/**
 * Handles operations with Widgets.
 * <p>
 */
public final class WidgetManager {
    /** Collection of registered Widget Factories. Non null, unique.*/
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
        Log.deb("Searching for external widgets.");
        // get folder
        File dir = App.WIDGET_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("External widgets registration failed.");
            return;
        }
        // get .fxml files
        try {
            Files.find(dir.toPath(), 2, (t,u) -> t.toString().endsWith(".fxml")).forEach(p->{
                // dont change t.toString().endsWith(".fxml") to: t.endsWith(".fxml") - wont work
                // register
                File f = new File(p.toUri());
                String name = FileUtil.getName(f);
                try {
                    URL source = f.toURI().toURL();
                    new FXMLWidgetFactory(name, source).register();
                    Log.deb("registering " + name);
                }catch(MalformedURLException e) {
                    Log.err("Error registering wirget: " + name);
                }
            });
        } catch(IOException e) {
            Log.err("Error during looking for widgets. Some widgets might not be available.");
        }
    }
    
/******************************************************************************/
    
    // remembers standalone widgets not part of any layout, mostly in popups
    private static final List<Widget> standaloneWidgets = new ArrayList();
    
    /** @return stream of currently loaded widgets. */
    public static Stream<Widget> getWidgets() {
        return Stream.concat(standaloneWidgets.stream(),
            LayoutManager.getLayouts().flatMap(l->l.getAllWidgets().stream()));
    }

    /**
     * Returns widget based on condition. If there is preferred widget it will 
     * be returned first. Otherwise any widget can be returned from those that
     * fulfill condition.
     * @param cond widget predicate
     * @return widget testing true for predicate, null otherwise.
     */
    public static Widget getWidget(Predicate<Widget> cond) {
        Widget out = null;
        // attempt to get preferred widget from loaded widgets
        if (out == null)
            out = getWidgets()
                    .filter(w-> cond.test(w))
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
    public static Widget getWidgetOrCreate(Predicate<WidgetInfo> cond) {
        // get preferred widget from loaded widgets
        Widget out = getWidget(w->cond.test(w.getInfo()));
        
        // attempt to create new if no result yet
        if (out == null) {
            WidgetFactory f;
            // attempt to get preferred factory
                f = factories.stream()
                    .filter(w->cond.test(w.info))
                    .filter(w->w.preferred)
                    .findFirst().orElse(null);
            if (f==null)
            // attempt to get any factory
                f = factories.stream()
                    .filter(w->cond.test(w.info))
                    .findFirst().orElse(null);
            // open widget if found
            final Widget w = f==null ? null : f.create();
            if(w!=null) {
//                ContextManager.openFloatingWindow(w);
                standaloneWidgets.add(w);
                AnchorPane a = new AnchorPane();
                Layout l = new Layout();
                       l.setParentPane(a);
                       l.setChild(w);
                        
                PopOver p = new PopOver(l.load());
                        p.setAutoFix(false);
                        p.setArrowLocation(PopOver.ArrowLocation.RIGHT_CENTER);
                        p.show(App.getWindowOwner().getStage(), ContextManager.getX(), ContextManager.getY());
                        p.setOnHidden(e->standaloneWidgets.remove(w));
                return w;
            }
        }
        
        return out;
    }
    
    /**
     * @return any open widget supporting tagging or loaded new if none is loaded.
     * Null if no tagging widget available in application.
     */
    public static TaggingFeature getTaggerOrCreate() {
        return (TaggingFeature)getWidgetOrCreate(w->w.group()==Group.TAGGER).getController();
    }
}