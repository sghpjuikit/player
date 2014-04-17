
package Configuration;

import AudioPlayer.playback.PLAYBACK;
import GUI.GUI;
import PseudoObjects.TODO;
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import main.App;
import utilities.Log;
import utilities.Parser.Parser;

/**
 * @author uranium
 * 
 * Manages application's configurations.
 */
@TODO("find out what changed and apply new settings only for modules")
public final class ConfigManager {
    
    /**
     * Changes configuration to default and applies them and saves.
     * Equivalent to change(Configuration.getDefault());
     */
    public static void setToDefault() {
        change(Configuration.getDefault());
    }
    
    /**
     * Changes configuration to the specified one and applies them and saves.
     * @param new_config
     * @return true if configuration changed, otherwise false.
     */
    public static boolean change(Configuration new_config) {
        if (new_config.isCurrent()) {   // no change
            if (new_config.isDefault())
                Log.mess("Configuration is already default. No change.");
            else
                Log.mess("No change.");
            return false;
        } else {                        // change
            if (new_config.isDefault()) 
                Log.mess("Changing settings. All settings now have default values.");
            else
                Log.mess("Changing settings.");
            new_config.save();          // save
            apply(new_config);          // apply effects
            return true;
        }
    }
    
    /*
     * Attempts to apply new settings' effects, but it is not guaranteed this
     * will succeed. Certain settings might require application restart or all
     * kinds of application modules reinitialization. Also, upon application
     * initialization (during start up) these modules might be unavailable.
     */
    public static void apply(Configuration c) {
        c.getFields().stream().forEach(f -> c.applyField(f.name, Parser.toS(f.value)) );
        if (App.getInstance().isGuiInitialized()) {
            GUI.refresh();
            PLAYBACK.playcountMan.configureIncrementation();        // reinitialize playcount incrementer
        }
                
        ls.forEach(l->JIntellitype.getInstance().removeHotKeyListener(l));
        ls.clear();
//        lsb.clear();
        go = -1;
        can = false;
        c.getShortcuts().forEach(s->{
            JIntellitype.getInstance().unregisterHotKey(c.getShortcuts().indexOf(s));
            JIntellitype.getInstance().registerHotKey(c.getShortcuts().indexOf(s), s.getKeysAsString());
            ls.add((int i) -> {
//                System.out.println("BEHAVING "+ i);
                go = i;
                can = false;                
            });
//            lsb.add(false);
        });
        ls.forEach(l->JIntellitype.getInstance().addHotKeyListener(l));
    }
    
    static int go = -1;
    static boolean can = false;
    static final List<HotkeyListener> ls = new ArrayList<>();
//    static final List<Boolean> lsb = new ArrayList<>();
    static final Thread lst = new Thread(() -> {
        while(true) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                System.out.println("interrupted");
                continue;
            }
            
            if (go > 0) {
                if(!can)
                    can = true;
                else {System.out.println(go+" "+can);
                    final int i = go;
                    Platform.runLater(()->{
                        System.out.println("REALLY BEHAVING "+Configuration.getEmpty().getShortcuts().get(i).getName());
                        Configuration.getEmpty().getShortcuts().get(i).getAction().run();
                        go=-1;
                        can = false;
                    });
                    can = false;
                    go=-1;
                }
            }
        }
    });
    
    /** Loads settings and applies them. */
    public static Configuration loadConfiguration() {
        Configuration c = Configuration.getEmpty();
        c.load();       // load settings
        apply(c);       // take effects (if gui is not initialized some values 
                        // wont take change, it must be run twice(see App class))
        
        if(!lst.isAlive()) {
            lst.setDaemon(true);
            lst.start();
        }
        return c;
    }
    
    /** Saves current settings. */
    public static void saveConfiguration() {
        Configuration.getCurrent().save();
    }

}