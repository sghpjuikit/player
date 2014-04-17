
package Configuration;

import utilities.functional.functor.Procedure;
import com.melloware.jintellitype.JIntellitype;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import main.App;
import utilities.Log;

/**
 * Encapsulates application behavior.
 * @author uranium
 */
public final class Action {
    
    private final String name;
    private final Procedure action;
    private final String info;
    private KeyCombination keys = KeyCombination.NO_MATCH;
    
    public Action(String name, Procedure action, String info) {
        this.name = name;
        this.action = action;
        this.info = info;
    }
    public Action(String name, Procedure action, String info, String keys) {
        this.name = name;
        this.action = action;
        this.info = info;
        setKeys(keys);
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @return the action */
    public Procedure getAction() {
        return action;
    }

    /** @return the id */
    public String getInfo() {
        return info;
    }
    
     /** @return the key combination */
    public KeyCombination getKeys() {
        return keys;
    }
    
    /** @return the key combination as string */
    public String getKeysAsString() {
        return keys.getName();
    }
    
    /** 
     * Change and apply key combination. After invoking this method, the shortcut
     * will be always registered.
     */
    public void changeKeys(String shortcut) {// System.out.println("changing shortcut from "+keys.toString() + " to " + shortcut);
        unregister();
        setKeys(shortcut);
        register();
    }
    
    private void setKeys(String keys) {
        if(keys.isEmpty()) keys = KeyCombination.NO_MATCH.toString();
        try {
            this.keys = KeyCombination.keyCombination(keys);
        } catch (Exception ex) {
            Log.mess("Illegal shortcut keys parameter. Shortcut keys disabled for: " + name + " Keys: '" + keys + "'");
            this.keys = KeyCombination.NO_MATCH;
        }
    }
    
    /**
     * Registers shortcut.
     * Only registered shortcuts work.
     * Method will succeed only if GUI is already initialized.
     */
    private void register() {
        if (!Configuration.global_shortcuts || !JIntellitype.isJIntellitypeSupported())
//            
//        else
            registerInApp();
    }
    private void unregister() {
        if (!Configuration.global_shortcuts || !JIntellitype.isJIntellitypeSupported())
//            
//        else
            unregisterInApp();
    }
    
    private void registerInApp() {
        if (App.getInstance().isGuiInitialized()) {
            //System.out.println("registering in app shortcut "+name);
            Scene scene = App.getInstance().getScene();
                  scene.getAccelerators().put(keys, () -> action.run());
        }
    }
    private void unregisterInApp() {
        if (App.getInstance().isGuiInitialized()) {
            //System.out.println("unregistering in app shortcut "+name);
            Scene scene = App.getInstance().getScene();
                  scene.getAccelerators().remove(keys);
        }
    }
}
