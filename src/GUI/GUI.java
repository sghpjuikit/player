
package GUI;

import AudioPlayer.Player;
import Configuration.Configurable;
import Configuration.Configuration;
import Configuration.IsAction;
import Configuration.IsConfig;
import Configuration.SkinEnum;
import GUI.objects.popups.MoodPicker;
import Layout.Layout;
import Layout.LayoutManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import main.App;
import utilities.FileUtil;
import utilities.Log;

/**
 *
 * @author uranium
 */
public class GUI implements Configurable {
    
    // properties
    @IsConfig(name = "Font", info = "Application font.")
    public static Font font = Font.getDefault();
    @IsConfig(name = "Opacity", info = "Application opacity.", min=0, max=1)
    public static double windowOpacity = 1;
    @IsConfig(name = "Skin", info = "Application skin.")
    public static SkinEnum skin = new SkinEnum("Modena");
    
    @IsConfig(name = "Overlay effect", info = "Use color overlay effect.")
    public static boolean gui_overlay = false;
    @IsConfig(name = "Overlay effect use song color", info = "Use song color if available as source color for gui overlay effect.")
    public static boolean gui_overlay_use_song = false;
    @IsConfig(name = "Overlay effect color", info = "Set color for color overlay effect.")
    public static Color gui_overlay_color = Color.BLACK;
    @IsConfig(name = "Overlay effect normalize", info = "Forbid contrast and brightness change. Applies only hue portion of the color for overlay effect.")
    public static boolean gui_overlay_normalize = true;
    @IsConfig(name = "Overlay effect intensity", info = "Intensity of the color overlay effect.", min=0, max=1)
    public static double gui_overlay_norm_factor = 0.5;
    
    @IsConfig(name = "Layout mode blur", info = "Layout mode use blur effect.")
    public static boolean blur_layoutMode = true;
    @IsConfig(name = "Layout mode fade", info = "Layout mode use fade effect.")
    public static boolean opacity_layoutMode = false;
    @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.", min=0.0, max=1.0)
    public static double opacity_LM = 0.2;
    @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur efect intensity.", min=0.0, max=10.0)
    public static double blur_LM = 5;
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
    @IsAction(name = "Reload GUI.", info = "Reload application GUI.", shortcut = "F5")
    public static void refresh() {
        if (App.getInstance().isGuiInitialized()) {
            App.getInstance().setSkin(skin.get());                  // set skin
            applyFont();
            App.getInstance().getWindow().update();                 // reinitialize window
            applyOverlayUseAppColor();
            applyColorOverlay();
            loadLayout();
            applyAlignTabs();
        }
    }
    
    /** Loads/refreshes active layout. */
    @IsAction(name = "Reload layout", info = "Reload layout.", shortcut = "F6")
    public static void loadLayout() {
        LayoutManager.loadActive();
        LayoutManager.active.values().stream()
                .flatMap(l->l.getAllWidgets().stream()).forEach(w->w.getController().refresh());
    }
    
    /** Toggles layout controlling mode. */
    @IsAction(name = "Manage Layout", info = "Enables layout managment mode.", shortcut = "F7")
    public static void toggleLayoutMode() {
        if (alt_state)
            setLayoutMode(false);
        else
            setLayoutMode(true);
    }
    
    @IsAction(name = "Minimize", info = "Switch minimized mode.", shortcut = "F9")
    public static void toggleMinimize() {
        App.getInstance().getWindow().toggleMinimize();
    }
    
    @IsAction(name = "Maximize", info = "Switch maximized mode.", shortcut = "F10")
    public static void toggleMaximize() {
        App.getInstance().getWindow().toggleMaximize();
    }
    
    @IsAction(name = "Fullscreen", info = "Switch fullscreen mode.", shortcut = "F11")
    public static void toggleFullscreen() {
        App.getInstance().getWindow().toggleFullscreen();
    }

    
    /**
     * Searches for .css files in skin folder and registers them as available
     * skins. Use on app start or to discover newly added layouts dynamically.
     */
    public static void findSkins() {
        // get + verify path
        File dir = new File(Configuration.SKIN_FOLDER);
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
        skin.clear();
        if (files.isEmpty())
            Log.mess("Skin folder '" + Configuration.SKIN_FOLDER + "' is empty. No valid skins found.");
        else
            Log.mess(files.size() + " valid skins found.");
        
        for (File f : files) {
            String name = FileUtil.getName(f);
            skin.add(name);
            Log.mess("Skin " + name + " registered.");
        }
//        skin.add("Modena");
//        skin.add("Caspian");
        skin.add(Application.STYLESHEET_CASPIAN);
        skin.add(Application.STYLESHEET_MODENA);
    }
    
    public static List<String> getSkins() {
       return skin.values();
    }
    
    
/****************************  applying methods *******************************/
    
    private static void applyFont() {
        if (App.getInstance().isGuiInitialized()) {
            String tmp = font.getStyle().toLowerCase();
            FontPosture style = tmp.contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
            FontWeight weight = tmp.contains("bold") ? FontWeight.BOLD : FontWeight.NORMAL;
            // for some reason java and css values are quite different...
            String styleS = style==FontPosture.ITALIC ? "italic" : "normal";
            String weightS = weight==FontWeight.BOLD ? "bold" : "normal";
            App.getInstance().getWindow().getStage().getScene().getRoot().setStyle(
                    "-fx-font-family: \"" + font.getFamily() + "\";" +
                    "-fx-font-style: " + styleS + ";" +
                    "-fx-font-weight: " + weightS + ";" +
                    "-fx-font-size: " + font.getSize() + ";");
        }
    }
    
    private static void applyColorOverlay() {
        if(gui_overlay) {            
            // get color
            Color c = null;
            if(gui_overlay_use_song && Player.getCurrentMetadata()!= null)
                            c = Player.getCurrentMetadata().getColor();
            if(c == null)   c = gui_overlay_color;
            
            // normalize color
            if(gui_overlay_normalize)
                c = Color.hsb(c.getHue(), 0.5, 0.5, gui_overlay_norm_factor);
            
            // apply effect
            ContextManager.gui.colorEffectPane.setBlendMode(BlendMode.OVERLAY);
            ContextManager.gui.colorEffectPane.setBackground(new Background(
                    new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
            ContextManager.gui.colorEffectPane.setVisible(true);
        } else {
            // disable effect
            ContextManager.gui.colorEffectPane.setVisible(false);
        }
    }
    
    private static InvalidationListener setOverlay;
    
    private static void applyOverlayUseAppColor() {
        if(gui_overlay_use_song) {
            if(setOverlay==null) setOverlay = (o) -> applyColorOverlay();
            Player.currentMetadataProperty().addListener(setOverlay);
        }
        else {
            if(setOverlay!=null)
                Player.currentMetadataProperty().removeListener(setOverlay);
            setOverlay=null;
        }
    }
    
    private static void applyAlignTabs() {
        if(align_tabs) ContextManager.gui.alignTabs();
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
    
    public static void setColorEffect(Color color) {
        gui_overlay_color = color;
        applyColorOverlay();
    }
}
