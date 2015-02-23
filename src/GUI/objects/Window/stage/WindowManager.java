/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Window.stage;

import Action.IsAction;
import Action.IsActionable;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.LayoutAggregators.SimpleWithMenuAgregator;
import GUI.LayoutAggregators.SwitchPane;
import GUI.objects.FadeButton;
import Layout.Layout;
import Layout.Widgets.WidgetManager;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.stage.Screen;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import main.App;
import util.File.FileUtil;
import util.dev.Log;

/**
 *
 * @author Plutonium_
 */
@IsConfigurable("Window")
@IsActionable
public class WindowManager {
    
    @IsConfig(name="Docked mini mode", info="Whether application layout is in mini - docked mode.")
    public static boolean mini = false;
    
    @AppliesConfig("mini")
    private static void applyMini() {
        setMini(mini);
    }
    
    static Window miniWindow;
    
    @IsAction(name = "Toggle mini mode", description = "Toggle minimal layout docked mode", global = true, shortcut = "F9")    
    public static void toggleMini() {
        setMini(!mini);
    }
    
    private static Animation t;

    public static void setMini(boolean val) {
        mini = val;
        if(val) {App.getWindow().hide();
            // avoid pointless operation
            if(miniWindow!=null && miniWindow.isShowing()) return;
            // get window instance by deserializing saved state
            File f = new File(App.LAYOUT_FOLDER(), "mini-window.w");
            miniWindow = Window.deserializeSuppressed(f);
            // if not available, make new one, set initial size
            if(miniWindow == null)  miniWindow = Window.create();
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 40);
            miniWindow.resizable.set(false);
            
            // create content
                // layout
            Layout l = new Layout();
                   l.locked.set(true);
                // layout wrapper
            SimpleWithMenuAgregator la= new SimpleWithMenuAgregator(l);
                // window control buttons
            FadeButton closeB = new FadeButton(TIMES, 15);
                       closeB.setOnMouseClicked( e -> {
                           App.close(); // closing the window is not enogh
                           e.consume();
                       });
                       Tooltip.install(closeB, new Tooltip("Close"));
            FadeButton onTopB = new FadeButton(STOP, 15);
                       onTopB.setOnMouseClicked( e -> {
                           miniWindow.toggleAlwaysOnTOp();
                           e.consume();
                       });
                       Tooltip.install(onTopB, new Tooltip("Toggle on top"));
            FadeButton toggleMiniB = new FadeButton(CARET_UP, 15);
                       toggleMiniB.setOnMouseClicked( e -> {
                           toggleMini();
                           e.consume();
                       });
                       Tooltip.install(toggleMiniB, new Tooltip("Toggle mini mode"));
            la.getMenu().getChildren().addAll(toggleMiniB,onTopB,closeB);
            miniWindow.setContent(la.getRoot());
                // widget
            l.setChild(WidgetManager.getFactory("PlayerControlsTiny").create());
            // show and apply state
            miniWindow.show();
            miniWindow.setHeaderAllowed(false);
            miniWindow.update();
            
            // install autohiding
            t = new Transition() {
                {
                    setCycleDuration(Duration.millis(300));
                }
                @Override
                protected void interpolate(double frac) {
                    double H = miniWindow.getHeight()-2; // leave 2 pixels visible
                    miniWindow.setY(-H*frac, false);
                }
            };
            
//            miniWindow.s.focusedProperty().addListener( (o,ov,nv) -> {
//                if(nv) return;
//                Duration delay = Duration.ZERO;//Duration.seconds(0.8);
//                Duration d = t.getCurrentTime();
//                if(d.equals(Duration.ZERO)) {
//                    d = Duration.millis(300).subtract(d);
//                    delay = Duration.seconds(0.8);
//                }
//                t.stop();
//                t.setDelay(delay);
//                t.setOnFinished(a->miniWindow.setContentMouseTransparent(true));
//                t.setRate(1);
//                t.playFrom(Duration.millis(300).subtract(d));
//            });
            miniWindow.getStage().getScene().getRoot().addEventFilter(MOUSE_EXITED, e -> {
                Duration delay = Duration.ZERO;//Duration.seconds(0.8);
                Duration d = t.getCurrentTime();
                if(d.equals(Duration.ZERO)) {
                    d = Duration.millis(300).subtract(d);
                    delay = Duration.seconds(0.8);
                }
                t.stop();
                t.setDelay(delay);
                t.setOnFinished(a->miniWindow.setContentMouseTransparent(true));
                t.setRate(1);
                t.playFrom(Duration.millis(300).subtract(d));
            });
            
            miniWindow.getStage().getScene().getRoot().addEventFilter(MOUSE_ENTERED, e -> {
                Duration d = t.getCurrentTime();
                if(d.equals(Duration.ZERO)) 
                    d = Duration.millis(300).subtract(d);
                t.stop();
                t.setDelay(ZERO);
                t.setOnFinished(a->miniWindow.setContentMouseTransparent(false));
                t.setRate(-1);
                t.playFrom(d);
                
//                miniWindow.getStage().toFront();miniWindow.focus();
//                miniWindow.getStage().getScene().getRoot().requestFocus();
            });
            
            
            
            
        } else {
            // do nothing if not in minimode (for example during initialization)
            if(miniWindow==null) return;
            // serialize mini
            File f = new File(App.LAYOUT_FOLDER(), "mini-window.w");
            miniWindow.serializeSupressed(f);
            // hide mini, show normal
            App.getWindow().show();
            miniWindow.close();
            miniWindow=null;
            if(t!=null) {
                t.stop();
                t=null;
            }
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
    }
    
    
/******************************** NO TASKBAR MODE *****************************/
    
    @IsConfig(name="Show taskbar icon", info="Show taskbar icon. Requires application restart.")
    public static boolean show_taskbar_icon = true;
    
}