
package GUI;

import Action.IsAction;
import Action.IsActionable;
import Configuration.AppliesConfig;
import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Configuration.SkinEnum;
import GUI.objects.Pickers.MoodPicker;
import Layout.Layout;
import Layout.LayoutManager;
import java.io.File;
import static java.io.File.separator;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import static javafx.application.Application.STYLESHEET_CASPIAN;
import static javafx.application.Application.STYLESHEET_MODENA;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import javafx.scene.text.FontWeight;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import main.App;
import utilities.FileUtil;
import utilities.Log;

/**
 *
 * @author uranium
 */
@IsActionable
@IsConfigurable
public class GUI implements Configurable {
    
    // properties
    @IsConfig(name = "Font", info = "Application font.")
    public static Font font = Font.getDefault();
    @IsConfig(name = "Skin", info = "Application skin.")
    public static SkinEnum skin = new SkinEnum("Default");
    
    @IsConfig(name = "Layout mode blur", info = "Layout mode use blur effect.")
    public static boolean blur_layoutMode = false;
    @IsConfig(name = "Layout mode fade", info = "Layout mode use fade effect.")
    public static boolean opacity_layoutMode = false;
    @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.", min=0.0, max=1.0)
    public static double opacity_LM = 0.2;
    @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur efect intensity.", min=0.0, max=20.0)
    public static double blur_LM = 8;
    @IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
    public static double duration_LM = 250;
    

    @IsConfig(name = "Snapping", info = "Allows snapping feature for windows and controls.")
    public static boolean snapping = true;
    @IsConfig(name = "Snap distance", info = "Distance at which snap feature gets activated")
    public static double snapDistance = 7;
    @IsConfig(name = "Tab auto align", info = "Always alignes tabs after tab dragging so only one is on screen.")
    public static boolean align_tabs = true;
    @IsConfig(name = "Tab switch min distance", info = "Required length of drag at"
            + " which tab switch animation gets activated. Tab switch activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    public static double dragDistance = 350;
    @IsConfig(name = "Tab switch min fraction", info = "Required length of drag as"
            + " a fraction of window at which tab switch animation gets activated."
            + " Tab switch activates if at least one condition is fulfilled min "
            + "distance or min fraction.", min=0.0, max=1.0)
    public static double dragFraction = 350;
    
//    @IsConfig(info = "Preffered delay for tooltip to show up.")
//    public static double tooltipDelay = 800;
    
    // other
    public static boolean alt_state = false;
    
    // 'singleton' objects and controls for use within app
    public static final MoodPicker MOOD_PICKER = new MoodPicker();
    
/******************************************************************************/
    
    public static void initialize(){
        findSkins();
    }
    
    public static void setLayoutMode(boolean val) {
        if (!val) {
            LayoutManager.active.values().forEach(Layout::hide);
            ContextManager.windows.forEach(w-> {
                if(w.getLayout()!=null) w.getLayout().hide();
            });
            alt_state = false;
        } else {
            LayoutManager.active.values().forEach(Layout::show);
            ContextManager.windows.forEach(w-> {
                if(w.getLayout()!=null) w.getLayout().show();
            });
            alt_state = true;
        }
    }
    
    public static boolean isLayoutMode() {
        return alt_state;
    }
    /** Loads/refreshes whole gui. */
    @IsAction(name = "Reload GUI.", description = "Reload application GUI.", shortcut = "F5")
    public static void refresh() {
        if (App.isInitialized()) {
            applySkin();
            applyFont();
            loadLayout();
            applyAlignTabs();
        }
    }
    
    /** Loads/refreshes active layout. */
    @IsAction(name = "Reload layout", description = "Reload layout.", shortcut = "F6")
    public static void loadLayout() {
        LayoutManager.getLayouts().forEach(l-> {
//            l.close();
            l.load();});
    }
    
    /** Toggles layout controlling mode. */
    @IsAction(name = "Manage Layout", description = "Enables layout managment mode.", shortcut = "F7")
    public static void toggleLayoutMode() {
        setLayoutMode(!alt_state);
    }
    
    @IsAction(name = "Show/Hide application", description = "Equal to switching minimized mode.", shortcut = "CTRL+ALT+W", global = true)
    @IsAction(name = "Minimize", description = "Switch minimized mode.", shortcut = "F9")
    public static void toggleMinimize() {
        if (ContextManager.activeWindow != null)
            ContextManager.activeWindow.toggleMinimize();
    }
    
    @IsAction(name = "Maximize", description = "Switch maximized mode.", shortcut = "F10")
    public static void toggleMaximize() {
        if (ContextManager.activeWindow != null)
            ContextManager.activeWindow.toggleMaximize();
    }
    
    @IsAction(name = "Fullscreen", description = "Switch fullscreen mode.", shortcut = "F11")
    public static void toggleFullscreen() {
        if (ContextManager.activeWindow != null)
            ContextManager.activeWindow.toggleFullscreen();
    }

    
    /**
     * Searches for .css files in skin folder and registers them as available
     * skins. Use on app start or to discover newly added layouts dynamically.
     */
    public static void findSkins() {
        // get + verify path
        File dir = App.SKIN_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Search for skins failed.");
            return;
        }
        // find skin directories
        File[] dirs;
        dirs = dir.listFiles(f -> FileUtil.isValidDirectory(f));
        // find skins
        List<File> files = new ArrayList<>();
        for (File d: dirs) {
            File[] tmp;
            tmp = d.listFiles(f -> FileUtil.isValidSkinFile(f));
            files.add(tmp[0]);
        }
        // populate skins
        skin.removeAll();
        if (files.isEmpty())
            Log.mess("Skin folder '" + dir.getPath() + "' is empty. No valid skins found.");
        else
            Log.mess(files.size() + " valid skins found.");
        
        for (File f : files) {
            String name = FileUtil.getName(f);
            skin.add(name);
            Log.mess("Skin " + name + " registered.");
        }
        
        skin.add(STYLESHEET_CASPIAN);
        skin.add(STYLESHEET_MODENA);
    }
    
    public static List<String> getSkins() {
       return skin.values();
    }
    
    

    
/*****************************  setter methods ********************************/
    
    /**
     * Applies specified font on the application. The font can be overridden 
     * locally.
     * The method executes only if the GUI is fully initialized, otherwise does
     * nothing.
     * @param font
     */
    public static void setFont(Font _font) {
        font = _font;
        applyFont();
    }
    
    /**
     * Changes application's skin and applies it.
     * This is a convenience method that constructs a file from the skinname
     * and calls the setSkin(File skin) method.
     * The skin file will be constructed based on the fact that it must pass
     * the isValidSkinFile() check. The path is constructed like this:
     * Application.SkinsPath/skinname/skinname.css
     * For any details regarding the mechanics behind the method see documentation
     * of that method.
     * @param skinname name of the skin to apply.
     */
    public static void setSkin(String skinname) {
        if (skinname == null || skinname.isEmpty() || skinname.equalsIgnoreCase(STYLESHEET_MODENA)) {
            setSkinModena();
        } else if (skinname.equalsIgnoreCase(STYLESHEET_CASPIAN)) {
            setSkinCaspian();
        }
        String path = App.SKIN_FOLDER().getPath() + separator + skinname + separator + skinname + ".css";
        File skin_file = new File(path);
        setSkinExternal(skin_file);
    }
    /**
     * Changes application's skin and applies it.
     * @param skin - css file of the skin to load. It is expected that the skin
     * will have all its external resources ready and usable or it could fail
     * to load properly.
     * To avoid exceptions and errors, isValidSkinFile() check  is ran on this
     * parameter before running this method.
     * @return true if the skin has been applied.
     * False return value signifies that gui has not been initialized,
     * skin file could be loaded or was not valid skin file.
     * True return value doesnt imply successful skin loading, but guarantees
     * that the new skin has been applied regardless of the success.
     */
    private static boolean setSkinExternal(File skin) {
        if (App.getWindowOwner().isInitialized() && FileUtil.isValidSkinFile(skin)) {
            try {
                String url = skin.toURI().toURL().toExternalForm();
                // force refresh skin if already set
                if (GUI.skin.get().equals(FileUtil.getName(skin))) {
                    Application.setUserAgentStylesheet(null);
                    Application.setUserAgentStylesheet(url);
                // set skin
                } else {
                    Application.setUserAgentStylesheet(url);
                    GUI.skin = new SkinEnum(FileUtil.getName(skin));
                }
                return true;
            } catch (MalformedURLException ex) {
                Log.err(ex.getMessage());
                return false;
            }
        }
        return false;
    }
    private static boolean setSkinModena() {
        if (App.getWindowOwner().isInitialized()) {
            Application.setUserAgentStylesheet(STYLESHEET_MODENA);
            GUI.skin = new SkinEnum("Modena");
            return true;
        }
        return false;
    }
    private static boolean setSkinCaspian() {
        if (App.getWindowOwner().isInitialized()) {
            Application.setUserAgentStylesheet(STYLESHEET_CASPIAN);
            GUI.skin = new SkinEnum("Caspian");
            return true;
        }
        return false;
    }
    
/****************************  applying methods *******************************/
    
    @AppliesConfig(config = "skin")
    private static void applySkin() {System.out.println("RUNNING SKIN "+skin);
        setSkin(skin.get());
    }
    
    @AppliesConfig(config = "font")
    private static void applyFont() {
        // apply only if application initialized correctly
        if (App.isInitialized()) {
            // we need to apply to each window separately
            ContextManager.windows.forEach( w ->{
                String tmp = font.getStyle().toLowerCase();
                FontPosture style = tmp.contains("italic") ? ITALIC : REGULAR;
                FontWeight weight = tmp.contains("bold") ? BOLD : NORMAL;
                // for some reason javaFX and css values are quite different...
                String styleS = style==ITALIC ? "italic" : "normal";
                String weightS = weight==BOLD ? "bold" : "normal";
                w.getStage().getScene().getRoot().setStyle(
                    "-fx-font-family: \"" + font.getFamily() + "\";" + 
                    "-fx-font-style: " + styleS + ";" + 
                    "-fx-font-weight: " + weightS + ";" + 
                    "-fx-font-size: " + font.getSize() + ";"
                );
            });
        }
    }
    
    @AppliesConfig(config = "align_tabs")
    private static void applyAlignTabs() {
        if(align_tabs) ContextManager.gui.alignTabs();
    }
}
