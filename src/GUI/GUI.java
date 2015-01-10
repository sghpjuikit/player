
package GUI;

import Action.IsAction;
import Action.IsActionable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.LayoutAggregators.LayoutAggregator;
import GUI.LayoutAggregators.SwitchPane;
import Layout.Layout;
import Layout.LayoutManager;
import com.sun.javafx.css.StyleManager;
import java.io.File;
import static java.io.File.separator;
import java.net.MalformedURLException;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import javafx.application.Application;
import static javafx.application.Application.STYLESHEET_CASPIAN;
import static javafx.application.Application.STYLESHEET_MODENA;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import javafx.scene.text.FontWeight;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import main.App;
import util.Log;
import util.Parser.File.FileUtil;
import static util.Util.capitalizeStrong;
import util.access.Accessor;
import util.access.AccessorEnum;

/**
 *
 * @author uranium
 */
@IsActionable
@IsConfigurable
public class GUI {
    
    private static final String DEF_SKIN = GUI.class.getResource("Skin/Skin.css").toExternalForm();
    private static String skinOldUrl = ""; // set to not sensible non null value
    private static boolean alt_state = false;
    private static final List<String> skins = new ArrayList();
    
    // applied configs
    @IsConfig(name = "Skin", info = "Application skin.")
    public static final AccessorEnum<String> skin = new AccessorEnum<>("Default", GUI::setSkin, GUI::getSkins);
    /**
     * Font of the application. Overrides font defined by skin. The font can be 
     * overridden programmatically or stylesheet.
     * 
     * Note: font is applied only if the GUI is fully initialized, otherwise does
     * nothing.
     */
    @IsConfig(name = "Font", info = "Application font.")
    public static final Accessor<Font> font = new Accessor<>(Font.getDefault(), GUI::applyFont);
    
    // non applied configs
    @IsConfig(name = "Layout mode blur bgr", info = "Layout mode use blur effect.")
    public static boolean blur_layoutMode = false;
    @IsConfig(name = "Layout mode fade bgr", info = "Layout mode use fade effect.")
    public static boolean opacity_layoutMode = true;
    @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.", min=0.0, max=1.0)
    public static double opacity_LM = 0.8;
    @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur efect intensity.", min=0.0, max=20.0)
    public static double blur_LM = 8;
    @IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
    public static double duration_LM = 250;
    @IsConfig(name = "Snap", info = "Allows snapping feature for windows and controls.")
    public static boolean snapping = true;
    @IsConfig(name = "Snap activation distance", info = "Distance at which snap feature gets activated")
    public static double snapDistance = 7;
    @IsConfig(name = "Lock layout", info = "Locked layout will not enter layout mode.")
    private final static BooleanProperty locked_layout = new SimpleBooleanProperty(false){
        @Override public void set(boolean v) {
            super.set(v);
            Action.Action.actionStream.push("Layout lock");
        }
    };
        
/******************************************************************************/
    
    public static void initialize(){
        findSkins();
    }
    
    /**
     * Component might rely on this method to alter its behavior. For example
     * it can leave layout mode on its own independently but forbid this
     * behavior if this method returns TRUE.
     * <p>
     * Note that this method consistently returns FALSE at the time
     * of entering and leaving the layout mode, thus allowing to safely query
     * layout mode state just before the state change. In other words the state
     * change itself will not influence the result, which changes only after the
     * change occurred. not after it was invoked.
     * <p>
     * Basically, at the time of invoking show() and hide() methods the flag
     * is (and must be) FALSE and never TRUE
     *
     * @return whether layout mode was on or not.
     */
    public static boolean isLayoutMode() {
        return alt_state;
    }
    
    public static BooleanProperty layoutLockedProperty() {
        return locked_layout;
    }
    
    /** Set lock to prevent layouting. */
    public static void setLayoutlocked(boolean val) {
        locked_layout.set(val);
    }
    
    /** Return lock to prevent layouting. */
    public static boolean isLayoutLocked() {
        return locked_layout.get();
    }
    
    @IsAction(name = "Toggle layout lock.", description = "Lock/unlock layout.", shortcut = "F4")
    /** Toggles lock to prevent layouting. */
    public static void toggleLayoutLocked() {
        locked_layout.set(!locked_layout.get());
    }
    
    /** Loads/refreshes whole gui. */
    @IsAction(name = "Reload GUI.", description = "Reload application GUI. Includes skin, font, layout.", shortcut = "F5")
    public static void refresh() {
        if (App.isInitialized()) {
            skin.applyValue();
            font.applyValue();
            loadLayout();
        }
    }
    
    /** Loads/refreshes active layout. */
    @IsAction(name = "Reload layout", description = "Reload layout.", shortcut = "F6")
    public static void loadLayout() {
        LayoutManager.getLayouts().forEach(Layout::load);
    }
    
    /** Toggles layout controlling mode. */
    @IsAction(name = "Reload skin", description = "Reloads skin.", shortcut = "F7")
    public static void loadSkin() {
        skin.applyValue();
    }
    
    /** 
     * Sets layout mode on/off.
     * <p>
     * Note that {@link #isLayoutMode()} consistently returns FALSE at the time
     * of entering and leaving the layout mode.
     * @see #isLayoutMode()
     */
    public static void setLayoutMode(boolean val) {
        // avoid pointless operation
        if(alt_state==val) return;
        // Note that we set the layout mode flag after invoking show() but
        // before invoking hide().
        // This is important to maintain consistency. See documentation.
        if (val) {
            LayoutManager.getLayouts().forEach(Layout::show);
            alt_state = val;
        } else {
            alt_state = val;
            LayoutManager.getLayouts().forEach(Layout::hide);
            setZoomMode(false);
        }
        if(val) Action.Action.actionStream.push("Layout mode");
    }
    
    public static void setZoomMode(boolean val) {
        Window w = Window.getFocused();
        if(w!=null) {
            SwitchPane l = SwitchPane.class.cast(w.getLayoutAggregator());
            if(l.isZoomedOut() && !val) l.zoomOut();
            if(!l.isZoomedOut() && val) {
                l.zoomIn();
                Action.Action.actionStream.push("Zoom mode");
            }
        }
    }
    
    /** Toggles layout mode. */
    @IsAction(name = "Manage Layout", description = "Toggles layout mode on/off.")
    public static void toggleLayoutMode() {
        setLayoutMode(!alt_state);
    }
    
    /** Toggles zoom mode. */
    @IsAction(name = "Zoom Layout", description = "Toggles layout zoom in/out.")
    public static void toggleZoomMode() {
        Window w = Window.getFocused();
        if(w!=null) {
            SwitchPane l = SwitchPane.class.cast(w.getLayoutAggregator());
            if(l.isZoomedOut()) l.zoomIn();
            else {
                l.zoomOut();
            }
        }
    }
    
    public static void setLayoutNzoom(boolean v) {
        setLayoutMode(v);
        setZoomMode(v);
    }
    
    @IsAction(name = "Manage Layout & Zoom", description = "Enables layout managment mode and zooms.", shortcut = "F8")
    public static void toggleLayoutNzoom() {
        toggleLayoutMode();
        toggleZoomMode();
    }
    
    @IsAction(name = "Show/Hide application", description = "Equal to switching minimized mode.", shortcut = "CTRL+ALT+W", global = true)
    @IsAction(name = "Minimize", description = "Switch minimized mode.", shortcut = "F10")
    public static void toggleMinimizeFocus() {
        if(!App.getWindowOwner().isMinimized() && Window.getFocused()==null)
            Window.getActive().focus();
        else
            Window.getActive().toggleMinimize();
    }
    
    public static void toggleMinimize() {
        Window.getActive().toggleMinimize();
    }
    
    @IsAction(name = "Maximize", description = "Switch maximized mode.", shortcut = "F11")
    public static void toggleMaximize() {
        Window.getActive().toggleMaximize();
    }
    
    @IsAction(name = "Loop maximized state", description = "Switch to different maximized window states.", shortcut = "F3")
    public static void toggleMaximizedState() {
        Window w = Window.getActive();
        w.setMaximized(w.isMaximized().next());
    }
    
    @IsAction(name = "Fullscreen", description = "Switch fullscreen mode.", shortcut = "F12")
    public static void toggleFullscreen() {
        Window.getActive().toggleFullscreen();
    }

    @IsAction(name = "Align tabs", description = "Aligns tabs to the window", shortcut = "SHIFT+UP")
    public static void tabAlign() {
        LayoutAggregator la = Window.getActive().getLayoutAggregator();
        if(la instanceof SwitchPane) SwitchPane.class.cast(la).alignTabs();
    }

    @IsAction(name = "Align to next tab", description = "Goes to next tab and aligns tabs to the window", shortcut = "SHIFT+RIGHT")
    public static void tabNext() {
        LayoutAggregator la = Window.getActive().getLayoutAggregator();
        if(la instanceof SwitchPane) SwitchPane.class.cast(la).alignRightTab();
    }

    @IsAction(name = "Align to previous tab", description = "Goes to previous tab and aligns tabs to the window", shortcut = "SHIFT+LEFT")
    public static void tabPrevious() {
        LayoutAggregator la = Window.getActive().getLayoutAggregator();
        if(la instanceof SwitchPane) SwitchPane.class.cast(la).alignLeftTab();
    }
    
    
    /**
     * Searches for .css files in skin folder and registers them as available
     * skins. Use on app start or to discover newly added layouts dynamically.
     */
    public static void findSkins() {
        // get + verify path
        File dir = App.SKIN_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Search for skins failed." + dir.getPath() + " could not be accessed.");
            return;
        }
        // find skin directories
        File[] dirs = dir.listFiles(File::isDirectory);
        // find & register skins
        Log.info("Registering external skins.");
        skins.clear();
        for (File d: dirs) {
            String name = d.getName();
            File css = new File(d, name + ".css");
            if(FileUtil.isValidFile(css)) {
                skins.add(name);
                Log.info("    Skin " + name + " registered.");
            }
        }
        
        if (skins.isEmpty())
            Log.info("No skins found.");
        else
            Log.info(skins.size() + " skins found.");
        
        Log.info("Registering internal skins.");
        skins.add(capitalizeStrong(STYLESHEET_CASPIAN));
        skins.add(capitalizeStrong(STYLESHEET_MODENA));
        Log.info("    Skin Modena registered.");
        Log.info("    Skin Caspian registered.");
    }
    
    public static List<String> getSkins() {
       return skins;
    }
    
/*****************************  setter methods ********************************/
    
    /**
     * Changes application's skin and applies it.
     * This is a convenience method that constructs a file from the skinname
     * and calls the setSkin(File skin) method.
     * The skin file will be constructed like this:
     * application location .../Skins/skinname/skinname.css
     * For any details regarding the mechanics behind the method see documentation
     * of that method.
     * @param s name of the skin to apply.
     */
    public static void setSkin(String s) {
        if (s == null || s.isEmpty()) throw new IllegalArgumentException();
        
        if (s.equalsIgnoreCase(STYLESHEET_MODENA)) {
            setSkinModena();
        } else if (s.equalsIgnoreCase(STYLESHEET_CASPIAN)) {
            setSkinCaspian();
        } else {
            File skin_file = new File(App.SKIN_FOLDER().getPath(), s + separator + s + ".css");
            setSkinExternal(skin_file);
        }
    }
    /**
     * Changes application's skin.
     * @param file - css file of the skin to load. It is expected that the skin
     * file and resources are available.
     * @return true if the skin has been applied.
     * False return value signifies that gui has not been initialized or that
     * skin file could be accessed.
     * True return value doesnt imply successful skin loading, but guarantees
     * that the new skin has been applied regardless of the success. There can
     * still be parsing errors resulting in imperfect skin application.
     */
    public static boolean setSkinExternal(File file) {
        if (App.getWindowOwner().isInitialized() && FileUtil.isValidSkinFile(file)) {
            try {
                String url = file.toURI().toURL().toExternalForm();
                // remove old skin
                StyleManager.getInstance().removeUserAgentStylesheet(skinOldUrl);
                // set core skin
                StyleManager.getInstance().setDefaultUserAgentStylesheet(DEF_SKIN);
                // add new skin
                StyleManager.getInstance().addUserAgentStylesheet(url);
                // set current skin
                skin.setValue(FileUtil.getName(file));
                // store its url so we can remove the skin later
                skinOldUrl = url;
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
            // remove old skin
            StyleManager.getInstance().removeUserAgentStylesheet(skinOldUrl);
            // set code skin
            Application.setUserAgentStylesheet(STYLESHEET_MODENA);
            skin.setValue("Modena");
            return true;
        }
        return false;
    }
    private static boolean setSkinCaspian() {
        if (App.getWindowOwner().isInitialized()) {                
            // remove old skin
            StyleManager.getInstance().removeUserAgentStylesheet(skinOldUrl);
            // set code skin
            Application.setUserAgentStylesheet(STYLESHEET_CASPIAN);
            skin.setValue("Caspian");
            return true;
        }
        return false;
    }
    
    public static List<File> getGuiImages() {
        File location = new File(App.SKIN_FOLDER(), skin + separator + "Images");
        if(FileUtil.isValidDirectory(location)) {
            return FileUtil.getImageFiles(location, -1);
        } else {
            Log.deb("Can not access skin directory: " + location.getPath());
            return EMPTY_LIST;
        }
    }
    
/*****************************  helper methods ********************************/
    
    private static void applyFont(Font f) {
        // apply only if application initialized correctly
        if (App.isInitialized()) {
            // we need to apply to each window separately
            Window.windows.forEach(w ->{
                String tmp = f.getStyle().toLowerCase();
                FontPosture style = tmp.contains("italic") ? ITALIC : REGULAR;
                FontWeight weight = tmp.contains("bold") ? BOLD : NORMAL;
                // for some reason javaFX and css values are quite different...
                String styleS = style==ITALIC ? "italic" : "normal";
                String weightS = weight==BOLD ? "bold" : "normal";
                w.getStage().getScene().getRoot().setStyle(
                    "-fx-font-family: \"" + f.getFamily() + "\";" + 
                    "-fx-font-style: " + styleS + ";" + 
                    "-fx-font-weight: " + weightS + ";" + 
                    "-fx-font-size: " + f.getSize() + ";"
                );
            });
        }
    }
}