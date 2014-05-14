
package Layout;

import Configuration.Configurable;
import Configuration.Configuration;
import GUI.ContextManager;
import Serialization.Serializator;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import main.App;
import utilities.FileUtil;
import utilities.Log;
import utilities.functional.impl.NotNull;

/**
 * @author uranium
 *
 */
public final class LayoutManager implements Configurable {

    public static final List<String> layouts = new ArrayList<>();
    public static Map<Integer,Layout> active = new HashMap<>();
    
    
    public static Layout getActive() {
        return active.get(ContextManager.gui.currTab());
    }
    
    /**
     * @return all active Layouts in the application.
     */
    public static Stream<Layout> getLayouts() {
        Stream.Builder<Layout> lsb = Stream.builder();
        active.values().stream().forEach(lsb::add);
        ContextManager.windows.stream().map(w->w.getLayout()).forEach(lsb::add);
        return lsb.build().filter(new NotNull());
    }
    
    /**
     * Searches for .l files in layout folder and registers them as available
     * layouts. Use on app start or to discover newly added layouts.
     */
    public static void findLayouts() {
        // get + verify path
        File dir = new File(App.LAYOUT_FOLDER());
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
            Log.mess("Layout folder '" + App.LAYOUT_FOLDER() + "' is empty. An empty layout will be created.");
            return;
        }
        for (File f : files) {
            layouts.add(FileUtil.getName(f));
        }
    }

    /**
     * Loads layout marked as last used. 
     * Allows resuming layout from last session. If last used layout is
     * already being used, it reverts to its last saved state.
     * This method guarantees initialization of the layout. 
     * If layout fails to load, nothing happens, but in case of initialization,
     * new empty layout will be created and assigned as active.
     */
    public static void loadLast() {
        for (String l: layouts) {            
            if (l.equals(Configuration.last_layout))
                putLayout(0, l);
            if (l.equals(Configuration.right_layout))
                putLayout(1, l);
            if (l.equals(Configuration.left_layout))
                putLayout(-1, l);
        }
        active.putIfAbsent(0, new Layout("layout0"));
        active.keySet().forEach( i -> ContextManager.gui.addTab(i));
    }
    
    private static void putLayout(int i, String s) {
        Layout l = Serializator.deserializeLayout(new File(App.LAYOUT_FOLDER()+File.separator+s+".l"));
        if (l!=null) active.put(i, l);
    }
    
    /**
     * Takes preview/thumbnail/snapshot of the active layout and saves it as .png
     * under same name.
     */
    public static void makeSnapshot() {        
        active.values().forEach(Layout::makeSnapshot);
    } 
    
    /** Loads/refreshes active layout. */
    public static void loadActive() {
        active.entrySet().forEach( l -> {
            l.getValue().serialize();
            l.getValue().load();
            
            if(l.getKey()==0)
                Configuration.last_layout = l.getValue().getName();
            if(l.getKey()==1)
                Configuration.right_layout = l.getValue().getName();
             if(l.getKey()==-1)
                Configuration.left_layout = l.getValue().getName();
        });
    }
    
    /** Loads specified layout as active. */
    public static void changeActiveLayout(Layout l) {
        active.get(0).makeSnapshot();
        active.put(0, l);
        loadActive();
    }
    
    public static void serialize() {
        active.values().forEach(Layout::serialize);
    }
}