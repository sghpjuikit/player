
package gui.objects.Window.stage;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;

import org.reactfx.Subscription;

import com.thoughtworks.xstream.io.StreamException;

import Configuration.Configurable;
import Configuration.IsConfigurable;
import Layout.Component;
import Layout.WidgetImpl.Configurator;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetFactory;
import Layout.Widgets.WidgetManager;
import gui.objects.PopOver.PopOver;
import gui.objects.icon.Icon;
import main.App;
import util.File.FileUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COGS;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static util.File.FileUtil.getName;
import static util.dev.Util.forbidNull;

/**
 *
 * @author uranium
 */
@IsConfigurable
public final class UiContext {
    
/********************************************** CLICK *********************************************/
    
    private static double x;
    private static double y;
    private static final Set<ClickHandler> onClicks = new HashSet<>();
    
    /** 
     * Handles mouse click anywhere in the application. Receives event source window 
     * as additional parameters next to the event.
     */
    public static Subscription onClick(ClickHandler h) {
        onClicks.add(h);
        return () -> onClicks.remove(h); 
    }
    
    /** Simple version of {@link #onClick(ClickHandler)} with no extra parameter. */
    public static Subscription onClick(EventHandler<MouseEvent> h) {
        return onClick((w,e) -> h.handle(e));
    }
    
    /** Fires an even for {@link #onClick(ClickHandler)}*/
    public static void fireAppMouseClickEvent(Window w, MouseEvent e) {
        onClicks.forEach(h -> h.handle(w,e));
    }

    /** Set last mouse press screen coordinatea. */
    static void setPressedXY(double screenX, double screenY) { 
        x = screenX;
        y = screenY;
    }
    
    /** Get last mouse press screen x coordinate. */
    public static double getX() {
        return Window.getActive().getX()+x;
    }
    
    /** Get last mouse press screen y coordinate. */
    public static double getY() {
        return Window.getActive().getY()+y;
    }
    
    
    /** 
     * @param widget widget to open, does nothing when null.
     */
    public static Window showWindow(Component widget) {
        Window w = Window.create();
               w.setContent(widget);
               w.show();
               w.setScreen(Window.getActive().getScreen());
               w.setXyCenter();
        return w;
    }
    
    public static PopOver showFloating(Widget w) {
        forbidNull(w);
        
        // build popup content
        Icon propB = new Icon(COGS,12,"Settings", e -> {
                  showSettings(w.getName(), w, (Node)e.getSource());
                  e.consume();
              });
        // build popup
        PopOver p = new PopOver(w.load());
                p.title.set(w.getInfo().name());
                p.setAutoFix(false);
                p.getHeaderIcons().addAll(propB);
                p.show(Window.getActive().getStage(),getX(),getY());
                // unregister the widget from active eidgets manually
                p.addEventFilter(WINDOW_HIDING, we -> WidgetManager.standaloneWidgets.remove(w));
        return p;
    }
    
    public static void showSettings(Widget w, Node n) {
        showSettings(w.getName(), w, n);
    }
    public static void showSettings(Configurable c, Node n) {
        showSettings(null, c, n);
    }
    public static void showSettings(String name, Configurable c, Node n) {
            Configurator sc = new Configurator(true);
                         sc.configure(c);
            PopOver p = new PopOver(sc);
                    p.title.set((name==null ? "" : name+" ") + " Settings");
                    p.setArrowSize(0); // autofix breaks the arrow position, turn off - sux
                    p.setAutoFix(true); // we need autofix here, because the popup can get rather big
                    p.setAutoHide(true);
                    p.show(n);
    }
    
    public static PopOver showFloating(Node content, String title) {
        forbidNull(content);
        forbidNull(title);  // we could use null, but disallow
        
        PopOver p = new PopOver(content);
                p.title.set(title);
                p.setAutoFix(false);
                p.show(Window.getActive().getStage(),getX(),getY());
        return p;
    }
    
    private static boolean launching1st = !App.INSTANCE.normalLoad;
    
    public static void launchComponent(File launcher) {
        WidgetFactory wf = null;
        Component w = null;
        
        // simple launcher version, contains widget name on 1st line
        String wn = FileUtil.readFileLines(launcher).limit(1).findAny().orElse("");
        wf = WidgetManager.getFactory(wn);
        if(wf!=null) w = wf.create();
        
        // try to deserialize normally
        if(w==null) {
            try {
                w = (Component) App.INSTANCE.serialization.x.fromXML(launcher);
            } catch (ClassCastException | StreamException ignored) {}
        }
            
        // try to build widget using just launcher filename
        if(w==null) {
            wf = WidgetManager.getFactory(getName(launcher));
            if(wf!=null) w = wf.create();
        }
        
        // launch
        if(w!=null) {
            if(launching1st) {
                App.getWindow().setContent(w);
                launching1st = false;
            } else {
                showWindow(w);
            }
        }
    }
    
    
/******************************************************************************/   
    
    public static WritableImage makeSnapshot(Node n) {
        return n.snapshot(new SnapshotParameters(), null);
    }
    
    
    public static interface ClickHandler {
        void handle(Window w, MouseEvent e);
    }
    
}