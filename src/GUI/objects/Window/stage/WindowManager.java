/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.Window.stage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import Layout.container.layout.Layout;
import Layout.widget.Widget;
import Layout.widget.feature.HorizontalDock;
import util.action.IsAction;
import util.action.IsActionable;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.Window.stage.WindowBase.Maximized;
import gui.objects.icon.Icon;
import main.App;
import util.File.FileUtil;
import util.access.V;
import util.access.VarEnum;
import util.animation.Anim;
import util.async.executor.FxTimer;
import util.dev.TODO;
import util.dev.TODO.Purpose;
import util.graphics.fxml.ConventionFxmlLoader;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static java.io.File.separator;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.File.FileUtil.listFiles;
import static util.dev.Util.log;
import static util.dev.Util.no;
import static util.functional.Util.mapB;
import static util.functional.Util.stream;
import static util.graphics.Util.add1timeEventHandler;
import static util.reactive.Util.maintain;

/**
 * Manages windows.
 *
 * @author Plutonium_
 */
@IsConfigurable("Window")
@IsActionable
public class WindowManager implements Configurable<Object> {

    // todo: remove & auto-crate from config annotation
    @IsAction(name = "Mini mode", global = true, keys = "F9",
              desc = "Dock auxiliary window with playback control to the screen edge")
    private static void implToggleMini() {
        APP.windowManager.toggleMini();
    }

/**************************************************************************************************/

    public final ObservableList<Window> windows = Window.WINDOWS;
    public Window miniWindow;

    /**************************************** WINDOW SETTINGS *************************************/

    @IsConfig(name = "Opacity", info = "Window opacity.", min = 0, max = 1)
    public final V<Double> windowOpacity = new V<>(1d);

    @IsConfig(name = "Borderless", info = "Hides borders.")
    public final V<Boolean> window_borderless = new V<>(true);

    @IsConfig(name = "Headerless", info = "Hides header.")
    public final V<Boolean> window_headerless = new V<>(false);

    @IsConfig(name = "Bgr effect", info = "Effect applied on window background.")
    public final V<Effect> window_bgr_effect = new V<>(new BoxBlur(11, 11, 4));

    @IsConfig(name="Show windows", info="Shows/hides all windows. Useful in minimode.")
    public final V<Boolean> show_windows =  new V<>(true, v -> {
        if(!App.APP.normalLoad) return;
        if(v) windows.stream().filter(w->w!=miniWindow).forEach(Window::show);
        else windows.stream().filter(w->w!=miniWindow).forEach(Window::hide);
    });

    @IsConfig(name="Mini mode", info="Whether application has mini window docked to top screen edge active.")
    public final V<Boolean> mini = new V<>(false, this::setMini);

    @IsConfig(name="Mini open hover delay", info="Time for mouse to hover to open mini window.")
    public Duration mini_hover_delay = millis(700);

    @IsConfig(name="Mini hide when inactive", info="Hide mini window when no mouse activity is detected.")
    public final V<Boolean> mini_hide_onInactive = new V<>(true);

    @IsConfig(name="Mini hide when inactive for", info="Time of no activity to hide mini window after.")
    public Duration mini_inactive_delay = millis(700);

    @IsConfig(name="Mini widget", info="Widget to use in mini window.")
    public final VarEnum<String> mini_widget = new VarEnum<String>("PlayerControlsTiny",
        () -> APP.widgetManager.getFactories().filter(wf -> wf.hasFeature(HorizontalDock.class)).map(wf -> wf.name()).collect(toList())
    );



    /**
     * Get focused window. There is zero or one focused window in the application
     * at any given time.
     *
     * @see #getActive()
     * @return focused window or null if none focused.
     */
    public Window getFocused() {
	return stream(windows).findAny(w -> w.focused.get()).orElse(null);
    }

    /**
     * Same as {@link #getFocused()} but when none focused returns main window
     * instead of null.
     * <p>
     * Both methods are equivalent except for when the application itself has no
     * focus - which is when no window has focus.
     * <p>
     * Use when null must absolutely be avoided and the main window substitute
     * for focused window will not break expected behavior and when this method
     * can get called when app has no focus (such as through global shortcut).
     * @return focused window or main window if none. Never null.
     */
    public Window getActive() {
	return stream(windows).findAny(w -> w.focused.get()).orElse(APP.window);
    }

    public Window create() {
        Stage owner = new Stage(UTILITY); // utility means no taskbar
              owner.setWidth(0);
              owner.setHeight(0);
              owner.setX(0);
              owner.setY(0);
              owner.setOpacity(0);
              owner.show();
        return create(owner,UNDECORATED);
    }

    public Window create(Stage owner, StageStyle style) {
        Window w = new Window(owner,style);
        try {
            w.root.getStylesheets().add( new File(APP.DIR_SKINS.getPath(), gui.GUI.skin.getValue() + separator + gui.GUI.skin.getValue() + ".css").toURI().toURL().toExternalForm());
        } catch (MalformedURLException ex) {
            Logger.getLogger(WindowManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        new ConventionFxmlLoader(Window.class, w.root, w).loadNoEx();   // load fxml part
        if(APP.window==null) setAsMain(w);
        windows.add(w); // add to list of active windows
        w.initialize();

        // bind properties
        w.disposables.add(maintain(windowOpacity, w.getStage().opacityProperty()));
        w.disposables.add(maintain(window_bgr_effect, w.backimage.effectProperty()));
        w.disposables.add(maintain(window_borderless, w::setBorderless));
        w.disposables.add(maintain(window_headerless, v -> !v, w::setHeaderVisible));

        return w;
    }

    public static Window createWindowOwner() {
	Window w = new Window();
               w.getStage().initStyle(UTILITY);
               w.s.setOpacity(0);
               w.s.setScene(new Scene(new Region()));
               ((Region)w.s.getScene().getRoot()).setBackground(null);
               w.s.getScene().setFill(null);
               w.s.setTitle(APP.name);
               w.s.getIcons().add(APP.getIcon());
               w.setSize(20, 20);
	return w;
    }

    private void setAsMain(Window w) {
        no(APP.window!=null, "Only one window can be main");

	APP.window = w;
	w.main = true;

        w.setIcon(null);
        w.setTitle(null);

        // move the window owner to screen of this window, which
        // moves taskbar icon to respective screen's taskbar
        w.moving.addListener((o,ov,nv) -> {
            if(ov && !nv)
                APP.taskbarIcon.setScreen(w.getScreen(w.getCenterXY()));
        });
        add1timeEventHandler(w.s, WINDOW_SHOWN, e -> APP.taskbarIcon.setScreen(w.getScreen(w.getCenterXY())));
//        s.iconifiedProperty().addListener((o,ov,nv) -> {
//            if(nv) APP.taskbarIcon.iconify(nv);
//        });

        Icon mainw_i = new Icon(FontAwesomeIcon.CIRCLE,5)
                .tooltip("Main window\n\nThis window is main app window\nClosing it will "
                       + "close application.");
        w.rightHeaderBox.getChildren().add(0, new Label(""));
        w.rightHeaderBox.getChildren().add(0,mainw_i);
    }


    private void toggleMini() {
        setMini(!mini.get());
    }

    private void toggleMiniFull() {
        if(!App.APP.normalLoad) return;
        if(mini.get()) APP.window.show();
        else APP.window.hide();
        setMini(!mini.get());
    }

    private void toggleShowWindows() {
        if(!App.APP.normalLoad) return;
        show_windows.set(!show_windows.get());
    }

    private void setMini(boolean val) {
        if(!App.APP.normalLoad) return;

        mini.set(val);
        if(val) {
            // avoid pointless operation
            if(miniWindow!=null && miniWindow.isShowing()) return;
            // get window instance by deserializing saved state
            // miniWindow = Window.deserialize(FILE_MINIWINDOW); // disabled for now (but works)
            // if not available, make new one, set initial size
            if(miniWindow == null)  miniWindow = create();
            Window.WINDOWS.remove(miniWindow); // ignore mini window in window operations
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 40);
            miniWindow.resizable.set(false);
            miniWindow.setAlwaysOnTop(true);

            // content controls
            Icon miniB = new Icon(null, 13, "Docked mode", this::toggleMiniFull);
            maintain(miniB.hoverProperty(), mapB(ANGLE_DOUBLE_UP,ANGLE_UP), miniB::icon);
            Icon mainB = new Icon(null, 13, "Show main window", this::toggleShowWindows);
            maintain(mainB.hoverProperty(), mapB(ANGLE_DOUBLE_DOWN,ANGLE_DOWN), mainB::icon);
            HBox controls = new HBox(8,mainB,miniB);
                 controls.setAlignment(Pos.CENTER_RIGHT);
                 controls.setFillHeight(false);
                 controls.setPadding(new Insets(5,5,5,25));

            // content
            BorderPane content = new BorderPane();
            miniWindow.setContent(content);
            content.setRight(controls);

            // maintain propert widget content until window closes
            miniWindow.disposables.add(maintain(
                mini_widget,
                name -> {
                    // Create widget or supply empty if not available
                    Widget<?> newW = APP.widgetManager.factories.getOrOther(name,"Empty").create();
                    // Close old widget if any to free resources
                    Widget<?> oldW = (Widget) content.getProperties().get("widget");
                    if(oldW!=null) oldW.close();
                    // set new widget
                    content.getProperties().put("widget",newW);
                    content.setCenter(newW.load());
                }
            ));

            // show and apply state
            miniWindow.show();
            miniWindow.setHeaderAllowed(false);
            miniWindow.setBorderless(true);
            miniWindow.update();
            miniWindow.back.setStyle("-fx-background-size: cover;"); // disallow bgr stretching
            miniWindow.content.setStyle("-fx-background-color: -fx-pane-color;"); // imitate widget area bgr
            // todo - rather allow widgetarea have no controls and use Window.setContent(Component)
            //        for simplicity and consistency

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
            mw_root.addEventHandler(MOUSE_CLICKED, e -> {
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
            miniWindow.serialize(new File(APP.DIR_LAYOUTS, "mini-window.w"));
            miniWindow.close();
            miniWindow=null;
        }
    }

    public void serialize() {
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
            miniWindow.serialize(new File(APP.DIR_LAYOUTS, "mini-window.w"));
    }

    public void deserialize(boolean load_normally) {
        List<Window> ws = new ArrayList<>();
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
            Window w = create();
                   w.setXYSizeInitial();
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

    public final class WindowConverter implements Converter {

	@Override
	public boolean canConvert(Class type) {
	    return Window.class.equals(type);
	}

        @TODO(purpose = Purpose.BUG, note = "fullscreen deserialization bug in WindowBase")
	@Override
	public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
	    Window w = (Window) value;
	    writer.startNode("W");
	    writer.setValue(w.W.getValue().toString());
	    writer.endNode();
	    writer.startNode("H");
	    writer.setValue(w.H.getValue().toString());
	    writer.endNode();
	    writer.startNode("X");
	    writer.setValue(w.X.getValue().toString());
	    writer.endNode();
	    writer.startNode("Y");
	    writer.setValue(w.Y.getValue().toString());
	    writer.endNode();
	    writer.startNode("minimized");
	    writer.setValue(w.s.iconifiedProperty().getValue().toString());
	    writer.endNode();
	    writer.startNode("maximized");
	    writer.setValue(w.MaxProp.getValue().toString());
	    writer.endNode();
	    writer.startNode("fullscreen");
//	    writer.setValue(w.FullProp.getValue().toString());
	    writer.setValue(Boolean.FALSE.toString());
	    writer.endNode();
	    writer.startNode("resizable");
	    writer.setValue(w.resizable.getValue().toString());
	    writer.endNode();
	    writer.startNode("alwaysOnTop");
	    writer.setValue(w.s.alwaysOnTopProperty().getValue().toString());
	    writer.endNode();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
	    Window w = create();
	    if (w == null) return null;

	    reader.moveDown();
	    w.W.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.H.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.X.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.Y.set(Double.parseDouble(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.s.setIconified(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.MaxProp.set(Maximized.valueOf(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.FullProp.set(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.resizable.set(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    reader.moveDown();
	    w.setAlwaysOnTop(Boolean.parseBoolean(reader.getValue()));
	    reader.moveUp();
	    return w;
	}
    }
}