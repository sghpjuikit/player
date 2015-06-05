
package Layout;

import Configuration.Configurable;
import gui.objects.Window.stage.ContextManager;
import gui.objects.Window.stage.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import main.App;
import util.File.FileUtil;
import util.dev.Log;

/**
 * @author uranium
 *
 */
public final class LayoutManager implements Configurable {

    private static final List<String> layouts = new ArrayList();
    
    
    public static Layout getActive() {
        // get focused window (not active - that is an illegal state in this
        // context. User should always be able to determine which layout is
        // active by seeing the focused window. If none is focused no layout
        // should be active as application is either not focused or in an
        // illegal state itself.
        Window aw = Window.getFocused();
        // get active layout from focused window
        return aw==null ? null : aw.getLayoutAggregator().getActive();
    }
    
    /**
     * @return all active Layouts in the application.
     */
    public static Stream<Layout> getLayouts() {
        // get all windows and fetch their layouts
        return Window.windows.stream()
                    .map(w->w.getLayoutAggregator())
                    .flatMap(la->la.getLayouts().values().stream());
    }
    
    /**
     * Return all names of all layouts available to the application, including
     * serialized layouts in files.
     * @return 
     */
    public static Stream<String> getAllLayoutsNames() {
        findLayouts();
        // get all windows and fetch their layouts
        return Stream.concat(getLayouts().map(Layout::getName), layouts.stream()).distinct();
        
    }
    
    /**
     * Searches for .l files in layout folder and registers them as available
     * layouts. Use on app start or to discover newly added layouts.
     */
    public static void findLayouts() {
        // get + verify path
        File dir = App.LAYOUT_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Search for Layouts failed.");
            return;
        }
        // find layout files
        File[] files;
        files = dir.listFiles((File pathname) -> pathname.getName().endsWith(".l"));
        // load layouts
        layouts.clear();
        if (files.length == 0) {
            Log.info("Layout folder '" + dir.getPath() + "' is empty. An empty layout will be created.");
            return;
        }
        for (File f : files) {
            layouts.add(FileUtil.getName(f));
        }
    }
    
    /** Loads specified layout as active. */
    public static void changeActiveLayout(Layout l) {
        // get active layouot
        Layout al = getActive();
        // change layout
        if (al!=null) al.setChild(l.getChild());
    }
}