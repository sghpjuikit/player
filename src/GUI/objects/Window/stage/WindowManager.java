/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.Window.stage;

import action.IsAction;
import action.IsActionable;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.Layout;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import gui.LayoutAggregators.SwitchPane;
import gui.objects.icon.Icon;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import main.App;
import util.File.FileUtil;
import util.animation.Anim;
import util.async.executor.FxTimer;
import util.dev.Log;
import static util.functional.Util.mapB;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
@IsConfigurable("Window")
@IsActionable
public class WindowManager {
    
    @IsConfig(name="Show windows", info="Shows/hides all windows. Useful in minimode.")
    public static boolean show_windows = true;
    
    @IsConfig(name="Mini mode", info="Whether application has mini window docked to top screen edge active.")
    public static boolean mini = false;
    
    @IsConfig(name="Mini open on click", info="Open mini window on screen top edge click.")
    public static boolean mini_show_onClick = true;
    
    @IsConfig(name="Mini open on hover", info="Open mini window on screen top edge hover.")
    public static boolean mini_show_onEnter = true;
    
    @IsConfig(name="Mini open hover delay", info="Time to hover to open mini window.")
    public static Duration mini_hover_delay = millis(500);
    
    @IsConfig(name="Mini hide when inactive", info="Hide mini window when no mouse activity is detected.")
    public static boolean mini_hide_onInactive = true;
    
    @IsConfig(name="Mini hide when inactive for", info="Time of no activity to hide mini window after.")
    public static Duration mini_inactive_delay = millis(500);
    
    @AppliesConfig("mini")
    private static void applyMini() {
        setMini(mini);
    }
    @AppliesConfig("show_windows")
    private static void applyShowWindows() {
        if(show_windows)
            Window.windows.stream().filter(w->w!=miniWindow).forEach(Window::show);
        else
            Window.windows.stream().filter(w->w!=miniWindow).forEach(Window::hide);
    }
    
    
    @IsAction(name = "Toggle mini mode", description = "Toggle minimal layout docked mode", global = true, shortcut = "F9")    
    public static void toggleMini() {
        setMini(!mini);
    }
    public static void toggleMiniFull() {
        if(mini) App.getWindow().show();
        else App.getWindow().hide();
        setMini(!mini);
    }
    public static void toggleShowWindows() {
        show_windows = !show_windows;
        applyShowWindows();
    }
    
    static Window miniWindow;
    private static Animation t;

    public static void setMini(boolean val) {
        mini = val;
        if(val) {
            // avoid pointless operation
            if(miniWindow!=null && miniWindow.isShowing()) return;
            // get window instance by deserializing saved state
            File f = new File(App.LAYOUT_FOLDER(), "mini-window.w");
            miniWindow = Window.deserializeSuppressed(f);
            // if not available, make new one, set initial size
            if(miniWindow == null)  miniWindow = Window.create();
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 40);
            miniWindow.resizable.set(false);
            miniWindow.setAlwaysOnTop(true);
            
            // create
                // widget
            Widget w = WidgetManager.getFactory("PlayerControlsTiny").create();
            BorderPane content = new BorderPane();
            content.setCenter(w.load());
            miniWindow.setContent(content);
                // menu
            Icon closeB = new Icon(CLOSE, 13, "Close window", App::close);
            Icon miniB = new Icon(null, 13, "Docked mode", WindowManager::toggleMiniFull);
            maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP,ANGLE_UP), miniB.icon);
            Icon mainB = new Icon(null, 13, "Show main window", WindowManager::toggleShowWindows);
            maintain(mainB.hoverProperty(), mapB(ANGLE_DOUBLE_DOWN,ANGLE_DOWN), mainB.icon);
            
            HBox controls = new HBox(8,mainB,miniB,closeB);
                 controls.setAlignment(Pos.CENTER_RIGHT);
                 controls.setFillHeight(false);
                 controls.setPadding(new Insets(5,5,5,25));
            content.setRight(controls);
            
            // show and apply state
            miniWindow.show();
            miniWindow.setHeaderAllowed(false);
            miniWindow.setBorderless(true);
            miniWindow.update();
            miniWindow.bgrImgLayer.setStyle("-fx-background-size: cover;"); // disallow bgr stretching
            miniWindow.content.setStyle("-fx-background-color: -fx-pane-color;"); // imitate widget area bgr
            miniWindow.s.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> WidgetManager.standaloneWidgets.remove(w));
            
            // autohiding
            double H = miniWindow.getHeight()-2; // leave 2 pixels visible
            Parent mw_root = miniWindow.getStage().getScene().getRoot();
            Anim a = new Anim(millis(300),frac -> {
                miniWindow.setY(-H*frac, false);
            });
            
            FxTimer hider = new FxTimer(0, 1, () -> {
                if(miniWindow.getY()!=0) return;    // if not open
                if(mw_root.isHover()) return;       // if mouse still in
                Duration d = a.getCurrentTime();
                if(d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(1);
                a.playFrom(millis(300).subtract(d));
            });
            mw_root.addEventFilter(MouseEvent.ANY, e -> {
                if(!mini_hide_onInactive) return;   // if disabled
                hider.restart(mini_inactive_delay);
            });
            
            FxTimer shower = new FxTimer(0, 1, () ->{
                if(miniWindow.getY()==0) return;    // if open
                if(!mw_root.isHover()) return;      // if mouse left
                Duration d = a.getCurrentTime();
                if(d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(-1);
                a.playFrom(d);
            });
            mw_root.addEventFilter(MOUSE_ENTERED, e -> {
                if(!mini_show_onEnter) return;      // if disabled
                if(!miniWindow.isShowing()) return; // bugfix
                shower.restart(mini_hover_delay);   // open after delay
            });
            mw_root.addEventFilter(MOUSE_CLICKED, e -> {
                if(!mini_show_onClick) return;      // if disabled
                if(miniWindow.getY()==0) return;    // if open
                if(!miniWindow.isShowing()) return; // bugfix
                shower.runNow();                    // open with delay
            });
        } else {
            // do nothing if not in minimode (for example during initialization)
            if(miniWindow==null) return;
            // serialize mini
            File f = new File(App.LAYOUT_FOLDER(), "mini-window.w");
            miniWindow.serializeSupressed(f);
            miniWindow.close();
            miniWindow=null;
            t=null;
        }
    }

    
    public static void serialize() {
        // make sure directory is accessible
        File dir = new File(App.LAYOUT_FOLDER(),"Current");
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Serialization of windows and layouts failed. " + dir.getPath() +
                    " could not be accessed.");
            return;
        }
        
        // get windows
        List<Window> src = new ArrayList<>(Window.windows);
                     src.remove(miniWindow);    // manually
        Log.deb("Serializing " + src.size() + " application windows for next session.");
        
        // remove serialized files from previous session
        for(File f: dir.listFiles()) f.delete();
        Log.deb("Removing all files from previous session.");
        
        // serialize - for now each window to its own file with .ws extension
        int count = 0;
        for(int wi=0; wi<src.size(); wi++) {
            // ret resources
            Window w = src.get(wi);
            String name = "window" + wi;
            File f = new File(dir, name + ".ws");
            // serialize
            boolean success = w.serializeSupressed(f);
            if (success) count++;
            // serialize contained layouts
            for(Map.Entry<Integer,Layout> e : w.getLayoutAggregator().getLayouts().entrySet()) {
                int li = e.getKey();
                Layout l = e.getValue();
                // dont continue if layout empty
                if(l.getChild() == null) continue;
                // associate the layout with the window by name & save
                l.setName("window" + wi + "-layout" + li);
                l.serialize(new File(dir,l.getName()+".l"));
            }
        }
        
        Log.deb("Serialized " + count + " windows.");
        
        // serialize mini too
        if(miniWindow!=null) {
            File f = new File(App.LAYOUT_FOLDER(), "mini-window.w");
            miniWindow.serializeSupressed(f);
        }
    }
    
    public static void deserialize() {
        // make sure directory is accessible
        File dir = new File(App.LAYOUT_FOLDER(),"Current");
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Deserialization of windows and layouts failed. " + dir.getPath() +
                    " could not be accessed.");
            return;
        }
        
        Log.deb("Deserializing application windows from next session.");
        // discover all window files with 'ws extension
        File[] fs = dir.listFiles(f->f.getName().endsWith(".ws"));
        Log.deb("Discovered " + fs.length + " window files to deserialize.");
        
        // prepare layout files
        Map<Integer,Map<Integer,File>> lmap= new HashMap(); // map windowIndex-layoutFiles
        File[] lfs = dir.listFiles(f->f.getPath().endsWith(".l"));
        for(File f : lfs) {
            // parse string and get window index
            String name = f.getName();
            int from = name.indexOf("window");
            int to = name.indexOf('-');
            String number = name.substring(from+6, to);
            int wIndex;
            try{
                wIndex = new Integer(number);
            } catch(NumberFormatException e) {
                // ignore file if damaged name
                continue;
            }
            
            // parse string and get layout index
            from = name.indexOf("layout");
            to = name.indexOf(".");
            number = name.substring(from+6, to);
            int lIndex;
            try{
                lIndex = new Integer(number);
            } catch(NumberFormatException e) {
                // ignore file if damaged name
                continue;
            }
            
            // put layout to map's list with index of the window it belongs to
            if(!lmap.containsKey(wIndex)) lmap.put(wIndex, new HashMap());
            lmap.get(wIndex).put(lIndex,f);
        }
        
        // deserialize windows
        List<Window> ws = new ArrayList();
        for(int i=0; i<fs.length; i++) {
            File f = fs[i];
            Window w = Window.deserializeSuppressed(f);
            
            // handle next window if this was not successfully deserialized
            if(w==null) continue;
            ws.add(w);
            
            // avoid null if no layout for this window
            if(lmap.get(i)==null) lmap.put(i,new HashMap<>());
            
            
            SwitchPane la = new SwitchPane();
            // otherwise deserialize layout
            lmap.get(i).forEach( (at,lf) -> {
                Layout l = new Layout(FileUtil.getName(lf)).deserialize(lf);
                la.addTab(at,l);
            });
            
            w.setLayoutAggregator(la);
        }
        
        // make sure there is at least one window
        if(ws.isEmpty()) {
            Window w = Window.create();
                   w.setXyNsizeToInitial();
                   w.setLayoutAggregator(new SwitchPane());
            ws.add(w);
        }
        
        // grab main window and initialize it
        ws.get(0).setAsMain();
               
        // show
        ws.forEach(w->{
            w.show();
            w.update();
        });
        Log.deb("Deserialized " + ws.size() + " windows.");
        
        // when deserializating in minimode make sure the windows state gets
        // updated
        if(mini) {
            ws.forEach( w -> w.getStage().setOnShown(e -> {
                // update window state when it is shown
                w.update();
                // remove handler, make this one time only
                w.getStage().setOnShown(null);
            }));
        }
        
        
        
        Widget.deserializeWidgetIO();
    }

    
/******************************** NO TASKBAR MODE *****************************/
    
    @IsConfig(name="Show taskbar icon", info="Show taskbar icon. Requires application restart.")
    public static boolean show_taskbar_icon = true;
    
}