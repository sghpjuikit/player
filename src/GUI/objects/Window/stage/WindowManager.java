/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.Window.stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.container.layout.Layout;
import Layout.widget.Widget;
import action.IsAction;
import action.IsActionable;
import gui.objects.icon.Icon;
import main.App;
import util.File.FileUtil;
import util.access.V;
import util.animation.Anim;
import util.async.executor.FxTimer;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.File.FileUtil.listFiles;
import static util.dev.Util.log;
import static util.functional.Util.mapB;
import static util.graphics.Util.add1timeEventHandler;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
@IsConfigurable("Window")
@IsActionable
public class WindowManager {

    /**************************************** WINDOW SETTINGS *************************************/

    @IsConfig(name = "Opacity", info = "Window opacity.", min = 0, max = 1)
    public static final V<Double> windowOpacity = new V<>(1d);

    @IsConfig(name = "Borderless", info = "Hides borders.")
    public static final V<Boolean> window_borderless = new V<>(true);

    @IsConfig(name = "Headerless", info = "Hides header.")
    public static final V<Boolean> window_headerless = new V<>(false);

    @IsConfig(name = "Bgr effect", info = "Effect applied on window background.")
    public static final V<Effect> window_bgr_effect = new V<>(new BoxBlur(11, 11, 4));



    public static final File FILE_MINIWINDOW = new File(APP.DIR_LAYOUTS, "mini-window.w");

    @IsConfig(name="Show windows", info="Shows/hides all windows. Useful in minimode.")
    public static boolean show_windows = true;

    @IsConfig(name="Mini mode", info="Whether application has mini window docked to top screen edge active.")
    public static boolean mini = false;

    @IsConfig(name="Mini open hover delay", info="Time for mouse to hover to open mini window.")
    public static Duration mini_hover_delay = millis(700);

    @IsConfig(name="Mini hide when inactive", info="Hide mini window when no mouse activity is detected.")
    public static final V<Boolean> mini_hide_onInactive = new V<>(true);

    @IsConfig(name="Mini hide when inactive for", info="Time of no activity to hide mini window after.")
    public static Duration mini_inactive_delay = millis(700);

    @AppliesConfig("mini")
    private static void applyMini() {
        setMini(mini);
    }
    @AppliesConfig("show_windows")
    private static void applyShowWindows() {
        if(!App.APP.normalLoad) return;

        if(show_windows)
            Window.WINDOWS.stream().filter(w->w!=miniWindow).forEach(Window::show);
        else
            Window.WINDOWS.stream().filter(w->w!=miniWindow).forEach(Window::hide);
    }


    @IsAction(name = "Mini mode", global = true, keys = "F9",
              desc = "Dock auxiliary window with playback control to the screen edge")
    public static void toggleMini() {
        setMini(!mini);
    }
    public static void toggleMiniFull() {
        if(!App.APP.normalLoad) return;

        if(mini) APP.window.show();
        else APP.window.hide();
        setMini(!mini);
    }
    public static void toggleShowWindows() {
        if(!App.APP.normalLoad) return;

        show_windows = !show_windows;
        applyShowWindows();
    }

    public static Window miniWindow;
    private static Animation t;

    public static void setMini(boolean val) {
        if(!App.APP.normalLoad) return;

        mini = val;
        if(val) {
            // avoid pointless operation
            if(miniWindow!=null && miniWindow.isShowing()) return;
            // get window instance by deserializing saved state
            // miniWindow = Window.deserialize(FILE_MINIWINDOW); // disabled for now (but works)
            // if not available, make new one, set initial size
            if(miniWindow == null)  miniWindow = Window.create();
            Window.WINDOWS.remove(miniWindow); // ignore mini window in window operations
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 40);
            miniWindow.resizable.set(false);
            miniWindow.setAlwaysOnTop(true);

            // create
                // widget
            Widget w = APP.widgetManager.factories.getOrOther("PlayerControlsTiny","Empty").create();
            BorderPane content = new BorderPane();
            content.setCenter(w.load());
            miniWindow.setContent(content);
                // menu
            Icon miniB = new Icon(null, 13, "Docked mode", WindowManager::toggleMiniFull);
            maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP,ANGLE_UP), miniB::icon);
            Icon mainB = new Icon(null, 13, "Show main window", WindowManager::toggleShowWindows);
            maintain(mainB.hoverProperty(), mapB(ANGLE_DOUBLE_DOWN,ANGLE_DOWN), mainB::icon);

            HBox controls = new HBox(8,mainB,miniB);
                 controls.setAlignment(Pos.CENTER_RIGHT);
                 controls.setFillHeight(false);
                 controls.setPadding(new Insets(5,5,5,25));
            content.setRight(controls);

            // show and apply state
            miniWindow.show();
            miniWindow.setHeaderAllowed(false);
            miniWindow.setBorderless(true);
            miniWindow.update();
            miniWindow.back.setStyle("-fx-background-size: cover;"); // disallow bgr stretching
            miniWindow.content.setStyle("-fx-background-color: -fx-pane-color;"); // imitate widget area bgr
            miniWindow.s.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> APP.widgetManager.standaloneWidgets.remove(w));

            // autohiding
            double H = miniWindow.getHeight()-2; // leave 2 pixels visible
            Parent mw_root = miniWindow.getStage().getScene().getRoot();
            Anim a = new Anim(millis(300),frac -> miniWindow.setY(-H*frac, false));

            FxTimer hider = new FxTimer(0, 1, () -> {
                if(miniWindow==null) return;
                if(miniWindow.getY()!=0) return;    // if not open
                Duration d = a.getCurrentTime();
                if(d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(1);
                a.playFrom(millis(300).subtract(d));
            });
            mw_root.addEventFilter(MouseEvent.ANY, e -> {
                if(!mini_hide_onInactive.get()) return;   // if disabled
                if(mw_root.isHover()) return;       // if mouse still in (we only want MOUSE_EXIT)
                hider.start(mini_inactive_delay);
            });
            hider.runNow();

            FxTimer shower = new FxTimer(0, 1, () ->{
                if(miniWindow==null) return;
                if(miniWindow.getY()==0) return;    // if open
                if(!mw_root.isHover()) return;      // if mouse left
                Duration d = a.getCurrentTime();
                if(d.equals(ZERO)) d = millis(300).subtract(d);
                a.stop();
                a.setRate(-1);
                a.playFrom(d);
            });
            mw_root.addEventFilter(MOUSE_ENTERED, e -> {
                if(!miniWindow.isShowing()) return;     // bugfix
                shower.start(mini_hover_delay);         // open after delay
            });
            mw_root.addEventFilter(MOUSE_CLICKED, e -> {
                if(e.getButton()==PRIMARY) {
                    if(!miniWindow.isShowing()) return; // bugfix
                    shower.runNow();                    // open with delay
                }
                if(e.getButton()==SECONDARY) {
                    if(!miniWindow.isShowing()) return; // bugfix
                    hider.runNow();                     // open with delay
                }
            });
        } else {
            // do nothing if not in minimode (for example during initialization)
            if(miniWindow==null) return;
            // serialize mini
            miniWindow.serialize(FILE_MINIWINDOW);
            miniWindow.close();
            miniWindow=null;
            t=null;
        }
    }

    public static void serialize() {
        // make sure directory is accessible
        File dir = new File(APP.DIR_LAYOUTS,"Current");
        if (!FileUtil.isValidatedDirectory(dir)) {
            log(WindowManager.class).error("Serialization of windows and layouts failed. " + dir.getPath() +
                    " could not be accessed.");
            return;
        }

        // get windows
        List<Window> src = new ArrayList<>(Window.WINDOWS);
                     src.remove(miniWindow);    // manually
        log(WindowManager.class).info("Serializing " + src.size() + " application windows");

        // remove serialized files from previous session
        listFiles(dir).forEach(File::delete);

        // serialize - for now each window to its own file with .ws extension
        for(int i=0; i<src.size(); i++) {
            // ret resources
            Window w = src.get(i);
            String name = "window" + i;
            File f = new File(dir, name + ".ws");
            // serialize window
            w.serialize(f);
            // serialize layout (associate the layout with the window by name)
            Layout l = w.getLayout();
            l.setName("layout" + i);
            l.serialize(new File(dir,l.getName()+".l"));
        }

        // serialize mini too
        if(miniWindow!=null)
            miniWindow.serialize(FILE_MINIWINDOW);
    }

    public static void deserialize(boolean load_normally) {
        List<Window> ws = new ArrayList();
        if(load_normally) {

            // make sure directory is accessible
            File dir = new File(APP.DIR_LAYOUTS,"Current");
            if (!FileUtil.isValidatedDirectory(dir)) {
                log(WindowManager.class).error("Deserialization of windows and layouts failed. " + dir.getPath() +
                        " could not be accessed.");
                return;
            }

            // discover all window files with 'ws extension
            File[] fs = dir.listFiles(f->f.getName().endsWith(".ws"));
            log(WindowManager.class).info("Deserializing {} application windows", fs.length);

            // deserialize windows
            for(int i=0; i<fs.length; i++) {
                File f = fs[i];
                Window w = Window.deserialize(f);

                // handle next window if this was not successfully deserialized
                if(w==null) continue;
                ws.add(w);

                // deserialize layout
                File lf = new File(dir,"layout"+i+".l");
                Layout l = lf.exists() ? new Layout("layout"+i).deserialize(lf) : null;
                if(l==null) w.initLayout();
                else w.initLayout(l);
            }
         }

        // show windows
        if(ws.isEmpty()) {
            Window w = Window.create();
                   w.setXyNsizeToInitial();
                   w.initLayout();
                   w.update();
                   w.show();
            ws.add(w);
        } else {
            ws.forEach(w -> add1timeEventHandler(w.s,WINDOW_SHOWING, e -> w.update()));
            log(WindowManager.class).info("Deserialized " + ws.size() + " windows.");
            Widget.deserializeWidgetIO();
        }
    }

/******************************** NO TASKBAR MODE *****************************/

    @IsConfig(name="Show taskbar icon", info="Show taskbar icon. Requires application restart.")
    public static boolean show_taskbar_icon = true;
}