
package Configuration;

/**
 * Manages application's configurations.
 *
 * @author uranium
 */
public final class ConfigManager {
        
    /*
     * Attempts to apply new settings' effects, but it is not guaranteed this
     * will succeed. Certain settings might require application restart or all
     * kinds of application modules reinitialization. Also, upon application
     * initialization (during start up) these modules might be unavailable.
     */
    public static void apply(Configuration c) {
        c.getFields().stream().forEach(c::applyF);
//        if (App.getWindow()!= null && App.getWindow().isInitialized()) {
//            GUI.refresh();
//            PLAYBACK.playcountMan.configureIncrementation();        // reinitialize playcount incrementer
//        }
    }
    
    /** Loads settings and applies them. */
    public static Configuration loadConfiguration() {
        Configuration c = Configuration.getEmpty();
        c.load();       // load settings
        apply(c);       // take effects (if gui is not initialized some values 
                        // wont take change, it must be run twice(see App class))
        return c;
    }
    
}