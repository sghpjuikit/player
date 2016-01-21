
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.io.StreamException;

import Configuration.Configurable;
import Configuration.IsConfigurable;
import Configurator.Configurator;
import Layout.Component;
import Layout.widget.Widget;
import Layout.widget.WidgetFactory;
import gui.objects.PopOver.PopOver;
import gui.objects.icon.Icon;
import main.App;
import util.File.FileUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COGS;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static main.App.APP;
import static util.File.FileUtil.getName;
import static util.dev.Util.noØ;

/**
 *
 * @author uranium
 */
@IsConfigurable
public final class UiContext {

/********************************************** CLICK *********************************************/

    private static final Logger LOGGER = LoggerFactory.getLogger(UiContext.class);
    private static boolean launching1st = !App.APP.normalLoad;
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
               w.initLayout();
               w.setContent(widget);
               w.show();
               w.setScreen(Window.getActive().getScreen());
               w.centerOnScreen();
        return w;
    }

    public static PopOver showFloating(Widget w) {
        noØ(w);

        // build popup content
        Icon propB = new Icon(COGS,12,"Settings", e -> {
                  showSettings(w, (Node)e.getSource());
                  e.consume();
              });
        // build popup
        PopOver p = new PopOver(w.load());
                p.title.set(w.getInfo().nameGui());
                p.setAutoFix(false);
                p.getHeaderIcons().addAll(propB);
                p.show(Window.getActive().getStage(),getX(),getY());
                // unregister the widget from active eidgets manually
                p.addEventFilter(WINDOW_HIDING, we -> APP.widgetManager.standaloneWidgets.remove(w));
        return p;
    }

    public static void showSettings(Configurable c, MouseEvent e) {
        showSettings(c, (Node) e.getSource());
    }

    public static void showSettings(Configurable c, Node n) {
        String name = c instanceof Widget ? ((Widget)c).getName() : "";
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
        noØ(content);
        noØ(title);  // we could use null, but disallow

        PopOver p = new PopOver(content);
                p.title.set(title);
                p.setAutoFix(false);
                p.show(Window.getActive().getStage(),getX(),getY());
        return p;
    }

    public static void launchComponent(File launcher) {
        try {
            WidgetFactory wf;
            Component w = null;

            // simple launcher version, contains widget name on 1st line
            String wn = FileUtil.readFileLines(launcher).limit(1).findAny().orElse("");
            wf = APP.widgetManager.factories.get(wn);
            if(wf!=null) w = wf.create();

            // try to deserialize normally
            if(w==null) {
                try {
                    w = App.APP.serializators.fromXML(Component.class,launcher);
                } catch (StreamException ignored) {
                    LOGGER.error("Could not load .fxwl {}", launcher);
                }
            }

            // try to build widget using just launcher filename
            if(w==null) {
                wf = APP.widgetManager.factories.get(getName(launcher));
                if(wf!=null) w = wf.create();
            }

            launchComponent(w);
        }catch(Exception x) {
            LOGGER.error("Could not load component from file {}", launcher,x);
        }
    }

    public static void launchComponent(String componentName) {
        WidgetFactory wf = APP.widgetManager.factories.get(componentName);
        Component w = wf==null ? null : wf.create();
        launchComponent(w);
    }

    private static void launchComponent(Component w) {
        if(w!=null) {
            if(launching1st) {
                APP.window.setContent(w);
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