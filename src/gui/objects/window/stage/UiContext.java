
package gui.objects.window.stage;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gui.objects.popover.PopOver;
import layout.Component;
import layout.container.layout.Layout;
import layout.widget.Widget;
import layout.widget.WidgetFactory;
import main.App;
import unused.SimpleConfigurator;
import util.conf.Configurable;
import util.file.Util;

import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static main.App.APP;
import static util.dev.Util.noØ;
import static util.file.Util.getName;
import static util.graphics.Util.getScreen;

/**
 *
 * @author Martin Polakovic
 */
public final class UiContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiContext.class);
    private static double x;
    private static double y;
    private static final Set<ClickHandler> onClicks = new HashSet<>();
    private static final WindowManager windowManager = APP.windowManager;

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
        return windowManager.getActive().get().getX()+x;
    }

    /** Get last mouse press screen y coordinate. */
    public static double getY() {
        return windowManager.getActive().get().getY()+y;
    }

    /**
     * @param widget non-null widget widget to open
     */
    public static Window showWindow(Component widget) {
	    noØ(widget);

        Window w = windowManager.create();
               w.initLayout();
               w.setContent(widget);
               w.show();
               w.setScreen(getScreen(APP.mouseCapture.getMousePosition()));
               w.setXYScreenCenter();
        return w;
    }

    public static PopOver showFloating(Widget w) {
        noØ(w);

        // build layout
	    // We are building standalone widget here, but every widget must be part of the layout
	    AnchorPane root = new AnchorPane();
	    Layout l = new Layout();
	    l.isStandalone = true;
	    l.load(root);

        // build popup
        PopOver p = new PopOver<>(root);
                p.title.set(w.getInfo().nameGui());
                p.setAutoFix(false);
                p.show(windowManager.getActive().get().getStage(),getX(),getY());
                // unregister the widget from active widgets manually on close
                p.addEventFilter(WINDOW_HIDING, we -> l.close());

        // load widget when graphics ready & shown
	    l.setChild(w);

	    // TODO: hack
		// make popup honor widget size
	    double prefW = ((Pane)w.load()).getPrefWidth();
	    double prefH = ((Pane)w.load()).getPrefHeight();
	    p.setPrefSize(prefW, prefH);

	    return p;
    }

	public static void showSettings(Configurable c, MouseEvent e) {
		showSettings(c, (Node) e.getSource());
	}

	public static void showSettings(Configurable c, Node n) {
		showSettingsSimple(c, n);
		// TODO: decide whether we use SimpleConfigurator or Configurator widget
//		String name = c instanceof Widget ? ((Widget)c).getName() : "";
//		Configurator sc = new Configurator(true);
//		sc.configure(c);
//		PopOver p = new PopOver<>(sc);
//		p.title.set((name==null ? "" : name+" ") + " Settings");
//		p.setArrowSize(0); // auto-fix breaks the arrow position, turn off - sux
//		p.setAutoFix(true); // we need auto-fix here, because the popup can get rather big
//		p.setAutoHide(true);
//		p.show(n);
	}

	public static void showSettingsSimple(Configurable c, MouseEvent e) {
		showSettingsSimple(c, (Node) e.getSource());
	}

	public static void showSettingsSimple(Configurable c, Node n) {
		String name = c instanceof Widget ? ((Widget)c).getName() : "";
		SimpleConfigurator sc = new SimpleConfigurator(c);
		PopOver p = new PopOver<>(sc);
		p.title.set((name==null ? "" : name+" ") + " Settings");
		p.setArrowSize(0); // auto-fix breaks the arrow position, turn off - sux
		p.setAutoFix(true); // we need auto-fix here, because the popup can get rather big
		p.setAutoHide(true);
		p.show(n);
	}

    public static PopOver showFloating(Node content, String title) {
        noØ(content);
        noØ(title);  // we could use null, but disallow

        PopOver p = new PopOver<>(content);
                p.title.set(title);
                p.setAutoFix(false);
                p.show(windowManager.getActive().get().getStage(),getX(),getY());
        return p;
    }

    public static void launchComponent(File launcher) {
        launchComponent(instantiateComponent(launcher));
    }

    public static void launchComponent(String componentName) {
        WidgetFactory<?> wf = APP.widgetManager.factories.get(componentName);
	    Component w = wf==null ? null : wf.create();
        launchComponent(w);
    }

    private static void launchComponent(Component w) {
        if (w!=null) {
            if (APP.windowManager.windows.isEmpty()) {
                APP.windowManager.getActiveOrNew().setContent(w);
            } else {
	            APP.windowManager.createWindow(w);
            }
        }
    }

    public static Component instantiateComponent(File launcher) {
        try {
            WidgetFactory<?> wf;
            Component w = null;

            // simple launcher version, contains widget name on 1st line
            String wn = Util.readFileLines(launcher).limit(1).findAny().orElse("");
            wf = APP.widgetManager.factories.get(wn);
            if (wf!=null)
            	w = wf.create();

            // try to deserialize normally
            if (w==null)
            	w = App.APP.serializators.fromXML(Component.class, launcher)
						.ifError(e -> LOGGER.error("Could not load component", e))
		                .get();


            // try to build widget using just launcher filename
            if (w==null) {
                wf = APP.widgetManager.factories.get(getName(launcher));
                if (wf!=null) w = wf.create();
            }

            return w;
        } catch(Exception x) {
            LOGGER.error("Could not load component from file {}", launcher,x);
            return null;
        }
    }

    public static WritableImage makeSnapshot(Node n) {
        return n.snapshot(new SnapshotParameters(), null);
    }


    public interface ClickHandler {
        void handle(Window w, MouseEvent e);
    }

}